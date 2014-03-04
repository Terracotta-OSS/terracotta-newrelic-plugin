package com.newrelic.plugins.terracotta;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
import com.newrelic.plugins.terracotta.utils.MetricsBufferingWorker;
import com.newrelic.plugins.terracotta.utils.MetricsFetcher;

public class TCL2Agent extends Agent {
	private static Logger log = LoggerFactory.getLogger(TCL2Agent.class);

	private String name = "Default";
	private final TCL2JMXClient jmxTCClient;
	private final MetricsBufferingWorker metricsWorker;

	public TCL2Agent(String name, String jmxHost, int jmxPort, String jmxUsername, String jmxPassword, boolean nameDiscovery, boolean trackUniqueCaches, boolean trackUniqueClients, long intervalInMillis) throws ConfigurationException {
		super("org.terracotta.Terracotta", "1.0.6");

		log.info(String.format("Connecting to JMX Server [%s:%d] with user=%s", jmxHost, jmxPort, jmxUsername));
		this.jmxTCClient = new TCL2JMXClient(jmxUsername, jmxPassword, jmxHost, jmxPort);

		L2ProcessInfo l2ProcessInfo = null;
		if(nameDiscovery && null != jmxTCClient && null != (l2ProcessInfo = jmxTCClient.getL2ProcessInfo())){
			this.name = l2ProcessInfo.getServerInfoSummary();
		} else {
			this.name = name;
		}

		this.metricsWorker = new MetricsBufferingWorker(name, new MetricsFetcher(this.jmxTCClient, trackUniqueCaches, trackUniqueClients), intervalInMillis);
		metricsWorker.startAndMoveOn();
	}

	@Override
	//this method is called only at agent setup...so if the name changes when agents are started, won't be taken...
	public String getComponentHumanLabel() {
		return this.name;
	}

	@Override
	public void pollCycle() {
		log.info(String.format("New Relic Agent[%s] - Pushing L2 Metrics to NewRelic Cloud at %s", getComponentHumanLabel(), new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())));

		//get all metrics and report to new relic
		AbstractMetric[] metrics = metricsWorker.getAndCleanMetrics();
		
		if(null != metrics){
			//sort array for ease of debugging
			if(log.isDebugEnabled())
				Arrays.sort(metrics);
			
			int reportedMetricCount = 0;
			for(AbstractMetric metric : metrics){
				try {
					if(null != metric){
						if(metric.isPublishEnabled()){
							long datapointCount = metric.getDataPointsCount();

							if(datapointCount == 0){
								if(log.isDebugEnabled())
									log.debug(String.format("New Relic Agent[%s] - Ignoring metric %s (0 Datapoint)", getComponentHumanLabel(), metric.getNameWithUnit()));
							} else {
								Map<MetricResultDefinition.ReturnValueType, Number> metricResult = metric.getMetricDataResults();

								if(metric.getResultDefinition().getReturnBundleType() == ReturnBundleType.DETAILED){
									if(log.isDebugEnabled())
										log.debug(String.format("New Relic Agent[%s] - Reporting DETAILED metric %s (%d datapoints) with [average=%f,min=%f,max=%f]", 
														getComponentHumanLabel(), 
														metric.getNameWithUnit(), 
														datapointCount, 
														metricResult.get(ReturnValueType.SUM).doubleValue() / datapointCount,
														metricResult.get(ReturnValueType.MIN).doubleValue(),
														metricResult.get(ReturnValueType.MAX).doubleValue()));

									reportMetric(metric.getName(), 
											metric.getUnit().getName(), 
											new Long(datapointCount).intValue(), 
											metricResult.get(ReturnValueType.SUM), 
											metricResult.get(ReturnValueType.MIN), 
											metricResult.get(ReturnValueType.MAX), 
											metricResult.get(ReturnValueType.SUMSQ));

									reportedMetricCount++;
								} else if(metric.getResultDefinition().getReturnBundleType() == ReturnBundleType.SINGLE){
									Number simpleResult = metricResult.get(metric.getResultDefinition().getReturnValueTypes()[0]);
									if(null != simpleResult){
										if(log.isDebugEnabled())
											log.debug(String.format("New Relic Agent[%s] - Reporting SIMPLE metric %s with value %s (%d datapoints)", getComponentHumanLabel(), metric.getNameWithUnit(), simpleResult.toString(), datapointCount));

										reportMetric(metric.getName(), metric.getUnit().getName(), simpleResult);
										reportedMetricCount++;
									}
								}

								//						if(metric.isBroadcastPercentiles()){ //add useful percentiles
								//							log.debug(String.format("New Relic Agent[%s] - Reporting Percentiles for metric: %s", getComponentHumanLabel(), metric.toString()));
								//							reportMetric(metric.getName() + "/50th", metric.getUnit().getName(), metric.getPercentile(50)); //median
								//							reportMetric(metric.getName() + "/95th", metric.getUnit().getName(), metric.getPercentile(95)); //95th
								//						}
							}
						} else {
							if(log.isDebugEnabled())
								log.debug(String.format("New Relic Agent[%s] - Publish not enabled for Metric: %s", getComponentHumanLabel(), metric.getNameWithUnit()));
						}
					} else {
						log.warn("Current metric is null");
					}
				} catch (Exception e) {
					log.error(String.format("New Relic Agent[%s] - Error with metrics reporting", metric.getNameWithUnit()), e);
				}
			}

			if(log.isDebugEnabled())
				log.debug(String.format("New Relic Agent[%s] - Total reported metric: %d", getComponentHumanLabel(), reportedMetricCount));
		} else {
			log.warn(String.format("New Relic Agent[%s] - The metrics array is null...something must be wrong.", getComponentHumanLabel()));
		}
	}
}