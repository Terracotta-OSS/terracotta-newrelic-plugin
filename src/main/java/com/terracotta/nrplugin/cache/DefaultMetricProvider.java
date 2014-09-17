package com.terracotta.nrplugin.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.*;

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

	long pid;

//	@Override
//	public Map<String, Map<String, Object>> getAllMetrics() {
//		log.debug("Gathering stats from cache...");
//		Map<String, Map<String, Object>> allMetrics = new HashMap<String, Map<String, Object>>();
//		Map<String, Collection<MetricDataset>> datasetMap = new HashMap<String, Collection<MetricDataset>>();
//		int numMetrics = 0;
//
//		// Get absolute metrics
//		for (Object key : statsCache.getKeys()) {
//			Element element = statsCache.get((key));
//			if (element != null && element.getObjectValue() instanceof MetricDataset) {
//				MetricDataset metricDataset = (MetricDataset) element.getObjectValue();
//				Collection<MetricDataset> componentMetrics = datasetMap.get(metricDataset.getComponentName());
//				if (componentMetrics == null) componentMetrics = new ArrayList<MetricDataset>();
//				componentMetrics.add(metricDataset);
//				datasetMap.put(metricDataset.getComponentName(), componentMetrics);
//				numMetrics++;
//			}
//		}
//
//		for (Map.Entry<String, Collection<MetricDataset>> entry : datasetMap.entrySet()) {
//			Map<String, Object> metricsAsJson = metricUtil.metricsAsJson(entry.getValue());
//			allMetrics.put(entry.getKey(), metricsAsJson);
////			metricsAsJson.putAll(metricUtil.metricsAsJson(entry.getValue()));
//		}
//
//		// Get diff metrics
////		for (Object key : diffsCache.getKeys()) {
////			Element element = diffsCache.get((key));
////			if (element != null && element.getObjectValue() instanceof Map) {
////				metrics.put((String) element.getObjectKey(), element.getObjectValue());
////			}
////		}
//
//
//		log.info("Returning " + numMetrics + " metric(s) from cache.");
//		return allMetrics;
//	}

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
					double durationSeconds = durationMillis * 0.001;
					component = new Component(componentPrefix + "_" + metricDataset.getComponentName(),
							metricDataset.getComponentGuid(), (long) durationSeconds);
				}
				Map.Entry entry = metricUtil.metricAsJson(metricDataset);
				component.putMetric((String) entry.getKey(), entry.getValue());
				componentMap.put(metricDataset.getComponentName(), component);
				numMetrics++;
			}
		}
		payload.setAgent(new Agent(hostname, pid, version));
		payload.setComponents(new ArrayList<Component>(componentMap.values()));
		log.info("Returning " + numMetrics + " metric(s) from cache.");
		return payload;
	}
}
