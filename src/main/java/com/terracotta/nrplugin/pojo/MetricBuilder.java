package com.terracotta.nrplugin.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeff on 9/24/2014.
 */
public class MetricBuilder {

    String name;
    String displayName;
    String dataPath;
    String reportingPath;
    Metric numeratorMetric;
    Metric denominatorMetric;
    List<String> reportingComponents = new ArrayList<String>();
    Metric.Source source;
    Metric.Unit unit;
    Metric.Type type;
    Metric.RatioType ratioType;
    Metric.RollupType rollupType;
    Integer maxWindowSize;
    boolean diff = false;
    boolean createDiff = false;

    public MetricBuilder(String name) {
        this.name = name;
    }

    public Metric build() {
        Metric metric = Metric.Type.ratio == type ? new RatioMetric() : new Metric();
        metric.setName(name);
        metric.setDisplayName(displayName);

        if (dataPath == null) {
            metric.setDataPath(toDataPath(this.name, this.source));
        } else {
            metric.setDataPath(dataPath);
        }

        metric.setReportingComponents(reportingComponents);
        metric.setSource(source);
        metric.setUnit(unit);
        metric.setType(type == null ? Metric.Type.regular : type);
        metric.setRatioType(ratioType);
        metric.setRollupType(rollupType);
        metric.setMaxWindowSize(maxWindowSize == null ? Metric.WINDOW_SIZE_DEFAULT : maxWindowSize);
        metric.setDiff(diff);
        metric.setCreateDiff(createDiff);

        if (Metric.Type.ratio.equals(type)) {
            ((RatioMetric) metric).setNumerator(numeratorMetric);
            ((RatioMetric) metric).setDenominator(denominatorMetric);
        }

        return metric;
    }

//	public String toReportingPath() {
//		String path = "";
//		Iterator<String> i = reportingComponents.iterator();
//		while (i.hasNext()) {
//			path += i.next();
//			if (i.hasNext()) path += NEW_RELIC_PATH_SEPARATOR;
//		}
//		path += "[" + unit.getName() + "]";
//		return path;
//	}

    private static String toDataPath(String metricName, Metric.Source source) {
        String dataPath = null;
        if (Metric.Source.client.equals(source) || Metric.Source.server.equals(source))
            dataPath = "$[?].statistics." + metricName;
        else if (Metric.Source.cache.equals(source)) dataPath = "$[?].attributes." + metricName;
        return dataPath;
    }

    public static MetricBuilder create(String metricName) {
        return new MetricBuilder(metricName);
    }

    public String getName() {
        return name;
    }

    public MetricBuilder addReportingPath(List<String> values) {
        this.reportingComponents.addAll(values);
        return this;
    }

    public MetricBuilder setDataPath(String dataPath) {
        this.dataPath = dataPath;
        return this;
    }

    public Metric.Source getSource() {
        return source;
    }

    public MetricBuilder setSource(Metric.Source source) {
        this.source = source;
        return this;
    }

    public MetricBuilder setUnit(Metric.Unit unit) {
        this.unit = unit;
        return this;
    }

    public MetricBuilder setType(Metric.Type type) {
        this.type = type;
        return this;
    }

    public MetricBuilder setRatioType(Metric.RatioType ratioType) {
        this.ratioType = ratioType;
        return this;
    }

    public MetricBuilder setMaxWindowSize(Integer maxWindowSize) {
        this.maxWindowSize = maxWindowSize;
        return this;
    }

    public MetricBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public MetricBuilder setNumeratorMetric(Metric numeratorMetric) {
        this.numeratorMetric = numeratorMetric;
        return this;
    }

    public MetricBuilder setDenominatorMetric(Metric denominatorMetric) {
        this.denominatorMetric = denominatorMetric;
        return this;
    }

    public MetricBuilder setDiff(boolean diff) {
        this.diff = diff;
        return this;
    }

    public MetricBuilder setReportingComponents(List<String> reportingComponents) {
        this.reportingComponents = reportingComponents;
        return this;
    }

    public MetricBuilder setCreateDiff(boolean createDiff) {
        this.createDiff = createDiff;
        return this;
    }

    public MetricBuilder setRollupType(Metric.RollupType rollupType) {
        this.rollupType = rollupType;
        return this;
    }
}
