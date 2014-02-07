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
	private volatile double lastValue = 0D;
	private volatile long startCaptureTimeInMillis = Long.MIN_VALUE;
	private volatile long lastCaptureTimeInMillis = Long.MIN_VALUE;

	public DifferentialMetricData() {
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
				if(log.isDebugEnabled())
					log.debug(String.format("Adding difference: NewValue[%f]-LastValue[%f]=%f",newValue, lastValue, newValue - lastValue));
				
				//add the difference to the dataset
				dataset.addValue(newValue - lastValue);
			}
			
			lastValue = newValue; // save the value for next
			lastCaptureTimeInMillis = timeCapture; // save the end time for rate calculation
			
			if(log.isDebugEnabled())
				log.debug(String.format("Saving new value %f and current time %d for use in next iteration", lastValue, timeCapture));
			
			//make sure to set firstPass to false at this point
			firstPass = false;
		}
	}

	public double getRatePerSecond() {
		return getSum() * 1000 / (lastCaptureTimeInMillis - startCaptureTimeInMillis);
	}

	@Override
	public DifferentialMetricData clone() throws CloneNotSupportedException {
		DifferentialMetricData cloned = new DifferentialMetricData();
		cloned.firstPass = this.firstPass;
		cloned.lastValue = this.lastValue;
		cloned.lastCaptureTimeInMillis = this.lastCaptureTimeInMillis;
		cloned.startCaptureTimeInMillis = this.startCaptureTimeInMillis;
		
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
