package com.newrelic.plugins.terracotta.metrics.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * This metric calculates the added values during a time window
 * Helpful to get a total added count + rate/sec
 */
public class DifferentialMetricData extends SummaryMetricData implements Cloneable {
	private static Logger log = LoggerFactory.getLogger(DifferentialMetricData.class);

	private volatile boolean firstPass = true;
	private volatile double lastValue = Double.NaN;
	private volatile long startCaptureTimeInMillis = Long.MIN_VALUE;
	private volatile long lastCaptureTimeInMillis = Long.MIN_VALUE;

	public DifferentialMetricData() {
		super();
	}

	protected DifferentialMetricData(DifferentialMetricData metric) {
		super(metric);
		if(null != metric){
			this.firstPass = metric.firstPass;
			this.lastValue = metric.lastValue;
			this.lastCaptureTimeInMillis = metric.lastCaptureTimeInMillis;
			this.startCaptureTimeInMillis = metric.startCaptureTimeInMillis;
		}
	}

	@Override
	public void add(Number... newMetricValues){
		long timeCapture = System.currentTimeMillis();;

		//do nothing if the metric is null. Work with only one value here
		if(null != newMetricValues && newMetricValues.length > 0 && null != newMetricValues[0]){
			double newValue = newMetricValues[0].doubleValue();

			//only assign the startTime if it is the first pass (and only add to the dataset if it is not the first time)
			if(firstPass){
				if(log.isDebugEnabled())
					log.debug(String.format("First pass...capturing start time in millis: %d", timeCapture));

				startCaptureTimeInMillis = timeCapture;
			} else {
				double diff = newValue - lastValue;
				if(log.isDebugEnabled())
					log.debug(String.format("Adding difference: NewValue[%f]-LastValue[%f]=%f", newValue, lastValue, diff));

				//add the difference to the dataset
				dataset.addValue(diff);
				lastAddedValue = diff;
			}

			lastValue = newValue; // save the value for next
			lastCaptureTimeInMillis = timeCapture; // save the end time for rate calculation

			if(log.isDebugEnabled())
				log.debug(String.format("Saving new value %f and current time %d for use in next iteration", lastValue, timeCapture));

			//make sure to set firstPass to false at this point
			firstPass = false;
		} else {
			if(log.isDebugEnabled())
				log.debug("value to add is null...will not count as a valid datapoint.");
		}
	}

	//	public double getRatePerSecond() {
	//		return getSum() * 1000 / (lastCaptureTimeInMillis - startCaptureTimeInMillis);
	//	}

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
		outBuffer.append("startCaptureTimeInMillis: ").append(startCaptureTimeInMillis).append(endl);
		outBuffer.append("lastCaptureTimeInMillis: ").append(lastCaptureTimeInMillis);
		outBuffer.append(dataset.toString());

		return outBuffer.toString();
	}
}
