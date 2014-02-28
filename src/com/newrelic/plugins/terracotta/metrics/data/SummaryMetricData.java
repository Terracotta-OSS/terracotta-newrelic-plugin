package com.newrelic.plugins.terracotta.metrics.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition.ReturnBundleType;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition.ReturnValueType;

/*
 * This metric calculates several useful statistics without storing the actual dataset values
 */
public class SummaryMetricData extends AbstractMetricData implements Cloneable {
	private static Logger log = LoggerFactory.getLogger(SummaryMetricData.class);
	protected final SummaryStatistics dataset = new SummaryStatistics();

	public SummaryMetricData(){
		super();
	}

	protected SummaryMetricData(SummaryMetricData metric) {
		if(null != metric)
			SummaryStatistics.copy(metric.dataset, this.dataset);
	}

	@Override
	public long getDataPointsCount() {
		return dataset.getN();
	}

	@Override
	public Map<ReturnValueType, Number> computeMetricResult() {
		Map <ReturnValueType, Number> returnMap = new HashMap<ReturnValueType, Number>();

		ReturnValueType[] returnValueTypes = null;
		if(resultDefinition == null || resultDefinition.getReturnBundleType() == ReturnBundleType.UNDEFINED || resultDefinition.getReturnBundleType() == ReturnBundleType.DETAILED) {
			returnValueTypes = new ReturnValueType[]{ReturnValueType.LASTADDED, ReturnValueType.DATAPPOINTCOUNT, ReturnValueType.SUM, ReturnValueType.SUMSQ, ReturnValueType.MIN, ReturnValueType.MAX, ReturnValueType.MEAN};
		} else if (resultDefinition.getReturnBundleType() == ReturnBundleType.SINGLE) {
			returnValueTypes = resultDefinition.getReturnValueTypes();
		}

		if(null != returnValueTypes && returnValueTypes.length > 0){
			Number returnValue = null;
			for(ReturnValueType type : returnValueTypes){
				switch(type){
				case LASTADDED:
					returnValue = lastAddedValue;
					break;
				case DATAPPOINTCOUNT:
					returnValue = dataset.getN();
					break;
				case SUM:
					returnValue = dataset.getSum();
					break;
				case SUMSQ:
					returnValue = dataset.getSumsq();
					break;
				case MIN:
					returnValue = dataset.getMin();
					break;
				case MAX:
					returnValue = dataset.getMax();
					break;
				case MEAN:
					returnValue = dataset.getMean();
					break;
				default:
					returnValue = null;
					break;
				}

				if(log.isDebugEnabled())
					log.debug(String.format("%s=%s", type.name(), (null != returnValue)?returnValue.toString():"null"));

				returnMap.put(type, returnValue);
			}
		}

		return returnMap;
	}

	@Override
	public void add(Number... newMetricValues){
		if(null != newMetricValues){
			for(Number val : newMetricValues){
				if(null != val && val.doubleValue() != Double.NaN){
					this.dataset.addValue(val.doubleValue());
					lastAddedValue = val;
				} else {
					if(log.isDebugEnabled())
						log.debug("value to add is null...will not count as a valid datapoint.");
				}
			}
		} else {
			if(log.isDebugEnabled())
				log.debug("value to add is null...will not count as a valid datapoint.");
		}
	}
	
	@Override
	public SummaryMetricData clone() throws CloneNotSupportedException {
		return new SummaryMetricData(this);
	}

	@Override
	public String toString() {
		StringBuilder outBuffer = new StringBuilder();
		String endl = "\n";
		outBuffer.append("SummaryMetricData:").append(endl);
		outBuffer.append(dataset.toString());

		return outBuffer.toString();
	}
}
