package com.terracotta.nrplugin.pojo;

import com.terracotta.nrplugin.util.MetricUtil;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/24/14
 * Time: 8:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class Metric implements Serializable, Cloneable {

    private static final long serialVersionUID = -1055398640926238446L;
    public static final int WINDOW_SIZE_DEFAULT = 10000;

    String name;
    String displayName;
    //	String reportingPath;
    List<String> reportingComponents;
    String dataPath;
    Source source;
    Unit unit;
    Type type;
    RatioType ratioType;
    RollupType rollupType;
    Integer maxWindowSize = WINDOW_SIZE_DEFAULT;
    boolean diff;
    boolean createDiff;

    public Metric() {
    }

    private Metric(String name, String displayName, List<String> reportingComponents, String dataPath, Source source, Unit unit, Type type, RatioType ratioType, RollupType rollupType, Integer maxWindowSize, boolean diff, boolean createDiff) {
        this.name = name;
        this.displayName = displayName;
        this.reportingComponents = reportingComponents;
        this.dataPath = dataPath;
        this.source = source;
        this.unit = unit;
        this.type = type;
        this.ratioType = ratioType;
        this.rollupType = rollupType;
        this.maxWindowSize = maxWindowSize;
        this.diff = diff;
        this.createDiff = createDiff;
    }

    public String getName() {
        return name;
    }

//    public String getName() {
//        String[] split = reportingPath.split(MetricUtil.NEW_RELIC_PATH_SEPARATOR);
//		return split.length > 0 ? split[split.length - 1] : null;
//	}

    public String getBaseReportingPath() {
        String path = "";
        Iterator<String> i = reportingComponents.iterator();
        while (i.hasNext()) {
            path += i.next();
            if (i.hasNext()) path += MetricUtil.NEW_RELIC_PATH_SEPARATOR;
        }
        return path;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getReportingPath() {
        String path = getBaseReportingPath();
        path += MetricUtil.NEW_RELIC_PATH_SEPARATOR + (diff ? "diff" : "absolute");
        path += MetricUtil.NEW_RELIC_PATH_SEPARATOR + (displayName == null ? name : displayName);
        path += "[" + unit.getName() + "]";
        return path;
    }

//	public void setReportingPath(String reportingPath) {
//		this.reportingPath = reportingPath;
//	}

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getMaxWindowSize() {
        return maxWindowSize;
    }

    public void setMaxWindowSize(Integer maxWindowSize) {
        this.maxWindowSize = maxWindowSize;
    }

    public boolean isDiff() {
        return diff;
    }

    public void setDiff(boolean diff) {
        this.diff = diff;
    }

    public boolean isCreateDiff() {
        return createDiff;
    }

    public void setCreateDiff(boolean createDiff) {
        this.createDiff = createDiff;
    }

    public RatioType getRatioType() {
        if (ratioType == null) ratioType = RatioType.neither;
        return ratioType;
    }

    public List<String> getReportingComponents() {
        return reportingComponents;
    }

    public void setReportingComponents(List<String> reportingComponents) {
        this.reportingComponents = reportingComponents;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setRatioType(RatioType ratioType) {
        this.ratioType = ratioType;
    }

    public RollupType getRollupType() {
        if (rollupType == null) rollupType = RollupType.none;
        return rollupType;
    }

    public void setRollupType(RollupType rollupType) {
        this.rollupType = rollupType;
    }

    public static enum Type {regular, special, ratio}

    public static enum RatioType {hit, miss, neither}

    public static enum Source {server, client, cache, topologies}

    public static enum RollupType {sum, none}

    public static enum Unit {
        Count("count"),
        CountSecond("count/sec"),
        Bytes("bytes"),
        QueriesSecond("queries/sec"),
        Rate("value/sec"),
        BytesSecond("bytes/sec"),
        Percent("percent");

        final private String m_name;

        private Unit(final String name) {
            m_name = name;
        }

        public String getName() {
            return m_name;
        }
    }

    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", reportingPath='" + getReportingPath() + '\'' +
                ", dataPath='" + dataPath + '\'' +
                ", source=" + source +
                ", unit=" + unit +
                ", type=" + type +
                ", ratioType=" + ratioType +
                ", maxWindowSize=" + maxWindowSize +
                '}';
    }


    @Override
    public Metric clone() throws CloneNotSupportedException {
        return (Metric) super.clone();
    }
}
