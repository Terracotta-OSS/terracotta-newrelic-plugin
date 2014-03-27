package com.newrelic.plugins.terracotta.metrics.data;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.plugins.terracotta.metrics.AbstractMetric;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition;

public abstract class AbstractMetricData implements Cloneable {
	private static Logger log = LoggerFactory.getLogger(AbstractMetricData.class);
	
	protected AbstractMetric parentMetric = null;
	protected MetricResultDefinition resultDefinition = null;
	protected Number lastAddedValue = null;
	
	protected AbstractMetricData(AbstractMetricData metricData){
		if(null != metricData){
			this.parentMetric = metricData.parentMetric;
			this.resultDefinition = metricData.resultDefinition;
			this.lastAddedValue = metricData.lastAddedValue;
		}
	}
	
	protected AbstractMetricData(AbstractMetric parentMetric) {
		super();
		this.parentMetric = parentMetric;
	}

	public String getParentMetricName() {
		return (null != parentMetric)?parentMetric.getName():"null";
	}

	public MetricResultDefinition getResultDefinition() {
		return resultDefinition;
	}

	public void setResultDefinition(MetricResultDefinition resultDefinition) {
		this.resultDefinition = resultDefinition;
	}

	public void clear(){
		clearData();
		lastAddedValue = null;
	}
	
	protected abstract void clearData();
	
	public abstract Map<MetricResultDefinition.ReturnValueType, Number> computeMetricResult();
	
	public abstract void add(Number... metricValue);
	
	public abstract long getDataPointsCount();
	
	@Override
	public abstract AbstractMetricData clone() throws CloneNotSupportedException;
}
