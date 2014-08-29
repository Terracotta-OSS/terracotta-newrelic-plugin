package com.terracotta.nrplugin.pojo.tmc;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 10:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class UnreadOperatorEventCount implements Serializable {

    private static final long serialVersionUID = 6663293253666036331L;

    int ERROR;
    int CRITICAL;
    int WARN;
    int INFO;
    int DEBUG;

    public int getERROR() {
        return ERROR;
    }

    public void setERROR(int ERROR) {
        this.ERROR = ERROR;
    }

    public int getCRITICAL() {
        return CRITICAL;
    }

    public void setCRITICAL(int CRITICAL) {
        this.CRITICAL = CRITICAL;
    }

    public int getWARN() {
        return WARN;
    }

    public void setWARN(int WARN) {
        this.WARN = WARN;
    }

    public int getINFO() {
        return INFO;
    }

    public void setINFO(int INFO) {
        this.INFO = INFO;
    }

    public int getDEBUG() {
        return DEBUG;
    }

    public void setDEBUG(int DEBUG) {
        this.DEBUG = DEBUG;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("ERROR", ERROR)
                .append("CRITICAL", CRITICAL)
                .append("WARN", WARN)
                .append("INFO", INFO)
                .append("DEBUG", DEBUG)
                .toString();
    }
}
