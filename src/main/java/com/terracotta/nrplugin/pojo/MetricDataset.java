package com.terracotta.nrplugin.pojo;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/12/14
 * Time: 4:28 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
@Scope("prototype")
public class MetricDataset implements Serializable, Cloneable {

    private static final long serialVersionUID = 483809302495395084L;

    final Metric metric;
    final MetricDatasetComponent componentDetail;
    final SynchronizedDescriptiveStatistics statistics;

    public MetricDataset(Metric metric, MetricDatasetComponent componentDetail) {
        if (metric == null || componentDetail == null)
            throw new IllegalArgumentException("Metric or MetricDatasetComponent may not be null");

        this.metric = metric;
        this.componentDetail = componentDetail;
        this.statistics = new SynchronizedDescriptiveStatistics(metric.getMaxWindowSize());
    }

    public void addValue(double value) {
        statistics.addValue(value);
    }

    public static String getKey(Metric metric, MetricDatasetComponent componentDetail) {
        return new MetricDataset(metric, componentDetail).getKey();
    }

    public String getKey() {
        return componentDetail.getComponentName() + metric.getReportingPath();
    }

    public Metric getMetric() {
        return metric;
    }

    public DescriptiveStatistics getStatistics() {
        return statistics;
    }

    public MetricDatasetComponent getComponentDetail() {
        return componentDetail;
    }

    public String getStatisticsAsString() {
        return String.format("%s -- [min=%.2f,max=%.2f,sum=%f,samples=%d,sumsq=%.2f] ", getMetric().getReportingPath(), getStatistics().getMin(),
                getStatistics().getMax(), getStatistics().getSum(),
                getStatistics().getN(), getStatistics().getSumsq());
    }

    @Override
    public MetricDataset clone() throws CloneNotSupportedException {
        return (MetricDataset) super.clone();
    }
}
