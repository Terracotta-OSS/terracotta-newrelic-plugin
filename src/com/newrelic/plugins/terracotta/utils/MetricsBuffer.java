package com.newrelic.plugins.terracotta.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsBuffer {
	private static Logger log = LoggerFactory.getLogger(MetricsBuffer.class);

	//using a simple hashMap because synchronizing all exposed access on it
	private final Map<String, Metric> metricsBuffer = new HashMap<String, Metric>();

	public MetricsBuffer() {
		super();
	}

	//this use synchronization to make sure that a bulk add is done atomically...and a getAllMetricsAndReset() call will block until this is done
	public void bulkAddMetrics(List<Metric> metrics){
		if(null != metrics && metrics.size() > 0){
			synchronized (metricsBuffer) {
				for(Metric metric : metrics){
					addMetric(metric);
				}
			}
		}
	}

	private void addMetric(Metric newMetric){
		if(null != newMetric){
			String metricKey = newMetric.getName();
			Metric existingMetric = metricsBuffer.get(metricKey);
			if(existingMetric == null){
				metricsBuffer.put(metricKey, new Metric(newMetric)); //make a copy of the metric object here
			} else {
				existingMetric.add(newMetric);
			}
		}
	}

	public Metric getSingleMetric(String metricKey){
		return metricsBuffer.get(metricKey);
	}

	//this use synchronization to make sure that getAll + Reset is done atomically, and no new metrics can be added before this operationb is finished.
	public List<Metric> getAllMetricsAndReset() {
		synchronized (metricsBuffer) {
			Set<String> keys = metricsBuffer.keySet();
			List<Metric> metrics = new ArrayList<Metric>(keys.size());

			for(String metricKey : keys){
				Metric metricInMap = metricsBuffer.get(metricKey);

				//add a copy of the metric to the resulting snapshot list
				metrics.add(new Metric(metricInMap));

				//reset that metric in the map once a copy has been added to the list
				metricInMap.reset();
			}

			return metrics;
		}
	}
}
