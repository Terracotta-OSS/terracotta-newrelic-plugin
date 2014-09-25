package com.terracotta.nrplugin.pojo;

import com.terracotta.nrplugin.util.MetricUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/12/14
 * Time: 4:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class MetricDataset implements Serializable {

	private static final long serialVersionUID = 483809302495395084L;
	public static final int WINDOW_SIZE_DEFAULT = 10000;

	Metric metric;
	SynchronizedDescriptiveStatistics statistics;
	Type type = Type.absolute;
	String componentName;
	String componentGuid;

//	@Value("${com.saggs.terracotta.nrplugin.nr.agent.terracotta.guid}")
	String terracottaAgentGuid = "Terracotta";

//	@Value("${com.saggs.terracotta.nrplugin.nr.agent.ehcache.guid}")
	String ehcacheAgentGuid = "Ehcache";
//	final ReentrantLock lock = new ReentrantLock();

	public enum Type {absolute, diff}

	public MetricDataset() {
		statistics = new SynchronizedDescriptiveStatistics(WINDOW_SIZE_DEFAULT);
	}

	public MetricDataset(Metric metric, String componentName) {
		this();
		this.metric = metric;
		this.componentName = componentName;
		this.componentGuid = Metric.Source.client.equals(metric.getSource()) || Metric.Source.cache.equals(metric.getSource()) ?
				ehcacheAgentGuid : terracottaAgentGuid;
	}

	public MetricDataset(Metric metric, String componentName, int windowSize) {
		this(metric, componentName);
		statistics = new SynchronizedDescriptiveStatistics(windowSize);
	}

	public void addValue(double value) {
//		lock.lock();
//		try {
			statistics.addValue(value);
//		} finally {
//			lock.unlock();
//		}
	}

	public static String getKey(Metric metric, String componentName) {
		return new MetricDataset(metric, componentName).getKey();
	}

	public String getKey() {
		return componentName + metric.getReportingPath();
	}

//	public Map.Entry<String, Map<String, Number>> extractAndClearData() {
//		lock.lock();
//		Map.Entry<String, Map<String, Number>> result = null;
//		try {
//			Map<String, Number> values = new HashMap<String, Number>();
//			values.put(NEW_RELIC_MIN, getStatistics().getMin());
//			values.put(NEW_RELIC_MAX, getStatistics().getMax());
//			values.put(NEW_RELIC_TOTAL, getStatistics().getSum());
//			values.put(NEW_RELIC_COUNT, getStatistics().getN());
//			values.put(NEW_RELIC_SUM_OF_SQUARES, getStatistics().getSumsq());
//			result = new AbstractMap.SimpleEntry<String, Map<String, Number>>(getReportingPath(), values);
//			getStatistics().clear();
//		} finally {
//			lock.unlock();
//		}
//		return result;
//	}

//	public String getReportingPath() {
////		return metric.getBaseReportingPath() + MetricBuilder.NEW_RELIC_PATH_SEPARATOR + type +
////				MetricBuilder.NEW_RELIC_PATH_SEPARATOR + metric.getName() + "[" + metric.getUnit() + "]";
//		return metric.getReportingPath();
//	}

	public Double getLastValue() {
		return statistics.getElement((int) statistics.getN() - 1);
	}

	public Metric getMetric() {
		return metric;
	}

	public DescriptiveStatistics getStatistics() {
		return statistics;
	}

	public void setMetric(Metric metric) {
		this.metric = metric;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public String getComponentGuid() {
		return componentGuid;
	}

	public void setComponentGuid(String componentGuid) {
		this.componentGuid = componentGuid;
	}

	public void setStatistics(SynchronizedDescriptiveStatistics statistics) {
		this.statistics = statistics;
	}
}
