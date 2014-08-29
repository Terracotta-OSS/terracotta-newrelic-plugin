package com.terracotta.nrplugin.pojo;

import com.terracotta.nrplugin.util.MetricUtil;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/24/14
 * Time: 8:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class Metric implements Serializable {

    private static final long serialVersionUID = -1055398640926238446L;

    String reportedPath;
    String dataPath;
    Map<String, String> dataPathVariables = new HashMap<String, String>();
    Source source;
    Unit unit;
    RatioType ratioType;

    public Metric() {
    }

    public Metric(String dataPath, String reportedPath, Source source, Unit unit) {
        this();
        if (reportedPath == null) throw new IllegalArgumentException("reportedPath cannot be null.");
//        if (dataPath == null) throw new IllegalArgumentException("dataPath cannot be null.");
        this.dataPath = dataPath;
        this.reportedPath = reportedPath;
        this.source = source;
        this.unit = unit;
    }

    public Metric(String dataPath, String reportedPath, Map<String, String> dataPathVariables, Source source,
                  Unit unit, RatioType ratioType) {
        this(dataPath, reportedPath, source, unit);
        this.dataPathVariables = dataPathVariables;
        this.ratioType = ratioType;
    }

    public String getName() {
        String[] split = reportedPath.split(MetricUtil.NEW_RELIC_PATH_SEPARATOR);
        return split.length > 0 ? split[split.length - 1] : null;
    }

    public String getBaseReportedPath() {
//        List<String> split = Arrays.asList(reportedPath.split(MetricUtil.NEW_RELIC_PATH_SEPARATOR));
        String[] split = reportedPath.split(MetricUtil.NEW_RELIC_PATH_SEPARATOR);
        String[] spliced = Arrays.copyOf(split, split.length - 1);
        return StringUtils.join(spliced, MetricUtil.NEW_RELIC_PATH_SEPARATOR);
    }

    public boolean hasDynamicPath() {
        return dataPathVariables.size() > 0;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getReportedPath() {
        return reportedPath;
    }

    public void setReportedPath(String reportedPath) {
        this.reportedPath = reportedPath;
    }

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

    public Map<String, String> getDataPathVariables() {
        return dataPathVariables;
    }

    public void setDataPathVariables(Map<String, String> dataPathVariables) {
        this.dataPathVariables = dataPathVariables;
    }

    public RatioType getRatioType() {
        if (ratioType == null) ratioType = RatioType.neither;
        return ratioType;
    }

    public void setRatioType(RatioType ratioType) {
        this.ratioType = ratioType;
    }

    public static enum RatioType {hit, miss, neither}

    public static enum Source {server, client, cache, topologies}

    public static enum Unit
    {
        Count( "count" ),
        CountSecond( "count/sec" ),
        Bytes( "bytes" ),
        QueriesSecond( "queries/sec" ),
        Rate( "value/sec" ),
        BytesSecond( "bytes/sec" ),
        Percent( "percent" );

        final private String m_name;

        private Unit(final String name)
        {
            m_name = name;
        }

        public String getName()
        {
            return m_name;
        }
    }
}
