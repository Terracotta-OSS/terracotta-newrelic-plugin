package com.newrelic.plugins.terracotta;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.jmxclient.TCL2JMXClient;
import org.terracotta.utils.jmxclient.beans.L2ProcessInfo;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition.ReturnBundleType;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition.ReturnValueType;
import com.newrelic.plugins.terracotta.metrics.data.AbstractMetricData;
import com.newrelic.plugins.terracotta.utils.MetricsBufferingWorker;
import com.newrelic.plugins.terracotta.utils.MetricsFetcher;

public class TCL2Agent extends Agent {
	private static Logger log = LoggerFactory.getLogger(TCL2Agent.class);

	private String name = "Default";
	private final TCL2JMXClient jmxTCClient;
	private final MetricsBufferingWorker metricsWorker;

	public TCL2Agent(String name, String jmxHost, int jmxPort, String jmxUsername, String jmxPassword, boolean nameDiscovery, boolean trackUniqueCaches, boolean trackUniqueClients, long intervalInMillis) throws ConfigurationException {
		super("org.terracotta.Terracotta", "1.0.5_SNAPSHOT");

		log.info(String.format("Connecting to JMX Server [%s:%d] with user=%s", jmxHost, jmxPort, jmxUsername));
		this.jmxTCClient = new TCL2JMXClient(jmxUsername, jmxPassword, jmxHost, jmxPort);

		L2ProcessInfo l2ProcessInfo = null;
		if(nameDiscovery && null != jmxTCClient && null != (l2ProcessInfo = jmxTCClient.getL2ProcessInfo())){
			this.name = l2ProcessInfo.getServerInfoSummary();
		} else {
			this.name = name;
		}

		this.metricsWorker = new MetricsBufferingWorker(intervalInMillis, new MetricsFetcher(this.jmxTCClient, trackUniqueCaches, trackUniqueClients));
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
		AbstractMetric[] metrics = metricsWorker.getAndCleanMetrics();
		if(null != metrics){
			for(AbstractMetric metric : metrics){
				try {
					if(null != metric){
						if(log.isDebugEnabled())
							log.debug(String.format("New Relic Agent[%s] - Reporting metric: %s", getComponentHumanLabel(), metric.toString()));

						if(metric.getDataPointsCount() == 0)
							log.warn("A metric with 0 data points should not be available here...");

						Map<MetricResultDefinition.ReturnValueType, Number> metricResult = metric.getMetricDataResults();
						
						if(metric.getResultDefinition().getReturnBundleType() == ReturnBundleType.DETAILED){
							reportMetric(metric.getName(), 
									metric.getUnit().getName(), 
									new Long(metric.getDataPointsCount()).intValue(), 
									metricResult.get(ReturnValueType.SUM), 
									metricResult.get(ReturnValueType.MIN), 
									metricResult.get(ReturnValueType.MAX), 
									metricResult.get(ReturnValueType.SUMSQ));
							
						} else if(metric.getResultDefinition().getReturnBundleType() == ReturnBundleType.CUSTOM){
							reportMetric(metric.getName(), metric.getUnit().getName(), metricResult.get(metric.getResultDefinition().getReturnValueTypes()[0]));
						}
						
//						if(metric.isBroadcastPercentiles()){ //add useful percentiles
//							log.debug(String.format("New Relic Agent[%s] - Reporting Percentiles for metric: %s", getComponentHumanLabel(), metric.toString()));
//							reportMetric(metric.getName() + "/50th", metric.getUnit().getName(), metric.getPercentile(50)); //median
//							reportMetric(metric.getName() + "/95th", metric.getUnit().getName(), metric.getPercentile(95)); //95th
//						}
					} else {
						log.warn("Current metric is null");
					}
				} catch (Exception e) {
					log.error(String.format("New Relic Agent[%s] - Error with metrics reporting", metric.getNameWithUnit()), e);
				}
			}
		} else {
			log.warn(String.format("New Relic Agent[%s] - The metrics array is null...something must be wrong.", getComponentHumanLabel()));
		}
	}
}