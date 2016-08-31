package com.terracotta.nrplugin.pojo;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 4/8/14
 * Time: 10:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class RatioMetric extends Metric {

    private static final long serialVersionUID = -8738437849912104248L;

    Metric numerator;
    Metric denominator;

    public RatioMetric() {
        super();
    }

    public Metric getNumerator() {
        return numerator;
    }

    public void setNumerator(Metric numerator) {
        this.numerator = numerator;
    }

    public Metric getDenominator() {
        return denominator;
    }

    public void setDenominator(Metric denominator) {
        this.denominator = denominator;
    }
}
