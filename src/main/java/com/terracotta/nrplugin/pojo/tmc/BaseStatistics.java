package com.terracotta.nrplugin.pojo.tmc;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseStatistics extends TmcBase {

    private static final long serialVersionUID = 144647955717932276L;

    String sourceId;
    Map<String, Object> statistics;

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
}
