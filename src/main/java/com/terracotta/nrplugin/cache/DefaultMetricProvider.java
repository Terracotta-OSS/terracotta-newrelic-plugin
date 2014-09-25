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

		payload.setAgent(new Agent(hostname, pid, version));
		payload.setComponents(new ArrayList<Component>(componentMap.values()));
		log.info("Returning " + numMetrics + " metric(s) from cache.");
		return payload;
	}

	private Component constructComponent(MetricDataset metricDataset) {
		double durationSeconds = durationMillis * 0.001;
		return new Component(componentPrefix + "_" + metricDataset.getComponentName(),
						metricDataset.getComponentGuid(), (long) durationSeconds);
	}
}
