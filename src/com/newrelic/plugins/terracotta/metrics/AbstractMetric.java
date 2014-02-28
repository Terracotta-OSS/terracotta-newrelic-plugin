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

public abstract class AbstractMetric implements Comparable<AbstractMetric>, Cloneable {
	private static Logger log = LoggerFactory.getLogger(AbstractMetric.class);

	public static final String METRICS_ALL = "All";
	public static final String METRICS_FAMILY_TC = "Terracotta";

	protected boolean publishEnabled = true;
	protected final String name;
	protected final MetricUnit unit;
	protected final AggregationType aggregationType;
	protected final MetricResultDefinition resultDefinition;

	protected AbstractMetricData metricData;

	protected AbstractMetric(AbstractMetric metric) {
		if(metric == null)
			throw new IllegalArgumentException("Metric cannot be null");
		
		this.name = metric.name;
		this.unit = metric.unit;
		this.aggregationType = metric.aggregationType;
		this.resultDefinition = metric.resultDefinition;
		this.publishEnabled = metric.publishEnabled;
	}
	
	protected AbstractMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition) {
		this(name, unit, aggregationType, resultDefinition, true);
	}

	protected AbstractMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition, boolean publishEnabled) {
		super();
		this.name = name;
		this.unit = unit;
		this.aggregationType = aggregationType;
		this.resultDefinition = resultDefinition;
		this.publishEnabled = publishEnabled;
	}

	public enum AggregationType
	{
		SUMMARY,
		EXTENDED,
		DIFFERENTIAL_SUMMARY,
		AGGREGATED_RATIO
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

		public static MetricResultDefinition createSingleMax(){
			return new MetricResultDefinition(ReturnBundleType.SINGLE, new ReturnValueType[]{ReturnValueType.MAX});
		}

		public static MetricResultDefinition createSingleAbsolute(){
			return new MetricResultDefinition(ReturnBundleType.SINGLE, new ReturnValueType[]{ReturnValueType.ABSOLUTE});
		}

		public static MetricResultDefinition createSingleSum(){
			return new MetricResultDefinition(ReturnBundleType.SINGLE, new ReturnValueType[]{ReturnValueType.SUM});
		}

		public static MetricResultDefinition createSingleAverage(){
			return new MetricResultDefinition(ReturnBundleType.SINGLE, new ReturnValueType[]{ReturnValueType.MEAN});
		}

		public static MetricResultDefinition createSingleLastAdded(){
			return new MetricResultDefinition(ReturnBundleType.SINGLE, new ReturnValueType[]{ReturnValueType.LASTADDED});
		}

		public enum ReturnBundleType
		{
			UNDEFINED,
			SINGLE,
			DETAILED
		}

		public enum ReturnValueType
		{
			ABSOLUTE,
			LASTADDED,
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
		if(log.isDebugEnabled()){
			if(null != newMetricValues){
				for(Number val : newMetricValues){
					log.debug(String.format("Adding value=%s for metric[%s]",(null != val)?val.toString():"null", getNameWithUnit()));
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

	public abstract String getPrefix();

	protected String sanitize(String name){
		return (null != name)?name.toLowerCase().replace("/", ":"):"";
	}

	public MetricResultDefinition getResultDefinition() {
		return resultDefinition;
	}

	public void setPublishEnabled(boolean publishEnabled) {
		this.publishEnabled = publishEnabled;
	}

	public boolean isPublishEnabled() {
		return publishEnabled;
	}

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

	/**
	  * @param aThat is a non-null Account.
	  *
	  * @throws NullPointerException if aThat is null.
	  */
	@Override
	public int compareTo(AbstractMetric obj) {
		return this.getNameWithUnit().compareTo(obj.getNameWithUnit());
	}

	@Override
	public String toString() {
		return "AbstractMetric [name=" + getName() + ", unit=" + unit
				+ ", metricData=" + metricData.toString() + "]";
	}
}
