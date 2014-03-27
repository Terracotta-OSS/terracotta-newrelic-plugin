package com.newrelic.plugins.terracotta.metrics.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.plugins.terracotta.metrics.AbstractMetric;


/*
 * This metric calculates the added values during a time window
 * Helpful to get a total added count + rate/sec
 */
public class DifferentialMetricData extends SummaryMetricData implements Cloneable {
	private static Logger log = LoggerFactory.getLogger(DifferentialMetricData.class);
	
	private Double lastValue = null;

	public DifferentialMetricData(AbstractMetric parentMetric) {
		super(parentMetric);
	}

	protected DifferentialMetricData(DifferentialMetricData metricData) {
		super(metricData);
		if(null != metricData){
			this.parentMetric = metricData.parentMetric;
			this.lastValue = metricData.lastValue;
		}
	}

	@Override
	public void add(Number... newMetricValues){
		long timeCapture = System.currentTimeMillis();;

		//do nothing if the metric is null. Work with only one value here
		if(null != newMetricValues && newMetricValues.length > 0 && null != newMetricValues[0]){
			double newValue = newMetricValues[0].doubleValue();

			if(log.isDebugEnabled())
				log.debug(String.format("Metric %s - Adding raw value=%f", getParentMetricName(), newValue));
			
			if(null != lastValue){
				double diff = newValue - lastValue;
				if(log.isDebugEnabled())
					log.debug(String.format("Metric %s - Adding difference: NewValue[%f]-LastValue[%f]=%f", getParentMetricName(), newValue, lastValue, diff));

				//add the difference to the dataset
				dataset.addValue(diff);
				lastAddedValue = diff;
			} else {
				if(log.isDebugEnabled())
					log.debug(String.format("Metric %s - First pass...nothing added.", getParentMetricName()));
			}

			lastValue = new Double(newValue); // save the value for next

			if(log.isDebugEnabled())
				log.debug(String.format("Metric %s - Saving last value %f for use in next iteration", getParentMetricName(), lastValue, timeCapture));
		} else {
			if(log.isDebugEnabled())
				log.debug(String.format("Metric %s - value to add is null...will not count as a valid datapoint.", getParentMetricName()));
		}
	}

	@Override
	public DifferentialMetricData clone() throws CloneNotSupportedException {
		DifferentialMetricData cloned = new DifferentialMetricData(this);
		return cloned;
	}

	@Override
	public String toString() {
		StringBuilder outBuffer = new StringBuilder();
		String endl = "\n";
		outBuffer.append("DifferentialMetricData:").append(endl);
		outBuffer.append("lastValue: ").append(lastValue).append(endl);
		outBuffer.append(dataset.toString());

		return outBuffer.toString();
	}
}
