package com.terracotta.nrplugin.pojo;

import com.terracotta.nrplugin.util.MetricUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
@Component
@Scope("prototype")
public class MetricDataset implements Serializable {

	private static final long serialVersionUID = 483809302495395084L;

	Metric metric;
	SynchronizedDescriptiveStatistics statistics;
	Type type = Type.absolute;
	String componentName;

	public enum Type {absolute, diff}

	public MetricDataset(Metric metric, String componentName) {
		this.metric = metric;
		this.componentName = componentName;
		this.statistics = new SynchronizedDescriptiveStatistics(metric.getMaxWindowSize());
	}

	public void addValue(double value) {
			statistics.addValue(value);
	}

	public static String getKey(Metric metric, String componentName) {
		return new MetricDataset(metric, componentName).getKey();
	}

	public String getKey() {
		return componentName + metric.getReportingPath();
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

	public void setStatistics(SynchronizedDescriptiveStatistics statistics) {
		this.statistics = statistics;
	}
}
