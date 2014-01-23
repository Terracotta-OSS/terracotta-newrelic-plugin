package com.newrelic.plugins.terracotta.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//must be thread safe, as 2 thread will access this
public class MetricsBuffer {
	private static Logger log = LoggerFactory.getLogger(MetricsBuffer.class);

	//using a simple hashMap because synchronizing all exposed access on it
	private final Map<String, Metric> metricsBuffer = new HashMap<String, Metric>();

	public MetricsBuffer() {
		super();
	}

	//this use synchronization to make sure that a bulk add is done atomically...and a getAllMetricsAndReset() call will block until this is done
	public void bulkAddMetrics(Metric[] metrics){
		synchronized (metricsBuffer) {
			if(null != metrics && metrics.length > 0){
				for(Metric newMetric : metrics){
					if(null != newMetric){
						String newMetricKey = newMetric.getName();
						Metric existingMetric = metricsBuffer.get(newMetricKey);
						if(existingMetric == null){
							metricsBuffer.put(newMetricKey, new Metric(newMetric)); //make a copy of the metric object here
						} else {
							existingMetric.add(newMetric);
						}
					}
				}
			}
		}
	}

	public Metric getSingleMetric(String metricKey){
		synchronized (metricsBuffer) {
			return metricsBuffer.get(metricKey);
		}
	}

	//this use synchronization to make sure that getAll + Reset is done atomically, and no new metrics can be added before this operation is finished.
	public Metric[] getAllMetricsAndReset() {
		synchronized (metricsBuffer) {
			Metric[] metrics = null;
			try{
				Set<String> keys = metricsBuffer.keySet();
				metrics = new Metric[keys.size()];
				
				//perform a deep copy of all the metrics objects
				int counter = 0;
				for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();counter++) {
					String metricKey = iterator.next();
					metrics[counter] = new Metric(metricsBuffer.get(metricKey));
				}
			} finally {
				metricsBuffer.clear();
			}
			return metrics;
		}
	}
}
