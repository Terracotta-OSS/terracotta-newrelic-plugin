package com.newrelic.plugins.terracotta.metrics;

import java.util.HashMap;
import java.util.Map;

import com.newrelic.plugins.terracotta.utils.MetricUnit;

public class NewRelicDeliveryBundle {
	private final String metricName;
	private final MetricUnit metricUnit;
	private final NewRelicDeliveryType deliveryType;
	private final Map<NewRelicValuesType, Number> deliveryValues = new HashMap<NewRelicValuesType, Number>();
	
	public enum NewRelicDeliveryType
	{
		SIMPLE,
		DETAILED
	}
	
	public enum NewRelicValuesType
	{
		ABSOLUTE,
		DATAPPOINTCOUNT,
	    SUM,
	    MIN,
	    MAX,
	    SUMSQ,
	    MEAN,
	    MEDIAN,
	    P95
	}
	
	public NewRelicDeliveryBundle(String metricName, MetricUnit metricUnit, NewRelicDeliveryType deliveryType) {
		super();
		this.metricName = metricName;
		this.metricUnit = metricUnit;
		this.deliveryType = deliveryType;
	}
	
	public NewRelicDeliveryBundle addBundleValue(NewRelicValuesType type, Number value){
		deliveryValues.put(type, value);
		return this;
	}
	
	public float getBundleValue(NewRelicValuesType type){
		Number deliveryValue = deliveryValues.get(type);
		float value;
		if(null != deliveryValue)
			value = deliveryValue.floatValue();
		else
			value = 0F;
		
		return value;
	}
}
