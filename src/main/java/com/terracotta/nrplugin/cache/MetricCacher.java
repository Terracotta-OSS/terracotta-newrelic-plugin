package com.terracotta.nrplugin.cache;

import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.terracotta.nrplugin.pojo.*;
import com.terracotta.nrplugin.rest.tmc.MetricFetcher;
import com.terracotta.nrplugin.util.MetricUtil;
import net.minidev.json.JSONArray;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/21/14
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class MetricCacher {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ExecutorService executorService;

    @Autowired
    MetricFetcher metricFetcher;

    @Autowired
    MetricUtil metricUtil;

    @Value("#{cacheManager.getCache('statsCache')}")
    Cache statsCache;

    @Value("#{cacheManager.getCache('diffsCache')}")
    Cache diffsCache;

    @Value("#{cacheManager.getCache('lastStatisticsCache')}")
    Cache lastStatisticsCache;

    @Value("${com.saggs.terracotta.nrplugin.data.windowSize}")
    int windowSize;

    @Autowired
    LockManager lockManager;

    @Scheduled(fixedDelayString = "${com.saggs.terracotta.nrplugin.restapi.executor.fixedDelay.milliseconds}", initialDelay = 500)
    public void cacheStats() throws Exception {
        try {
            lockManager.lockCache();
            doCacheStats();
        } finally {
            lockManager.unlockCache();
        }
    }

    private void doCacheStats() throws Exception {
        log.info("Starting to cache all stats...");
        Map<Metric.Source, String> metricData = metricFetcher.getAllMetricData();
        Map<Metric.Source, JSONArray> jsonObjects = toJsonArray(metricData);

        // Get all cache names
        Set<MetricDatasetCacheComponent> cacheComponents = getCacheComponents(jsonObjects);

        // Get all server names and their mapped stripe name
        Set<MetricDatasetServerComponent> serverComponents = getServerComponents(jsonObjects);

        log.info("Parsed metrics into JSONArrays...");
        for (Metric metric : metricUtil.getRegularMetrics()) {
            // Get all JSON data for this source
            JSONArray objects = jsonObjects.get(metric.getSource());

            if (Metric.Source.cache.equals(metric.getSource())) {
                for (MetricDatasetCacheComponent cacheComponent : cacheComponents) {
                    // Filter by cache name & metric
                    JSONArray values = JsonPath.read(objects, metric.getDataPath(), Filter.filter(Criteria.where("name").is(cacheComponent.getCacheName())));

                    MetricDataset metricDataset = getMetricDatasetFromCache(metric, cacheComponent, true);
                    putValueInDataset(metricDataset, values);
                    putMetricDataSetInCache(metricDataset);

                    if (metricDataset.getMetric().isCreateDiff()) {
                        MetricDataset diffMetricDataset = new MetricDataset(metricDataset.getMetric().clone(), metricDataset.getComponentDetail().clone());
                        putValueInDataset(diffMetricDataset, values);
                        putDiffDataInCache(diffMetricDataset);
                    }
                }
            } else if (Metric.Source.server.equals(metric.getSource())) {
                for (MetricDatasetServerComponent serverComponent : serverComponents) {
                    // Filter by server name & metric
                    JSONArray values = JsonPath.read(objects, metric.getDataPath(), Filter.filter(Criteria.where("sourceId").is(serverComponent.getServerName())));

                    MetricDataset metricDataset = getMetricDatasetFromCache(metric, serverComponent, true);

                    //set the component state as it may have changed since last iteration
                    setStateInServerMetricDataSet(metricDataset, serverComponent.getState());

                    putValueInDataset(metricDataset, values);
                    putMetricDataSetInCache(metricDataset);

                    if (metricDataset.getMetric().isCreateDiff()) {
                        MetricDataset diffMetricDataset = new MetricDataset(metricDataset.getMetric().clone(), metricDataset.getComponentDetail().clone());
                        putValueInDataset(diffMetricDataset, values);
                        putDiffDataInCache(diffMetricDataset);
                    }
                }
            }
        }

        // Handle special metrics
        for (Metric metric : metricUtil.getSpecialMetrics()) {
            JSONArray objects = jsonObjects.get(metric.getSource());
            if (MetricUtil.METRIC_NUM_CONNECTED_CLIENTS.equals(metric.getName())) {
                JSONArray clientEntities = JsonPath.read(objects, "$[*].clientEntities[*]");
                for (MetricDatasetServerComponent serverComponent : serverComponents) {
                    MetricDataset metricDataset = getMetricDatasetFromCache(metric, serverComponent, true);
                    if (null != metricDataset.getComponentDetail() &&
                            metricDataset.getComponentDetail() instanceof MetricDatasetServerComponent &&
                            ((MetricDatasetServerComponent) metricDataset.getComponentDetail()).getState() == MetricDatasetServerComponent.State.ACTIVE) {
                        putValueInDataset(metricDataset, clientEntities.size());
                    } else {
                        putValueInDataset(metricDataset, 0);
                    }
                    putMetricDataSetInCache(metricDataset);
                }
            } else if (MetricUtil.METRIC_SERVER_STATE.equals((metric.getReportingComponents().size() > 0) ? metric.getReportingComponents().get(metric.getReportingComponents().size() - 1) : "")) {
                for (MetricDatasetServerComponent serverComponent : serverComponents) {
                    MetricDataset metricDataset = getMetricDatasetFromCache(metric, serverComponent, true);
                    if (MetricDatasetServerComponent.State.parseString(metric.getName()) == serverComponent.getState()) {
                        putValueInDataset(metricDataset, 1);
                    } else {
                        putValueInDataset(metricDataset, 0);
                    }
                    putMetricDataSetInCache(metricDataset);
                }
            }
        }

        log.info("Starting to cache Ratio Metrics...");
        for (Metric metric : metricUtil.getRatioMetrics()) {
            RatioMetric ratioMetric = (RatioMetric) metric;
            for (MetricDatasetCacheComponent cacheComponent : cacheComponents) {
                MetricDataset numeratorDataset = null;
                MetricDataset denominatorDataset = null;
                for (Object key : statsCache.getKeys()) {
                    Element element = statsCache.get((key));
                    if (element != null && element.getObjectValue() instanceof MetricDataset) {
                        MetricDataset metricDataset = (MetricDataset) element.getObjectValue();
                        if (cacheComponent.equals(metricDataset.getComponentDetail())) {
                            if (metricDataset.getMetric().getReportingPath().equals(ratioMetric.getNumerator().getReportingPath())) {
                                numeratorDataset = metricDataset;
                            } else if (metricDataset.getMetric().getReportingPath().equals(ratioMetric.getDenominator().getReportingPath())) {
                                denominatorDataset = metricDataset;
                            }
                        }
                    }
                    if (null != numeratorDataset && null != denominatorDataset)
                        break;
                }

                Double numerator = getDiffSum(numeratorDataset);
                Double denominator = getDiffSum(denominatorDataset);
                if (numerator != null && denominator != null) {
                    denominator += numerator;
                    double ratio = denominator > 0 ? 100 * numerator / denominator : 0;

                    if (log.isTraceEnabled())
                        log.trace("Ratio = " + numeratorDataset.getKey() + " / " + denominatorDataset.getKey() + " = " + numerator + " / " + denominator + " = " + ratio);

                    MetricDataset ratioDataset = getMetricDatasetFromCache(ratioMetric, denominatorDataset.getComponentDetail(), true);
                    putValueInDataset(ratioDataset, ratio);
                    putMetricDataSetInCache(ratioDataset);
                } else {
                    log.warn("Could not calculate ratio for '" + ratioMetric.getName() + " because numerator or denomitator was null");
                }
            }
        }
        log.info("Done caching stats.");
    }

    private MetricDatasetServerComponent.State getServerState(Map<Metric.Source, JSONArray> jsonObjects, String serverName) {
        MetricDatasetServerComponent.State serverState = MetricDatasetServerComponent.State.UNKNOWN; //could not find the state for that server name...something happened.

        JSONArray topologies = jsonObjects.get(Metric.Source.topologies);
        JSONArray attributes = JsonPath.read(topologies, "$[*].serverGroupEntities.servers.attributes");
        JSONArray stateArray = JsonPath.read(attributes, "$[?].State", Filter.filter(Criteria.where("Name").is(serverName)));

        // always capture the state...
        // if no results from the json query, it means there's an error for that server...eg. server is down or something...
        if (stateArray.size() > 0) {
            serverState = MetricDatasetServerComponent.State.parseString((String) stateArray.get(0));
        } else {
            serverState = MetricDatasetServerComponent.State.ERROR;
        }

        return serverState;
    }

    private Double getDiffSum(MetricDataset metricDataset) {
        if (null != metricDataset) {
            Metric diffMetric = getDiffMetricForAbsoluteMetric(metricDataset.getMetric());
            MetricDataset diffDataSet = getDiffDataSetFromCache(MetricDataset.getKey(diffMetric, metricDataset.getComponentDetail()));
            if (null != diffDataSet) {
                if (log.isTraceEnabled()) {
                    log.trace("Got Diff Sum for metric '" + metricDataset.getMetric().getName() + "': " + diffDataSet.getStatistics().getSum());
                }
                return diffDataSet.getStatistics().getSum();
            }
        }
        return null;
    }

    private Set<String> getCacheManagerNames(Map<Metric.Source, JSONArray> jsonObjects) {
        JSONArray cacheStats = jsonObjects.get(Metric.Source.cache);
        return getSet(cacheStats, "cacheManagerName");
    }

    private Set<String> getCacheNamesInCacheManager(Map<Metric.Source, JSONArray> jsonObjects, String cacheManagerName) {
        JSONArray cacheStats = jsonObjects.get(Metric.Source.cache);

        Set<String> cacheNames;
        if (null != cacheManagerName && !"".equals(cacheManagerName.trim())) {
            JSONArray caches = JsonPath.read(cacheStats, "$[?]", Filter.filter(Criteria.where("cacheManagerName").is(cacheManagerName)));
            cacheNames = getSet(caches, "name");
        } else
            cacheNames = getSet(cacheStats, "name");


        return cacheNames;
    }

    private Set<String> getServerNames(Map<Metric.Source, JSONArray> jsonObjects) {
        return getServerNames(jsonObjects, null);
    }

    private Set<String> getServerNames(Map<Metric.Source, JSONArray> jsonObjects, String stripeName) {
        JSONArray topologies = jsonObjects.get(Metric.Source.topologies);

//        String jsonQuery;
//        if(null != stripeName && !"".equals(stripeName.trim()))
//            jsonQuery = String.format("$[*].serverGroupEntities[?(@.name=='%s')].servers[*].attributes", stripeName);
//        else
//            jsonQuery = "$[*].serverGroupEntities[*].servers[*].attributes";

        JSONArray servers;
        if (null != stripeName && !"".equals(stripeName.trim()))
            servers = JsonPath.read(topologies, "$[*].serverGroupEntities[?].servers[*].attributes", Filter.filter(Criteria.where("name").is(stripeName)));
        else
            servers = JsonPath.read(topologies, "$[*].serverGroupEntities[*].servers[*].attributes");

        return getSet(servers, "Name");
    }

    private Set<String> getServerStripes(Map<Metric.Source, JSONArray> jsonObjects) {
        JSONArray topologies = jsonObjects.get(Metric.Source.topologies);
        JSONArray stripes = JsonPath.read(topologies, "$[*].serverGroupEntities[*]");
        return getSet(stripes, "name");
    }

    private Map<String, String> getServersToStripesMap(Map<Metric.Source, JSONArray> jsonObjects) {
        Map<String, String> serversToStripesMap = new HashMap<String, String>();
        Set<String> stripes = getServerStripes(jsonObjects);
        for (String stripe : stripes) {
            Set<String> serversInStripe = getServerNames(jsonObjects, stripe);
            for (String serverName : serversInStripe) {
                serversToStripesMap.put(serverName, stripe);
            }
        }
        return serversToStripesMap;
    }

    private Set<MetricDatasetCacheComponent> getCacheComponents(Map<Metric.Source, JSONArray> jsonObjects) {
        Set<MetricDatasetCacheComponent> cacheComponents = new HashSet<MetricDatasetCacheComponent>();
        Set<String> cacheManagerNames = getCacheManagerNames(jsonObjects);
        for (String cacheManager : cacheManagerNames) {
            Set<String> cacheNames = getCacheNamesInCacheManager(jsonObjects, cacheManager);
            for (String cacheName : cacheNames) {
                cacheComponents.add(new MetricDatasetCacheComponent(cacheName, cacheManager));
            }
        }
        return cacheComponents;
    }

    private Set<MetricDatasetServerComponent> getServerComponents(Map<Metric.Source, JSONArray> jsonObjects) {
        Set<MetricDatasetServerComponent> serverComponents = new HashSet<MetricDatasetServerComponent>();
        Set<String> stripes = getServerStripes(jsonObjects);
        for (String stripe : stripes) {
            Set<String> serversInStripe = getServerNames(jsonObjects, stripe);
            for (String serverName : serversInStripe) {
                serverComponents.add(new MetricDatasetServerComponent(serverName, stripe, getServerState(jsonObjects, serverName)));
            }
        }
        return serverComponents;
    }

    private Set<String> getSet(JSONArray objects, String attribute) {
        JSONArray nameArray = JsonPath.read(objects, "$[*]." + attribute);
        Set<String> names = new HashSet<String>();
        for (Object object : nameArray) {
            names.add((String) object);
        }
        return names;
    }

    private Map<Metric.Source, JSONArray> toJsonArray(Map<Metric.Source, String> metricData) {
        Map<Metric.Source, JSONArray> jsonObjects = new HashMap<Metric.Source, JSONArray>();
        for (Metric.Source source : Metric.Source.values()) {
            String json = metricData.get(source);
            JSONArray objects = JsonPath.read(json, "$[*]");
            jsonObjects.put(source, objects);
        }
        return jsonObjects;
    }

    private void putValueInDataset(MetricDataset metricDataset, Object value) {
        if (value instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) value;
            for (Object child : jsonArray) {
                putPrimitiveValue(metricDataset, child);
            }
        } else if (value instanceof Number) {
            putPrimitiveValue(metricDataset, value);
        }
    }

    private void putPrimitiveValue(MetricDataset metricDataset, Object value) {
        if (value instanceof Number) {
            putPrimitiveValue(metricDataset, ((Number) value).doubleValue());
        } else {
            log.warn("Class " + value.getClass() + " not numeric.");
        }
    }

    private void putPrimitiveValue(MetricDataset metricDataset, double value) {
        log.trace("Adding values {} to dataset {}", value, metricDataset.getKey());
        metricDataset.addValue(value);
    }

    public MetricDataset getMetricDatasetFromCache(Metric metric, MetricDatasetComponent componentDetail, boolean createEmptyIfNull) {
        Element element = statsCache.get(MetricDataset.getKey(metric, componentDetail));
        MetricDataset metricDataset = null;
        if (element != null) {
            metricDataset = (MetricDataset) element.getObjectValue();
            log.trace("Extracting metricDataset values for {} - {}", metricDataset.getKey(), metricUtil.metricAsJson(metricDataset, true).toString());
        } else if (createEmptyIfNull) {
            log.trace("metricDataset not found in cache...creating a new one");
            metricDataset = new MetricDataset(metric, componentDetail);
        }

        return metricDataset;
    }

    public void setStateInServerMetricDataSet(MetricDataset metricDataset, MetricDatasetServerComponent.State state) {
        if (null != metricDataset && null != metricDataset.getComponentDetail() && metricDataset.getComponentDetail() instanceof MetricDatasetServerComponent) {
            ((MetricDatasetServerComponent) metricDataset.getComponentDetail()).setState(state);
        }
    }

    public void putMetricDataSetInCache(MetricDataset metricDataset) {
        log.trace("Putting " + metricDataset.getKey() + " to statsCache.");
        statsCache.put(new Element(metricDataset.getKey(), metricDataset));
    }

    public MetricDataset getDiffDataSetFromCache(String key) {
        Element element = diffsCache.get(key);
        if (element != null) return (MetricDataset) element.getObjectValue();
        else return null;
    }

    public void putDiffDataSetInCache(String key, MetricDataset metricDataset) {
        diffsCache.put(new Element(key, metricDataset));
    }

    public DescriptiveStatistics getLastStatisticsFromCache(String key) {
        Element element = lastStatisticsCache.get(key);
        if (element != null) return (DescriptiveStatistics) element.getObjectValue();
        else return null;
    }

    public void putLastStatisticsInCache(String key, DescriptiveStatistics stats) {
        lastStatisticsCache.put(new Element(key, stats));
    }

    private void putDiffDataInCache(MetricDataset latest) {
        DescriptiveStatistics previousStatistics = getLastStatisticsFromCache(latest.getKey());
        if (previousStatistics == null) {
            log.debug("No previously cached data for metric " + latest.getKey());
        } else {
            Metric diffMetric = getDiffMetricForAbsoluteMetric(latest.getMetric());
            log.trace("{}: Latest SUM {} - Previous SUM {} = {}", diffMetric.getReportingPath(), latest.getStatistics().getSum(), previousStatistics.getSum(), latest.getStatistics().getSum() - previousStatistics.getSum());

            MetricDataset diffDataSet = getDiffDataSetFromCache(MetricDataset.getKey(diffMetric, latest.getComponentDetail()));
            if (null == diffDataSet) {
                diffDataSet = new MetricDataset(diffMetric, latest.getComponentDetail());
            }

            //create a new dataset with the diff value and the right count
            diffDataSet.addValue(latest.getStatistics().getSum() - previousStatistics.getSum());

            putDiffDataSetInCache(diffDataSet.getKey(), diffDataSet);
        }

        // Update lastDataSet after done
        log.trace("Updating key '" + latest.getKey() + "', SUM: " + latest.getStatistics().getSum());
        putLastStatisticsInCache(latest.getKey(), new SynchronizedDescriptiveStatistics(
                (SynchronizedDescriptiveStatistics) latest.getStatistics()));
    }

    public Metric getDiffMetricForAbsoluteMetric(Metric absolute) {
        Metric diffMetric = null;
        if (null != absolute) {
            try {
                diffMetric = absolute.clone();
                diffMetric.setDiff(true);
            } catch (CloneNotSupportedException e) {
                log.error("Could not clone the Metric object", e);
            }
        }
        return diffMetric;
    }

    private double toDouble(double value) {
        if (Double.isNaN(value)) return 0;
        else return value;
    }
}
