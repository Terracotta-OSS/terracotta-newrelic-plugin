package com.newrelic.plugins.terracotta.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metric {
	private static Logger log = LoggerFactory.getLogger(Metric.class);

	private final String name;
	private final NewRelicMetricType unit;

	private volatile float aggregateValue = 0.0F;
	private volatile float aggregateSumOfSquares = 0.0F;
	private volatile int dataPointsCount = 0;
	private volatile float min, max;

	public Metric(Metric metric) {
		super();

		if(null == metric)
			throw new IllegalArgumentException("metric may not be null...");

		this.name = metric.name;
		this.unit = metric.unit;

		initInternals(metric.dataPointsCount, metric.aggregateValue, metric.aggregateSumOfSquares, metric.min, metric.max);
	}

	public Metric(String name, NewRelicMetricType unit){
		super();
		this.name = name;
		this.unit = unit;

		initInternals();
	}

	public Metric(String name, NewRelicMetricType unit, Number startValue){
		super();
		this.name = name;
		this.unit = unit;

		if(null != startValue){
			float value = startValue.floatValue();
			initInternals(1, value, value * value, value, value);
		} else {
			initInternals();
		}
	}

	private void initInternals(int dataPointsCount, float aggregateValue, float aggregateSumOfSquares, float min, float max){
		this.dataPointsCount = dataPointsCount;
		this.aggregateValue = aggregateValue;
		this.aggregateSumOfSquares = aggregateSumOfSquares;
		this.min = min;
		this.max = max;
	}

	private void initInternals(int dataPointsCount, float aggregateValue, float aggregateSumOfSquares) {
		initInternals(dataPointsCount, aggregateValue, aggregateSumOfSquares, Float.MAX_VALUE, -Float.MAX_VALUE);
	}

	private void initInternals() {
		//using float's min/max values so that the first value added will be the min and max automatically...
		initInternals(0, 0.0F, 0.0F, Float.MAX_VALUE, -Float.MAX_VALUE);
	}

	public String getName() {
		return name;
	}

	public NewRelicMetricType getUnit() {
		return unit;
	}

	public String getMetricFullName() {
		return String.format("%s[%s]", name, unit);
	}

	/**
	 * Add {@link Metric} to the current.
	 *
	 * @param metric
	 * @return {@link this}
	 */
	public Metric add(Metric metric) {
		//make sure another is the same type of metric
		if(null != metric && name.equals(metric.name) && unit.equals(metric.unit)){
			this.dataPointsCount += metric.dataPointsCount;
			this.aggregateValue += metric.aggregateValue;
			this.aggregateSumOfSquares += metric.aggregateSumOfSquares;

			if (metric.min < this.min)
				this.min = metric.min;

			if (metric.max > this.max)
				this.max = metric.max;
		} else {
			log.warn("Cannot add 2 different metrics together. Name and Unit must match. Doing nothing.");
		}
		return this;
	}

	public void add(Number metricValue){
		add(1, metricValue);
	}

	private void add(int dataPoints, Number metricValue){
		if(null != metricValue){
			float value = metricValue.floatValue();

			dataPointsCount += dataPoints;
			aggregateValue += value;
			aggregateSumOfSquares += (value * value);

			if (value < this.min)
				this.min = value;

			if (value > this.max)
				this.max = value;
		}
	}

	public int getDataPointsCount() {
		return dataPointsCount;
	}

	public float getMin() {
		return min;
	}

	public float getMax() {
		return max;
	}

	public float getAggregateSumOfSquares() {
		return aggregateSumOfSquares;
	}

	public float getAggregateValue() {
		return aggregateValue;
	}

	public float getAverage() throws IllegalArgumentException {
		if(dataPointsCount == 0)
			throw new IllegalArgumentException(String.format("Metric %s[%s] - No datapoint is added yet...cannot call average function until at least 1 point is added", name, unit.getName()));

		return aggregateValue / dataPointsCount;
	}

	@Override
	public String toString() {
		return "Metric [name=" + name + ", unit=" + unit + ", aggregateValue="
				+ aggregateValue + ", aggregateSumOfSquares="
				+ aggregateSumOfSquares + ", dataPointsCount="
				+ dataPointsCount + ", min=" + min + ", max=" + max + "]";
	}
}
