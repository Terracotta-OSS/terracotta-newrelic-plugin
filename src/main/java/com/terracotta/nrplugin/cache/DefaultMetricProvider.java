package com.terracotta.nrplugin.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.MetricDataset;
import com.terracotta.nrplugin.pojo.MetricDatasetServerComponent;
import com.terracotta.nrplugin.pojo.nr.Agent;
import com.terracotta.nrplugin.pojo.nr.Component;
import com.terracotta.nrplugin.pojo.nr.NewRelicPayload;
import com.terracotta.nrplugin.util.MetricUtil;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/25/14
 * Time: 8:03 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class DefaultMetricProvider implements MetricProvider {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("#{cacheManager.getCache('statsCache')}")
    Cache statsCache;

    @Value("#{cacheManager.getCache('diffsCache')}")
    Cache diffsCache;

    @Value("#{cacheManager.getCache('lastStatisticsCache')}")
    Cache lastStatisticsCache;

    @Autowired
    MetricUtil metricUtil;

    @Value("${com.saggs.terracotta.nrplugin.nr.executor.fixedDelay.milliseconds}")
    long durationMillis;

    @Value("${com.saggs.terracotta.nrplugin.version}")
    String version;

    @Value("${com.saggs.terracotta.nrplugin.nr.environment.prefix}")
    String componentPrefix;

    String hostname = "TC-NR-HOST";

    @Value("${com.saggs.terracotta.nrplugin.nr.agent.terracotta.guid}")
    String terracottaAgentGuid;

    @Value("${com.saggs.terracotta.nrplugin.nr.agent.ehcache.guid}")
    String ehcacheAgentGuid;

    long pid;

    @Autowired
    LockManager lockManager;

    @PostConstruct
    private void init() {
        Sigar sigar = new Sigar();

        try {
            pid = sigar.getPid();
        } catch (Error e) {
            log.error("Could not infer PID.");
            pid = -1;
        }

        try {
            hostname = sigar.getNetInfo().getHostName();
        } catch (Error e) {
            log.error("Could not infer hostname.");
        } catch (Exception ex) {
            log.error("Could not infer hostname.");
        }
    }

    @Override
    public void clearAllMetricData() {
        log.info("Clearing all metric data...");
        statsCache.removeAll();
        diffsCache.removeAll();

//            List<String> keys = statsCache.getKeys();
//            for (String key : keys) {
//                Element element = statsCache.get(key);
//                MetricDataset metricDataset = (MetricDataset) element.getObjectValue();
//                metricDataset.getStatistics().clear();
//            }
    }

    public NewRelicPayload assemblePayload() throws JsonProcessingException {
        NewRelicPayload payload = new NewRelicPayload();
        Map<String, Component> componentMap = new HashMap<String, Component>(); // componentName -> component
        Map<String, Metric.RollupType> metricRollupTypeMap = new HashMap<String, Metric.RollupType>(); // metricName -> rollupType
        Set<String> metricRollupActiveServers = new HashSet<String>(); // componentName
        Set<String> metricRollupPassiveServers = new HashSet<String>(); // componentName

        int numMetrics = 0;

        // Get absolute metrics
        for (Object key : statsCache.getKeys()) {
            Element element = statsCache.get((key));
            if (element != null && element.getObjectValue() instanceof MetricDataset) {
                MetricDataset metricDataset = (MetricDataset) element.getObjectValue();

                //save the rolluptype for each metric
                metricRollupTypeMap.put(metricDataset.getMetric().getReportingPath(), metricDataset.getMetric().getRollupType());

                String componentName = metricDataset.getComponentDetail().getComponentName();
                Component component = componentMap.get(componentName);
                if (component == null) {
                    component = constructComponent(metricDataset);
                }

                //specifics to server metrics
                if (metricDataset.getComponentDetail() instanceof MetricDatasetServerComponent) {
                    if (((MetricDatasetServerComponent) metricDataset.getComponentDetail()).getState() == MetricDatasetServerComponent.State.ACTIVE) {
                        metricRollupActiveServers.add(componentName);
                    } else if (((MetricDatasetServerComponent) metricDataset.getComponentDetail()).getState() == MetricDatasetServerComponent.State.PASSIVE) {
                        metricRollupPassiveServers.add(componentName);
                    }
                }

                component.putMetric(metricDataset, metricUtil, true); // metric path -> metric stats
                componentMap.put(componentName, component);
                numMetrics++;
            }
        }

        // Get diff metrics
        for (Object key : diffsCache.getKeys()) {
            Element element = diffsCache.get((key));
            if (element != null) {
//                MetricCacher.DiffEntry diffEntry = (MetricCacher.DiffEntry) element.getObjectValue();
//                MetricDataset metricDataset = diffEntry.getMetricDataset();

                MetricDataset metricDataset = (MetricDataset) element.getObjectValue();

//				Map.Entry<String, Map<String, Number>> diff = (Map.Entry<String, Map<String,Number>>) element.getObjectValue();

                //save the rolluptype for each metric
                metricRollupTypeMap.put(metricDataset.getMetric().getReportingPath(), metricDataset.getMetric().getRollupType());

                String componentName = metricDataset.getComponentDetail().getComponentName();
                Component component = componentMap.get(componentName);
                if (component == null) {
                    component = constructComponent(metricDataset);
                }

                //specifics to server metrics
                if (metricDataset.getComponentDetail() instanceof MetricDatasetServerComponent) {
                    if (((MetricDatasetServerComponent) metricDataset.getComponentDetail()).getState() == MetricDatasetServerComponent.State.ACTIVE) {
                        metricRollupActiveServers.add(componentName);
                    } else if (((MetricDatasetServerComponent) metricDataset.getComponentDetail()).getState() == MetricDatasetServerComponent.State.PASSIVE) {
                        metricRollupPassiveServers.add(componentName);
                    }
                }

                component.putMetric(metricDataset, metricUtil, false); // metric path -> metric stats
                componentMap.put(componentName, component);
            }
        }

        List<Component> allRollupComponents = new ArrayList<Component>();

        //rollup the caches
        allRollupComponents.addAll(parseComponentsGroupedByGUID(
                groupComponentsByGuid(componentMap, Collections.singleton(ehcacheAgentGuid), null, metricRollupTypeMap),
                "Rollup"
        ));

        //rollup the active servers
        allRollupComponents.addAll(parseComponentsGroupedByGUID(
                groupComponentsByGuid(componentMap, Collections.singleton(terracottaAgentGuid), metricRollupActiveServers, metricRollupTypeMap),
                "Active_Rollup"
        ));

        //rollup the passive servers
        allRollupComponents.addAll(parseComponentsGroupedByGUID(
                groupComponentsByGuid(componentMap, Collections.singleton(terracottaAgentGuid), metricRollupPassiveServers, metricRollupTypeMap),
                "Passive_Rollup"
        ));

        //rollup all the servers
        allRollupComponents.addAll(parseComponentsGroupedByGUID(
                groupComponentsByGuid(componentMap, Collections.singleton(terracottaAgentGuid), null, metricRollupTypeMap),
                "Rollup"
        ));

        for (Component component : allRollupComponents) {
            componentMap.put(component.getName(), component);
        }

        payload.setAgent(new Agent(hostname, pid, version));
        payload.setComponents(new ArrayList<Component>(componentMap.values()));
        log.info("Returning complete stats payload with " + numMetrics + " aggregated metric(s) gathered from cache.");
        return payload;
    }

    private Component constructComponent(MetricDataset metricDataset) {
        double durationSeconds = durationMillis * 0.001;
        String guid =
                Metric.Source.client.equals(metricDataset.getMetric().getSource()) ||
                        Metric.Source.cache.equals(metricDataset.getMetric().getSource()) ?
                        ehcacheAgentGuid : terracottaAgentGuid;
        return new Component(String.format("%s/%s", componentPrefix, metricDataset.getComponentDetail().getComponentName()), guid, (long) durationSeconds);
    }

    public Map<String, Map<String, Map<String, Number>>> groupComponentsByGuid(Map<String, Component> currentComponentMap, Set<String> filterGuid, Set<String> filterComponents, Map<String, Metric.RollupType> metricRollupTypeMap) {
        // Maps guid -> metric -> stats
        Map<String, Map<String, Map<String, Number>>> componentsGroupedByGUID = new ConcurrentHashMap<String, Map<String, Map<String, Number>>>(); // Component_GUID -> metric_name -> metric stats (json array)

        // Maps component name -> total entries
        for (Map.Entry<String, Component> newComponentEntry : currentComponentMap.entrySet()) {
            String componentName = newComponentEntry.getKey();
            String componentGUID = newComponentEntry.getValue().getGuid();

            //only catch the provided GUIDS and/or components
            if (
                    null == filterGuid || (null != filterGuid && filterGuid.contains(componentGUID)) &&
                            null == filterComponents || (null != filterComponents && filterComponents.contains(componentName))
                    ) {
                Map<String, Map<String, Number>> metricsRollupTotals = componentsGroupedByGUID.get(componentGUID);
                if (metricsRollupTotals == null) {
                    metricsRollupTotals = new ConcurrentHashMap<String, Map<String, Number>>();
                    componentsGroupedByGUID.put(componentGUID, metricsRollupTotals);
                }
                for (Map.Entry<String, Object> newMetricEntry : newComponentEntry.getValue().getMetrics().entrySet()) { // metricEntry = metric_name -> metric stats (json array)
                    String metricName = newMetricEntry.getKey();
                    Map<String, Number> newMetricDataSetStats = (Map<String, Number>) newMetricEntry.getValue();

                    Map<String, Number> metricStatsExistingInRollup = metricsRollupTotals.get(metricName);
                    if (metricStatsExistingInRollup == null) {
                        metricStatsExistingInRollup = new ConcurrentHashMap<String, Number>(); //metricname -> metric stats (json array)
                        metricsRollupTotals.put(metricName, metricStatsExistingInRollup);
                    }
                    addData(metricName, metricStatsExistingInRollup, newMetricDataSetStats, metricRollupTypeMap.get(metricName));
                }
            }
        }
        return componentsGroupedByGUID;
    }

    private List<Component> parseComponentsGroupedByGUID(Map<String, Map<String, Map<String, Number>>> componentsGroupedByGUID, String componentSuffix) {
        List<Component> rollupComponents = new ArrayList<Component>();
        for (Map.Entry<String, Map<String, Map<String, Number>>> entry : componentsGroupedByGUID.entrySet()) {
            double durationSeconds = durationMillis * 0.001;
            String guid = entry.getKey();
            String[] guidSplit = guid.split("\\.");
            String guidSuffix = guidSplit[guidSplit.length - 1];

            Component component = new Component(String.format("%s/%s_%s", componentPrefix, guidSuffix, componentSuffix), guid, (long) durationSeconds);
            Map<String, Map<String, Number>> metrics = entry.getValue();
            for (Map.Entry<String, Map<String, Number>> metric : metrics.entrySet()) {
                component.putMetric(metric.getKey(), metric.getValue());
            }
            rollupComponents.add(component);
        }
        return rollupComponents;
    }

    private void addData(String metricName, Map<String, Number> existingMetrics, Map<String, Number> newMetricData, Metric.RollupType rollupType) {
        for (Map.Entry<String, Number> newMetricStatData : newMetricData.entrySet()) {
            String stat = newMetricStatData.getKey();
            Number incr = newMetricStatData.getValue();
            Double initial = existingMetrics.get(stat) != null ? (Double) existingMetrics.get(stat) : 0;
            if (incr != null) {
                // Set count to the number of Components so that the average value equals the sum of all clients/TSAs
                if (MetricUtil.NEW_RELIC_COUNT.equals(stat) &&
                        newMetricData.containsKey(MetricUtil.NEW_RELIC_SUM_OF_SQUARES) &&
                        !metricName.contains("Ratio")) {
                    existingMetrics.put(MetricUtil.NEW_RELIC_COUNT, incr.doubleValue());
                } else {
                    if (rollupType == Metric.RollupType.sum)
                        existingMetrics.put(stat, initial + incr.doubleValue());
                    else if (rollupType == Metric.RollupType.none)
                        existingMetrics.put(stat, incr.doubleValue());
                }
            }
        }
        // Remove min/max for Ratio metrics
        if (metricName.contains("Ratio")) {
            existingMetrics.remove(MetricUtil.NEW_RELIC_MIN);
            existingMetrics.remove(MetricUtil.NEW_RELIC_MAX);
        }
    }
}
