package com.terracotta.nrplugin.pojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Jeff on 9/24/2014.
 */
public class MetricBuilder {

	String metricName;
	String displayName;
	String dataPath;
	String reportingPath;
	String numeratorCount;
  String denominatorCount;
	RatioMetric pair;
	List<String> reportingComponents = new ArrayList<String>();
	Metric.Source source;
	Metric.Unit unit;
	Metric.Type type;
	Metric.RatioType ratioType;
	Integer maxWindowSize = MetricDataset.WINDOW_SIZE_DEFAULT;

	public static final String NEW_RELIC_PATH_SEPARATOR = "/";

	public MetricBuilder(String metricName) {
		this.metricName = metricName;
	}

	public Metric build() {
		Metric metric = Metric.Type.ratio == type ? new RatioMetric() : new Metric();
		if (dataPath == null) {
			metric.setDataPath(toDataPath(this.metricName, this.source));
		}
		else {
			metric.setDataPath(dataPath);
		}
		reportingComponents.add(displayName == null ? metricName : displayName);
		metric.setReportedPath(toReportingPath());
		metric.setSource(source);
		metric.setUnit(unit);
		metric.setType(type == null ? Metric.Type.regular : type);
		metric.setRatioType(ratioType);
		metric.setMaxWindowSize(maxWindowSize);

		if (Metric.Type.ratio.equals(type)) {
			((RatioMetric) metric).setNumeratorCount(numeratorCount);
			((RatioMetric) metric).setDenominatorCount(denominatorCount);
			((RatioMetric) metric).setPair(pair);
		}

		return metric;
	}

	public String toReportingPath() {
		String path = "";
		Iterator<String> i = reportingComponents.iterator();
		while (i.hasNext()) {
			path += i.next();
			if (i.hasNext()) path += NEW_RELIC_PATH_SEPARATOR;
		}
		path += "[" + unit.getName() + "]";
		return path;
	}

	private static String toDataPath(String metricName, Metric.Source source) {
		String dataPath = null;
		if (Metric.Source.client.equals(source) || Metric.Source.server.equals(source)) dataPath = "$[?].statistics." + metricName;
		else if (Metric.Source.cache.equals(source)) dataPath = "$[?].attributes." + metricName;
		return dataPath;
	}

	public static MetricBuilder create(String metricName) {
		return new MetricBuilder(metricName);
	}

	public String getMetricName() {
		return metricName;
	}

	public String getReportingPath() {
		return reportingPath;
	}

	public MetricBuilder addReportingPath(List<String> values) {
//		if (reportingPath == null) reportingPath = "";
//		this.reportingPath += toReportingPath(values);
		this.reportingComponents.addAll(values);
		return this;
	}

	public String getDataPath() {
		return dataPath;
	}

//	public MetricBuilder setDataPath(String dataPath) {
//		this.dataPath = dataPath;
//		return this;
//	}

	public Metric.Source getSource() {
		return source;
	}

	public MetricBuilder setSource(Metric.Source source) {
		this.source = source;
		return this;
	}

	public Metric.Unit getUnit() {
		return unit;
	}

	public MetricBuilder setUnit(Metric.Unit unit) {
		this.unit = unit;
		return this;
	}

	public Metric.Type getType() {
		return type;
	}

	public MetricBuilder setType(Metric.Type type) {
		this.type = type;
		return this;
	}

	public Metric.RatioType getRatioType() {
		return ratioType;
	}

	public MetricBuilder setRatioType(Metric.RatioType ratioType) {
		this.ratioType = ratioType;
		return this;
	}

	public int getMaxWindowSize() {
		return maxWindowSize;
	}

	public MetricBuilder setMaxWindowSize(int maxWindowSize) {
		this.maxWindowSize = maxWindowSize;
		return this;
	}

	public MetricBuilder setDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public MetricBuilder setDenominatorCount(String denominatorCount) {
		this.denominatorCount = denominatorCount;
		return this;
	}

	public MetricBuilder setNumeratorCount(String numeratorCount) {
		this.numeratorCount = numeratorCount;
		return this;
	}

	public MetricBuilder setPair(RatioMetric pair) {
		this.pair = pair;
		return this;
	}

}