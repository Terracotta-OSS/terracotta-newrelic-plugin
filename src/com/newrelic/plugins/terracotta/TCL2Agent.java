package com.newrelic.plugins.terracotta;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.jmxclient.TCL2JMXClient;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.newrelic.plugins.terracotta.utils.Metric;
import com.newrelic.plugins.terracotta.utils.MetricsBufferingWorker;
import com.newrelic.plugins.terracotta.utils.MetricsFetcher;

public class TCL2Agent extends Agent {
	private static Logger log = LoggerFactory.getLogger(TCL2Agent.class);

	private String name = "Default";
	private final MetricsBufferingWorker metricsWorker;

	public TCL2Agent(String name, String jmxHost, int jmxPort, String jmxUsername, String jmxPassword) throws ConfigurationException {
		super("org.terracotta.Terracotta", "1.0.0");
		this.name = name;

		log.info(String.format("Connecting to JMX Server [%s:%d] with user=%s", jmxHost, jmxPort, jmxUsername));

		TCL2JMXClient jmxTCClient = new TCL2JMXClient(jmxUsername, jmxPassword, jmxHost, jmxPort);

		this.metricsWorker = new MetricsBufferingWorker(5000, new MetricsFetcher(jmxTCClient));
		metricsWorker.startAndMoveOn();
	}

	@Override
	public String getComponentHumanLabel() {
		if(null != metricsWorker.getMetricsFetcher().getL2ProcessInfo())
			return metricsWorker.getMetricsFetcher().getL2ProcessInfo().getServerInfoSummary();
		else
			return name;
	}

	@Override
	public void pollCycle() {
		log.info(String.format("New Relic Agent[%s] - Pushing L2 Metrics to NewRelic Cloud", getComponentHumanLabel()));

		//get all metrics and report to new relic
		List<Metric> metrics = metricsWorker.getMetricsSnapshot();
		if(null == metrics || metrics.size() == 0){
			log.warn(String.format("New Relic Agent[%s] - Buffered metrics are null! The background thread might have been terminated and agent shoudl be restarted.", getComponentHumanLabel()));
			log.warn(String.format("New Relic Agent[%s] - Meanwhile, until agent is restarted, non-buffered metrics will be fetched directly", getComponentHumanLabel()));
			metrics = metricsWorker.getMetricsFetcher().getMetricsFromServer();
		}

		if(null != metrics){
			for(Metric metric : metrics){
				try {
					if(log.isDebugEnabled())
						log.debug("Reporting metric: " + metric.toString());

					reportMetric(metric.getName(), metric.getUnit().getName(), metric.getDataPointsCount(), metric.getAggregateValue(), metric.getMin(), metric.getMax(), metric.getAggregateSumOfSquares());
				} catch (Exception e) {
					log.error(String.format("New Relic Agent[%s] - Error with metrics reporting", metric.getMetricFullName()), e);
				}
			}
		}
	}
}