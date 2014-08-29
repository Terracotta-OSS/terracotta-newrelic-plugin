package com.terracotta.nrplugin.pojo;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 4/8/14
 * Time: 10:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class RatioMetric extends Metric {

    private static final long serialVersionUID = -8738437849912104248L;

    RatioMetric pair;
    String numeratorCount;
    String denominatorCount;

    public RatioMetric() {
        super();
    }

    public RatioMetric(String reportedPath, Map<String, String> dataPathVariables, Source source, Unit unit,
                       RatioMetric pair, String numeratorCount, String denominatorCount) {
        super(null, reportedPath, dataPathVariables, source, unit, null);
        this.pair = pair;
        this.numeratorCount = numeratorCount;
        this.denominatorCount = denominatorCount;
    }

    public boolean isHitRatio() {
        return StringUtils.containsIgnoreCase(numeratorCount, "hit");
    }

    public boolean isMissRatio() {
        return StringUtils.containsIgnoreCase(numeratorCount, "miss");
    }

    public RatioMetric getPair() {
        return pair;
    }

    public void setPair(RatioMetric pair) {
        this.pair = pair;
    }

    public String getNumeratorCount() {
        return numeratorCount;
    }

    public void setNumeratorCount(String numeratorCount) {
        this.numeratorCount = numeratorCount;
    }

    public String getDenominatorCount() {
        return denominatorCount;
    }

    public void setDenominatorCount(String denominatorCount) {
        this.denominatorCount = denominatorCount;
    }
}
