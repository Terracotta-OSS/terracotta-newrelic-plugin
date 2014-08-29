package com.terracotta.nrplugin.pojo;

import com.terracotta.nrplugin.util.MetricUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/12/14
 * Time: 4:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class MetricDataset implements Serializable {

    private static final long serialVersionUID = 483809302495395084L;
    public static final int WINDOW_SIZE_DEFAULT = 100;

    Metric metric;
    SynchronizedDescriptiveStatistics statistics;
    Type type = Type.absolute;
    Map<String, String> actualVarReplaceMap = new HashMap<String, String>();

    public enum Type {absolute, diff}

    public MetricDataset() {
        statistics = new SynchronizedDescriptiveStatistics(WINDOW_SIZE_DEFAULT);
    }

    public MetricDataset(Metric metric, int windowSize) {
        this.metric = metric;
        statistics = new SynchronizedDescriptiveStatistics(windowSize);
    }

    public MetricDataset(Metric metric, int windowSize, Type type) {
        this.metric = metric;
        this.type = type;
        statistics = new SynchronizedDescriptiveStatistics(windowSize);
    }

    public MetricDataset(Metric metric, Type type, Map<String, String> actualVarReplaceMap) {
        this.metric = metric;
        this.type = type;
        this.actualVarReplaceMap = actualVarReplaceMap;
        statistics = new SynchronizedDescriptiveStatistics();
    }

    public void addValue(double value) {
        statistics.addValue(value);
    }

    public void putVarReplace(String key, String value) {
        actualVarReplaceMap.put(key, value);
    }

    public String getKey() {
        String key = metric.getBaseReportedPath() + MetricUtil.NEW_RELIC_PATH_SEPARATOR + type +
                MetricUtil.NEW_RELIC_PATH_SEPARATOR + metric.getName() + "[" + metric.getUnit() + "]";
        for (Map.Entry<String, String> entry : actualVarReplaceMap.entrySet()) {
            key = key.replaceAll(entry.getKey(), entry.getValue());
        }
        return key;
    }

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

    public Map<String, String> getActualVarReplaceMap() {
        return actualVarReplaceMap;
    }

    public void setActualVarReplaceMap(Map<String, String> actualVarReplaceMap) {
        this.actualVarReplaceMap = actualVarReplaceMap;
    }
}
