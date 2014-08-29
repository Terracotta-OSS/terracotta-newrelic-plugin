package com.terracotta.nrplugin.pojo.nr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/12/14
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportedMetric implements Serializable {

    private static final long serialVersionUID = -5040545136571234201L;

    double min;
    double max;
    double total;
    double count;
    double sum_of_squares;

    public ReportedMetric() {
    }

    public ReportedMetric(double min, double max, double total, double count, double sum_of_squares) {
        this.min = min;
        this.max = max;
        this.total = total;
        this.count = count;
        this.sum_of_squares = sum_of_squares;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public double getSum_of_squares() {
        return sum_of_squares;
    }

    public void setSum_of_squares(double sum_of_squares) {
        this.sum_of_squares = sum_of_squares;
    }
}
