package com.newrelic.plugins.terracotta.metrics.data;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition.ReturnValueType;

public class AggregatedRatioData extends AbstractMetricData {
	private static Logger log = LoggerFactory.getLogger(AggregatedRatioData.class);

	private volatile double aggregateDividendValue = 0.0D;
	private volatile double aggregateDivisorValue = 1.0D;
	private volatile long dataPointsCount = 0;

	public AggregatedRatioData() {
		super();
	}

	@Override
	public long getDataPointsCount() {
		return dataPointsCount;
	}

	@Override
	public Map<ReturnValueType, Number> computeMetricResult() {
		Map <ReturnValueType, Number> returnMap = new HashMap<ReturnValueType, Number>();

		double totalDivisor = aggregateDividendValue + aggregateDivisorValue;
		if(totalDivisor != 0.0D){
			double quotien = aggregateDividendValue / (totalDivisor);
			if(log.isDebugEnabled())
				log.debug(String.format("Calculated ratio: %d", quotien));

			returnMap.put(ReturnValueType.ABSOLUTE, quotien);
		} else {
			log.warn("Cannot divide by 0. Returning null");
			returnMap.put(ReturnValueType.ABSOLUTE, null);
		}

		return returnMap;
	}

	@Override
	public AbstractMetricData clone() throws CloneNotSupportedException {
		AggregatedRatioData cloned = new AggregatedRatioData();
		cloned.aggregateDividendValue = this.aggregateDividendValue;
		cloned.aggregateDivisorValue = this.aggregateDivisorValue;
		cloned.dataPointsCount = this.dataPointsCount;

		return cloned;
	}

	@Override
	public void add(Number... newMetricValues){
		//do nothing if the metric is null. Work with 2 values here
		if(null != newMetricValues && newMetricValues.length >= 2 && null != newMetricValues[0] && null != newMetricValues[1]){
			aggregateDividendValue += newMetricValues[0].doubleValue();
			aggregateDivisorValue += newMetricValues[1].doubleValue();

			dataPointsCount++;
		}
	}

	@Override
	public String toString() {
		return "AggregatedRatioData [aggregateDividendValue="
				+ aggregateDividendValue + ", aggregateDivisorValue="
				+ aggregateDivisorValue + ", dataPointsCount="
				+ dataPointsCount + "]";
	}
}
