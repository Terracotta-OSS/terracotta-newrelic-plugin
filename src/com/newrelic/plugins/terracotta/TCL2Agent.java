package com.newrelic.plugins.terracotta;

import java.util.List;

import org.terracotta.utils.jmxclient.TCL2JMXClient;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.newrelic.plugins.terracotta.utils.Metric;
import com.newrelic.plugins.terracotta.utils.MetricsBufferingWorker;
import com.newrelic.plugins.terracotta.utils.MetricsFetcher;

public class TCL2Agent extends Agent {
	private String name = "Default";
	private final MetricsBufferingWorker metricsWorker;

	public TCL2Agent(String name, String jmxHost, int jmxPort, String jmxUsername, String jmxPassword) throws ConfigurationException {
		super("org.terracotta.Terracotta", "1.0.2");
		this.name = name;

		System.out.println(String.format("Connecting to JMX Server [%s:%d] with user=%s", jmxHost, jmxPort, jmxUsername));
		
		TCL2JMXClient jmxTCClient = new TCL2JMXClient(jmxUsername, jmxPassword, jmxHost, jmxPort);

		this.metricsWorker = new MetricsBufferingWorker(5000, new MetricsFetcher(jmxTCClient));
		metricsWorker.startAndMoveOn();
	}

	@Override
	public String getComponentHumanLabel() {
		if(null != metricsWorker.getMetricsFetcher().getL2RuntimeInfo())
			return metricsWorker.getMetricsFetcher().getL2RuntimeInfo().getServerInfoSummary();
		else
			return name;
	}

	@Override
	public void pollCycle() {
		System.out.println(String.format("New Relic Agent[%s] - Pushing L2 Metrics to NewRelic Cloud", getComponentHumanLabel()));

		//get all metrics and report to new relic
		List<Metric> metrics = metricsWorker.getMetricsSnapshot();
		for(Metric metric : metrics){
			try {
				reportMetric(metric.getName(), metric.getUnit().getName(), metric.getDataPointsCount(), metric.getAggregateValue(), metric.getMin(), metric.getMax(), metric.getAggregateSumOfSquares());
			} catch (Exception e) {
				System.err.println(String.format("New Relic Agent[%s] - Error with metrics reporting: %s", metric.getMetricFullName(), e.getMessage()));
			}
		}
	}
}