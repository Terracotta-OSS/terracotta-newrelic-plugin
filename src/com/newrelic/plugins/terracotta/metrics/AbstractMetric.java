package com.newrelic.plugins.terracotta.metrics;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.plugins.terracotta.metrics.data.AbstractMetricData;
import com.newrelic.plugins.terracotta.metrics.data.AggregatedRatioData;
import com.newrelic.plugins.terracotta.metrics.data.DifferentialMetricData;
import com.newrelic.plugins.terracotta.metrics.data.ExtentedMetricData;
import com.newrelic.plugins.terracotta.metrics.data.SummaryMetricData;
import com.newrelic.plugins.terracotta.utils.MetricUnit;

public abstract class AbstractMetric implements Cloneable {
	private static Logger log = LoggerFactory.getLogger(AbstractMetric.class);

	public static final String METRICS_ALL = "All";
	public static final String METRICS_FAMILY_TC = "Terracotta";

	protected final String name;
	protected final MetricUnit unit;
	protected final AggregationType aggregationType;
	protected final MetricResultDefinition resultDefinition;

	protected AbstractMetricData metricData;

	protected AbstractMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition) {
		super();
		this.name = name;
		this.unit = unit;
		this.aggregationType = aggregationType;
		this.resultDefinition = resultDefinition;
	}

	public enum AggregationType
	{
		SUMMARY,
		EXTENDED,
		DIFFERENTIAL_SUMMARY,
		AGGREGATED_RATIO
	}

	public MetricResultDefinition getResultDefinition() {
		return resultDefinition;
	}

	public static class MetricResultDefinition {
		private ReturnBundleType returnBundleType = ReturnBundleType.UNDEFINED;
		private ReturnValueType[] returnValueTypes = null;

		public MetricResultDefinition(ReturnBundleType returnBundleType,
				ReturnValueType[] returnValueTypes) {
			super();
			this.returnBundleType = returnBundleType;
			this.returnValueTypes = returnValueTypes;
		}

		public static MetricResultDefinition createDetailed(){
			return new MetricResultDefinition(ReturnBundleType.DETAILED, null);
		}
		
		public static MetricResultDefinition createCustomMax(){
			return new MetricResultDefinition(ReturnBundleType.CUSTOM, new ReturnValueType[]{ReturnValueType.MAX});
		}
		
		public static MetricResultDefinition createCustomAbsolute(){
			return new MetricResultDefinition(ReturnBundleType.CUSTOM, new ReturnValueType[]{ReturnValueType.ABSOLUTE});
		}

		public static MetricResultDefinition createCustomAverage(){
			return new MetricResultDefinition(ReturnBundleType.CUSTOM, new ReturnValueType[]{ReturnValueType.MEAN});
		}

		public enum ReturnBundleType
		{
			UNDEFINED,
			CUSTOM,
			DETAILED
		}

		public enum ReturnValueType
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

		public ReturnBundleType getReturnBundleType() {
			return returnBundleType;
		}

		public void setReturnBundleType(ReturnBundleType returnBundleType) {
			this.returnBundleType = returnBundleType;
		}

		public ReturnValueType[] getReturnValueTypes() {
			return returnValueTypes;
		}

		public void setReturnValueTypes(ReturnValueType[] returnValueTypes) {
			this.returnValueTypes = returnValueTypes;
		}
	}

	public long getDataPointsCount(){
		log.info("begin getDataPointsCount");
		
		long datapoints = (null != metricData)?metricData.getDataPointsCount():0;
		
		if(log.isDebugEnabled())
			log.debug(String.format("%d datapoints for metric[%s]", datapoints, getNameWithUnit()));
		
		return datapoints;
	}
	
	public Map<MetricResultDefinition.ReturnValueType, Number> getMetricDataResults(){
		log.info("begin getMetricDataResults");
		return metricData.computeMetricResult();
	}

	public void addValue(Number... newMetricValues){
		if(null != newMetricValues){
			for(Number val : newMetricValues){
				if(null != val){
					if(log.isDebugEnabled())
						log.debug(String.format("Adding value=%f for metric[%s]",(null != val)?val.doubleValue():0.0D, getNameWithUnit()));
				}
			}
		}
		
		if(null == metricData) {
			createMetricData(newMetricValues);
		}
		else {
			metricData.add(newMetricValues);
		}
	}

	private void createMetricData(Number... values){
		if(log.isDebugEnabled())
			log.debug(String.format("Creating metricData using type [%s] for metric[%s]", aggregationType.name(), getNameWithUnit()));
		
		switch (aggregationType) {
		case EXTENDED:
			metricData = new ExtentedMetricData();
			break;
		case SUMMARY:
			metricData = new SummaryMetricData();
			break;
		case DIFFERENTIAL_SUMMARY:
			metricData = new DifferentialMetricData();
			break;
		case AGGREGATED_RATIO:
			metricData = new AggregatedRatioData();
			break;
		default:
			break;
		}
		
		metricData.setResultDefinition(resultDefinition);
		metricData.add(values);
	}
	
	protected String sanitize(String name){
		return (null != name)?name.toLowerCase().replace("/", ":"):"";
	}
	
	public abstract String getPrefix();

	public String getNameWithUnit(){
		return new StringBuilder(getName()).append("-[").append(unit.getName()).append("]").toString();
	}

	public String getShortName() {
		return name;
	}

	public String getName() {
		StringBuilder outBuffer = new StringBuilder(getPrefix());
        if(!name.startsWith("/")) outBuffer.append("/");
        outBuffer.append(name);
		
		return outBuffer.toString();
	}

	public MetricUnit getUnit() {
		return unit;
	}

	public AggregationType getAggregationType() {
		return aggregationType;
	}

	public AbstractMetricData getMetricData() {
		return metricData;
	}

	public void setMetricData(AbstractMetricData metricData) {
		this.metricData = metricData;
	}

	@Override
	public abstract AbstractMetric clone() throws CloneNotSupportedException;

	@Override
	public String toString() {
		return "AbstractMetric [name=" + name + ", unit=" + unit
				+ ", metricData=" + metricData.toString() + "]";
	}
}
