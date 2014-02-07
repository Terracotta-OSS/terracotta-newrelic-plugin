package com.newrelic.plugins.terracotta.metrics;

import com.newrelic.plugins.terracotta.utils.MetricUnit;

public class ServerMetric extends AbstractMetric {
	public static final String serverPrefix = String.format("%s/%s", METRICS_FAMILY_TC, "Servers");
	
	public ServerMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition) {
		super(name, unit, aggregationType, resultDefinition);
	}
	
	@Override
	public ServerMetric clone() throws CloneNotSupportedException {
		ServerMetric clone = new ServerMetric(name, unit, aggregationType, resultDefinition);
		if(null != getMetricData())
			clone.setMetricData(getMetricData().clone());
		
		return clone;
	}
	
	@Override
	public String getPrefix(){
		return String.format("%s", serverPrefix);
	}
}
