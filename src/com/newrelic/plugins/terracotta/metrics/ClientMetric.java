package com.newrelic.plugins.terracotta.metrics;

import com.newrelic.plugins.terracotta.utils.MetricUnit;

public class ClientMetric extends AbstractMetric {
	public static final String METRICS_FAMILY_CLIENTS = "Clients";
	public static final String clientsPrefix = String.format("%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_CLIENTS);
	
	protected String clientID;

	public ClientMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition, String clientID) {
		super(name, unit, aggregationType, resultDefinition);
		this.clientID = clientID;
	}
	
	public ClientMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition, String clientID, boolean publishEnabled) {
		super(name, unit, aggregationType, resultDefinition, publishEnabled);
		this.clientID = clientID;
	}

	public String getClientID() {
		return clientID;
	}

	@Override
	public ClientMetric clone() throws CloneNotSupportedException {
		ClientMetric clone = new ClientMetric(name, unit, aggregationType, resultDefinition, clientID, publishEnabled);
		if(null != getMetricData())
			clone.setMetricData(getMetricData().clone());
		
		return clone;
	}

	@Override
	public String getPrefix(){
		return String.format("%s/%s", 
				clientsPrefix, 
				(null==clientID)?METRICS_ALL:"id/"+sanitize(clientID)
				);
	}
}
