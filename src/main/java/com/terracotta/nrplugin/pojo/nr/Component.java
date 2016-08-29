package com.terracotta.nrplugin.pojo.nr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.terracotta.nrplugin.pojo.MetricDataset;
import com.terracotta.nrplugin.util.MetricUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/24/14
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Component {

    String name;
    String guid;
    long duration;
    Map<String, Object> metrics;

    public Component() {
    }

    public Component(String name, String guid, long duration) {
        this();
        this.name = name;
        this.guid = guid;
        this.duration = duration;
    }

    public Component(String name, String guid, long duration, Map<String, Object> metrics) {
        this(name, guid, duration);
        this.metrics = metrics;
    }

    public void putMetric(MetricDataset metricDataset, MetricUtil metricUtil, boolean full) {
        Map.Entry<String, Map<String, Number>> metricJson = metricUtil.metricAsJson(metricDataset, full);
        putMetric(metricJson.getKey(), metricJson.getValue());
    }

    public void putMetric(String key, Object value) {
        if (metrics == null) {
            metrics = new ConcurrentHashMap<String, Object>();
        }
        metrics.put(key, value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }
}
