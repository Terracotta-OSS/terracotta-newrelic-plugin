package com.newrelic.plugins.terracotta;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.jmxclient.TCL2JMXClient;
import org.terracotta.utils.jmxclient.beans.L2ProcessInfo;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.newrelic.plugins.terracotta.utils.Metric;
import com.newrelic.plugins.terracotta.utils.MetricsBufferingWorker;
import com.newrelic.plugins.terracotta.utils.MetricsFetcher;

public class TCL2Agent extends Agent {
	private static Logger log = LoggerFactory.getLogger(TCL2Agent.class);

	private String name = "Default";
	private final TCL2JMXClient jmxTCClient;
	private final MetricsBufferingWorker metricsWorker;

	public TCL2Agent(String name, String jmxHost, int jmxPort, String jmxUsername, String jmxPassword, boolean nameDiscovery, boolean trackUniqueClients, long intervalInMillis) throws ConfigurationException {
		super("org.terracotta.Terracotta", "1.0.2");

		log.info(String.format("Connecting to JMX Server [%s:%d] with user=%s", jmxHost, jmxPort, jmxUsername));
		this.jmxTCClient = new TCL2JMXClient(jmxUsername, jmxPassword, jmxHost, jmxPort);

		L2ProcessInfo l2ProcessInfo = null;
		if(nameDiscovery && null != jmxTCClient && null != (l2ProcessInfo = jmxTCClient.getL2ProcessInfo())){
			this.name = l2ProcessInfo.getServerInfoSummary();
		} else {
			this.name = name;
		}

		this.metricsWorker = new MetricsBufferingWorker(intervalInMillis, new MetricsFetcher(this.jmxTCClient, trackUniqueClients));
		metricsWorker.startAndMoveOn();
	}

	@Override
	//this method is called only at agent setup...so if the name changes when agents are started, won't be taken...
	public String getComponentHumanLabel() {
		return this.name;
	}

	@Override
	public void pollCycle() {
		log.info(String.format("New Relic Agent[%s] - Pushing L2 Metrics to NewRelic Cloud", getComponentHumanLabel()));

		//get all metrics and report to new relic
		List<Metric> metrics = metricsWorker.getMetricsSnapshot();
		if(null == metrics || metrics.size() == 0){
			log.warn(String.format("New Relic Agent[%s] - Buffered metrics are null! The background thread might have been terminated and agent shoudl be restarted.", getComponentHumanLabel()));
			log.warn(String.format("New Relic Agent[%s] - Meanwhile, until agent is restarted, non-buffered metrics will be fetched directly", getComponentHumanLabel()));

			try{
				metrics = metricsWorker.getMetricsFetcher().getMetricsFromServer();
			} catch (ConfigurationException cex){
				log.error("The JMX connection could not be established...moving on...", cex);
			} catch (Exception exc){
				log.error("Unexpected error while getting metrics from the server...moving on...", exc);
			}
		}

		if(null != metrics){
			for(Metric metric : metrics){
				try {
					if(null != metric){
						if(log.isDebugEnabled())
							log.debug("Reporting metric: " + metric.toString());

						
						if(metric.getName().equals(MetricsFetcher.SERVER_STATE)){ //special case for SERVER_STATE
							//returning max...that way if there was something happening in between the polling cycle, we would catch it
							reportMetric(metric.getName(), metric.getUnit().getName(), metric.getMax());
						} else {
							reportMetric(metric.getName(), metric.getUnit().getName(), metric.getDataPointsCount(), metric.getAggregateValue(), metric.getMin(), metric.getMax(), metric.getAggregateSumOfSquares());
						}
					} else {
						log.warn("Current metric is null");
					}
				} catch (Exception e) {
					log.error(String.format("New Relic Agent[%s] - Error with metrics reporting", metric.getMetricFullName()), e);
				}
			}
		}
	}
}