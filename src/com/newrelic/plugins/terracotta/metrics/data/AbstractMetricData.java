package com.newrelic.plugins.terracotta.metrics.data;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition;

public abstract class AbstractMetricData implements Cloneable {
	private static Logger log = LoggerFactory.getLogger(AbstractMetricData.class);
	
	protected MetricResultDefinition resultDefinition = null;
	protected Number lastAddedValue = null;
	
	public MetricResultDefinition getResultDefinition() {
		return resultDefinition;
	}

	public void setResultDefinition(MetricResultDefinition resultDefinition) {
		this.resultDefinition = resultDefinition;
	}

	public abstract Map<MetricResultDefinition.ReturnValueType, Number> computeMetricResult();
	
	public abstract void add(Number... metricValue);
	
	public abstract long getDataPointsCount();
	
	@Override
	public abstract AbstractMetricData clone() throws CloneNotSupportedException;
}
