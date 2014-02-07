package com.newrelic.plugins.terracotta.metrics.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.plugins.terracotta.metrics.AbstractMetric;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition.ReturnBundleType;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition.ReturnValueType;

/*
 * This metric is similar to @SummaryMetric but it does store the actual dataset values in order to offer more extensive calculation capabilities (i.e. percentiles, etc...)
 */
public class ExtentedMetricData extends AbstractMetricData implements Cloneable {
	private static Logger log = LoggerFactory.getLogger(ExtentedMetricData.class);

	private final DescriptiveStatistics dataset = new DescriptiveStatistics();;

	public ExtentedMetricData(){
		super();
	}

	public ExtentedMetricData(ExtentedMetricData metric) {
		if(null != metric)
			DescriptiveStatistics.copy(metric.dataset, this.dataset);
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
			returnValueTypes = new ReturnValueType[]{ReturnValueType.DATAPPOINTCOUNT, ReturnValueType.SUM, ReturnValueType.SUMSQ, ReturnValueType.MIN, ReturnValueType.MAX, ReturnValueType.MEAN};
		} else if (resultDefinition.getReturnBundleType() == ReturnBundleType.CUSTOM) {
			returnValueTypes = resultDefinition.getReturnValueTypes();
		}

		if(null != returnValueTypes && returnValueTypes.length > 0){
			Number returnValue = null;
			for(ReturnValueType type : returnValueTypes){
				switch(type){
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
				case MEDIAN:
					returnValue = dataset.getPercentile(50);
					break;
				case P95:
					returnValue = dataset.getPercentile(95);
					break;
				default:
					returnValue = null;
					break;
				}
				
				if(log.isDebugEnabled())
					log.debug(String.format("%s=%f", type.name(), (null != returnValue)?returnValue.doubleValue():0.0D));
				
				returnMap.put(type, returnValue);
			}
		}

		return returnMap;
	}

	@Override
	public void add(Number... newMetricValues){
		if(null != newMetricValues){
			for(Number val : newMetricValues){
				if(null != val){
					this.dataset.addValue(val.doubleValue());
				}
			}
		}
	}

	@Override
	public ExtentedMetricData clone() throws CloneNotSupportedException {
		return new ExtentedMetricData(this);
	}

	@Override
	public String toString() {
		StringBuilder outBuffer = new StringBuilder();
		String endl = "\n";
		outBuffer.append("ExtentedMetricData:").append(endl);
		outBuffer.append(dataset.toString());

		return outBuffer.toString();
	}
}
