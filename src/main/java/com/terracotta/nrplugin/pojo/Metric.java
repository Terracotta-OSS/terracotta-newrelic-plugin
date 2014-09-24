package com.terracotta.nrplugin.pojo;

import com.terracotta.nrplugin.util.MetricUtil;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/24/14
 * Time: 8:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class Metric implements Serializable {

	private static final long serialVersionUID = -1055398640926238446L;

	String metricName;
	String reportedPath;
	String dataPath;
	Source source;
	Unit unit;
	Type type;
	RatioType ratioType;
	int maxWindowSize = MetricDataset.WINDOW_SIZE_DEFAULT;

	public Metric() {
	}



    public String getMetricName() {
        return metricName;
    }

//    public String getName() {
//        String[] split = reportedPath.split(MetricUtil.NEW_RELIC_PATH_SEPARATOR);
//		return split.length > 0 ? split[split.length - 1] : null;
//	}

	public String getBaseReportedPath() {
//        List<String> split = Arrays.asList(reportedPath.split(MetricUtil.NEW_RELIC_PATH_SEPARATOR));
		String[] split = reportedPath.split(MetricBuilder.NEW_RELIC_PATH_SEPARATOR);
		String[] spliced = Arrays.copyOf(split, split.length - 1);
		return StringUtils.join(spliced, MetricBuilder.NEW_RELIC_PATH_SEPARATOR);
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

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public int getMaxWindowSize() {
		return maxWindowSize;
	}

	public void setMaxWindowSize(int maxWindowSize) {
		this.maxWindowSize = maxWindowSize;
	}

	public RatioType getRatioType() {
		if (ratioType == null) ratioType = RatioType.neither;
		return ratioType;
	}

	public void setRatioType(RatioType ratioType) {
		this.ratioType = ratioType;
	}
		public static enum Type {regular, special, ratio}

	public static enum RatioType {hit, miss, neither}

	public static enum Source {server, client, cache, topologies}

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
                "metricName='" + metricName + '\'' +
                ", reportingPath='" + reportedPath + '\'' +
                ", dataPath='" + dataPath + '\'' +
                ", source=" + source +
                ", unit=" + unit +
                ", type=" + type +
                ", ratioType=" + ratioType +
                ", maxWindowSize=" + maxWindowSize +
                '}';
    }
}
