package com.newrelic.plugins.terracotta.metrics;

import com.newrelic.plugins.terracotta.utils.MetricUnit;

public class ServerMetric extends AbstractMetric {
	public static final String serverPrefix = String.format("%s/%s", METRICS_FAMILY_TC, "Servers");
	
	protected ServerMetric(ServerMetric metric){
		super(metric);
	}
	
	public ServerMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition) {
		super(name, unit, aggregationType, resultDefinition);
	}
	
	public ServerMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition, boolean publishEnabled) {
		super(name, unit, aggregationType, resultDefinition, publishEnabled);
	}
	
	@Override
	public ServerMetric clone() throws CloneNotSupportedException {
		ServerMetric cloned = new ServerMetric(this);
		if(null != this.getMetricData())
			cloned.setMetricData(this.getMetricData().clone());
		
		return cloned;
	}
	
	@Override
	public String getPrefix(){
		return String.format("%s", serverPrefix);
	}
}
