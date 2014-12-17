package com.terracotta.nrplugin.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.MetricDataset;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	public NewRelicPayload assemblePayload() throws JsonProcessingException {
		NewRelicPayload payload = new NewRelicPayload();
		Map<String, Component> componentMap = new HashMap<String, Component>(); // componentName -> component
		int numMetrics = 0;

		// Get absolute metrics
		for (Object key : statsCache.getKeys()) {
			Element element = statsCache.get((key));
			if (element != null && element.getObjectValue() instanceof MetricDataset) {
				MetricDataset metricDataset = (MetricDataset) element.getObjectValue();
				Component component = componentMap.get(metricDataset.getComponentName());
				if (component == null) {
					component = constructComponent(metricDataset);
				}
				Map.Entry entry = metricUtil.metricAsJson(metricDataset);
				component.putMetric((String) entry.getKey(), entry.getValue());
				componentMap.put(metricDataset.getComponentName(), component);
				numMetrics++;
			}
		}

		// Get diff metrics
		for (Object key : diffsCache.getKeys()) {
			Element element = diffsCache.get((key));
			if (element != null) {
				MetricCacher.DiffEntry diffEntry = (MetricCacher.DiffEntry) element.getObjectValue();
//				Map.Entry<String, Map<String, Number>> diff = (Map.Entry<String, Map<String,Number>>) element.getObjectValue();
				String componentName = diffEntry.getMetricDataset().getComponentName();
				Component component = componentMap.get(componentName);
				if (component == null) {
					component = constructComponent(diffEntry.getMetricDataset());
				}
				component.putMetric(diffEntry.getMetricDataset().getMetric().getReportingPath(), diffEntry.getDiffs());
				componentMap.put(componentName, component);
			}
		}

		// Get rollup metrics
		for (Component component : createRollups(componentMap)) {
			componentMap.put(component.getName(), component);
		}

		payload.setAgent(new Agent(hostname, pid, version));
		payload.setComponents(new ArrayList<Component>(componentMap.values()));
		log.info("Returning " + numMetrics + " metric(s) from cache.");
		return payload;
	}

	private Component constructComponent(MetricDataset metricDataset) {
		double durationSeconds = durationMillis * 0.001;
		String guid =
				Metric.Source.client.equals(metricDataset.getMetric().getSource()) ||
						Metric.Source.cache.equals(metricDataset.getMetric().getSource()) ?
						ehcacheAgentGuid : terracottaAgentGuid;
		return new Component(componentPrefix + "_" + metricDataset.getComponentName(), guid, (long) durationSeconds);
	}

	public List<Component> createRollups(Map<String, Component> componentMap) {
		// Maps guid -> metric -> stats
		Map<String, Map<String, Map<String, Number>>> guids = new ConcurrentHashMap<String, Map<String, Map<String, Number>>>();
		for (Map.Entry<String, Component> entry : componentMap.entrySet()) {
			String guid = entry.getValue().getGuid();
			Map<String, Map<String, Number>> metrics = guids.get(guid);
			if (metrics == null) {
				metrics = new ConcurrentHashMap<String, Map<String, Number>>();
				guids.put(guid, metrics);
			}
			for (Map.Entry<String, Object> metricEntry : entry.getValue().getMetrics().entrySet()) {
				String metricName = metricEntry.getKey();
				Map<String, Number> rollup = metrics.get(metricName);
				if (rollup == null) {
					rollup = new ConcurrentHashMap<String, Number>();
					metrics.put(metricName, rollup);
				}
				Map<String, Number> data = (Map<String, Number>) metricEntry.getValue();
				addData(rollup, data);
			}
		}

		List<Component> rollupComponents = new ArrayList<Component>();
		for (Map.Entry<String, Map<String, Map<String, Number>>> entry : guids.entrySet()) {
			double durationSeconds = durationMillis * 0.001;
			String guid = entry.getKey();
			String[] guidSplit = guid.split("\\.");
			String guidSuffix = guidSplit[guidSplit.length - 1];
			Component component = new Component(componentPrefix + "_" + guidSuffix + "_Rollup", guid, (long) durationSeconds);
			Map<String, Map<String, Number>> metrics = entry.getValue();
			for (Map.Entry<String, Map<String, Number>> metric : metrics.entrySet()) {
				component.putMetric(metric.getKey(), metric.getValue());
			}
			rollupComponents.add(component);
		}

		return rollupComponents;
	}

	private void addData(Map<String, Number> metrics, Map<String, Number> data) {
		for (Map.Entry<String, Number> entry : data.entrySet()) {
			String stat = entry.getKey();
			Number incr = entry.getValue();
			Double initial = metrics.get(stat) != null ? (Double) metrics.get(stat) : 0;
			if (incr != null) {
				metrics.put(stat, initial + incr.doubleValue());
			}
		}
	}


}
