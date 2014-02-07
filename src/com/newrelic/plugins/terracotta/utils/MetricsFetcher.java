package com.newrelic.plugins.terracotta.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.jmxclient.TCL2JMXClient;
import org.terracotta.utils.jmxclient.beans.CacheManagerInfo;
import org.terracotta.utils.jmxclient.beans.CacheStats;
import org.terracotta.utils.jmxclient.beans.L2ClientID;
import org.terracotta.utils.jmxclient.beans.L2ClientRuntimeInfo;
import org.terracotta.utils.jmxclient.beans.L2DataStats;
import org.terracotta.utils.jmxclient.beans.L2ProcessInfo;
import org.terracotta.utils.jmxclient.beans.L2RuntimeStatus;
import org.terracotta.utils.jmxclient.beans.L2TransactionsStats;
import org.terracotta.utils.jmxclient.utils.L2RuntimeState;

import com.newrelic.plugins.terracotta.metrics.AbstractMetric.AggregationType;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition;
import com.newrelic.plugins.terracotta.metrics.AbstractMetric.MetricResultDefinition.ReturnValueType;
import com.newrelic.plugins.terracotta.metrics.ClientMetric;
import com.newrelic.plugins.terracotta.metrics.EhcacheClientMetric;
import com.newrelic.plugins.terracotta.metrics.ServerMetric;
import com.newrelic.plugins.terracotta.utils.MetricsBufferingWorker.MetricsBuffer;

public class MetricsFetcher {
	private static Logger log = LoggerFactory.getLogger(MetricsFetcher.class);

	private final TCL2JMXClient jmxTCClient;
	public final boolean trackUniqueCaches;
	public final boolean trackUniqueClients;

	public final boolean learningMode = PluginConfig.getInstance().getPropertyAsBoolean("com.newrelic.plugins.terracotta.learningmode", false);
	public final boolean disableEhcacheStats = PluginConfig.getInstance().getPropertyAsBoolean("com.newrelic.plugins.terracotta.ehcache.statistics.disable", false);
	public final boolean enableEhcacheStats = PluginConfig.getInstance().getPropertyAsBoolean("com.newrelic.plugins.terracotta.ehcache.statistics.enable", false);
	public final String[] ehcacheStatsFilterCaches;
	public final String[] ehcacheStatsFilterClients;

	public MetricsFetcher(TCL2JMXClient jmxTCClient, boolean trackUniqueCaches, boolean trackUniqueClients) {
		super();
		this.jmxTCClient = jmxTCClient;
		this.trackUniqueCaches = trackUniqueCaches;
		this.trackUniqueClients = trackUniqueClients;

		String filterCache = PluginConfig.getInstance().getProperty("com.newrelic.plugins.terracotta.ehcache.statistics.enable.filter_caches");
		if(null != filterCache && !"".equals(filterCache.trim())){
			this.ehcacheStatsFilterCaches = filterCache.trim().toLowerCase().split(",");
			Arrays.sort(this.ehcacheStatsFilterCaches);
		} else {
			this.ehcacheStatsFilterCaches = null;
		}

		String filterClients = PluginConfig.getInstance().getProperty("com.newrelic.plugins.terracotta.ehcache.statistics.enable.filter_clients");
		if(null != filterClients && !"".equals(filterClients.trim())){
			this.ehcacheStatsFilterClients = filterClients.trim().toLowerCase().split(",");
			Arrays.sort(this.ehcacheStatsFilterClients);
		} else {
			this.ehcacheStatsFilterClients = null;
		}
	}

	public TCL2JMXClient getJmxTCClient() {
		return jmxTCClient;
	}

	//this method is called when buffer is closed and emptied...useful to perform last cleanup before delivery to agent
	/*	public NewRelicDeliveryBundle[] packageMetricsForDelivery(AbstractMetric[] globalMetrics) {
		List<NewRelicDeliveryBundle> packagedMetrics = new ArrayList<NewRelicDeliveryBundle>();

		//these buffers are for the aggregate values
		MetricsBuffer clientsStatsAggregateBuffer = new MetricsBuffer();
		MetricsBuffer ehcacheStatsAggregateBuffer = new MetricsBuffer();

		if(null != globalMetrics){
			for(AbstractMetric metric : globalMetrics){
				if(log.isDebugEnabled())
					log.debug(String.format("Packaging metric: %s", metric.toString()));

				if(metric.getMetricData() == null || metric.getMetricData().getValuesCount() == 0){
					log.warn("A metric with no data or 0 data points should not be available here...doing nothing");
					continue;
				}

				if(metric.getNameWithUnit().equals(new ServerMetric("State", MetricUnit.Count).getNameWithUnit())){ //special case for SERVER_STATE
					//returning max...that way if there was something happening in between the polling cycle, we would catch it
					packagedMetrics.add(new NewRelicDeliveryBundle(metric.getName(), metric.getUnit(), NewRelicDeliveryType.SIMPLE).addBundleValue(NewRelicValuesType.ABSOLUTE, metric.getMetricData().getMax()));
				} else if(metric instanceof ClientMetric) {
					ClientMetric clientMetric = (ClientMetric) metric;

					//aggregate all clients averages by default (and use DetailedMetric to possibly calculate percentiles for the averages)
					clientsStatsAggregateBuffer.addSummaryMetric(new ClientMetric(clientMetric.getShortName(), clientMetric.getUnit(), null), clientMetric.getMetricData().getAverage());

					if(trackUniqueClients){
						NewRelicDeliveryBundle bundle = new NewRelicDeliveryBundle(clientMetric.getName(), clientMetric.getUnit(), NewRelicDeliveryType.DETAILED)
						.addBundleValue(NewRelicValuesType.DATAPPOINTCOUNT, clientMetric.getMetricData().getValuesCount())
						.addBundleValue(NewRelicValuesType.SUM, clientMetric.getMetricData().getSum())
						.addBundleValue(NewRelicValuesType.SUMSQ, clientMetric.getMetricData().getSumOfSquares())
						.addBundleValue(NewRelicValuesType.MIN, clientMetric.getMetricData().getMin())
						.addBundleValue(NewRelicValuesType.MAX, clientMetric.getMetricData().getMax())
						.addBundleValue(NewRelicValuesType.MEAN, clientMetric.getMetricData().getAverage());

						if (clientMetric.getMetricData() instanceof ExtentedMetricData) {
							ExtentedMetricData metricData = (ExtentedMetricData) clientMetric.getMetricData();

							bundle.addBundleValue(NewRelicValuesType.MEDIAN,metricData.getPercentile(50));
							bundle.addBundleValue(NewRelicValuesType.P95, metricData.getPercentile(95));
						}

						packagedMetrics.add(bundle);
					}
				} else if(metric instanceof EhcacheClientMetric) {
					EhcacheClientMetric ehcacheMetric = (EhcacheClientMetric) metric;

					//calculate the sum of all these stats
					if(ehcacheMetric.getMetricData() instanceof DifferentialMetricData){
						ehcacheStatsAggregateBuffer.addSummaryMetric(new EhcacheClientMetric(ehcacheMetric.getShortName(), ehcacheMetric.getUnit(), null, ehcacheMetric.getCacheManagerName(), ehcacheMetric.getCacheName()), metric.getMetricData().getSum());
					} else {
						//aggregate all clients averages by default (and use DetailedMetric to possibly calculate percentiles for the averages)
						ehcacheStatsAggregateBuffer.addSummaryMetric(new EhcacheClientMetric(ehcacheMetric.getShortName(), ehcacheMetric.getUnit(), null, ehcacheMetric.getCacheManagerName(), ehcacheMetric.getCacheName()), metric.getMetricData().getAverage());
					}

					if(trackUniqueClients){
						NewRelicDeliveryBundle bundle = new NewRelicDeliveryBundle(ehcacheMetric.getName(), ehcacheMetric.getUnit(), NewRelicDeliveryType.DETAILED)
						.addBundleValue(NewRelicValuesType.DATAPPOINTCOUNT, ehcacheMetric.getMetricData().getValuesCount())
						.addBundleValue(NewRelicValuesType.SUM, ehcacheMetric.getMetricData().getSum())
						.addBundleValue(NewRelicValuesType.SUMSQ, ehcacheMetric.getMetricData().getSumOfSquares())
						.addBundleValue(NewRelicValuesType.MIN, ehcacheMetric.getMetricData().getMin())
						.addBundleValue(NewRelicValuesType.MAX, ehcacheMetric.getMetricData().getMax())
						.addBundleValue(NewRelicValuesType.MEAN, ehcacheMetric.getMetricData().getAverage());

						if (ehcacheMetric.getMetricData() instanceof ExtentedMetricData) {
							ExtentedMetricData metricData = (ExtentedMetricData) ehcacheMetric.getMetricData();

							bundle.addBundleValue(NewRelicValuesType.MEDIAN,metricData.getPercentile(50));
							bundle.addBundleValue(NewRelicValuesType.P95, metricData.getPercentile(95));
						}

						packagedMetrics.add(bundle);
					}
				} else {
					NewRelicDeliveryBundle bundle = new NewRelicDeliveryBundle(metric.getName(), metric.getUnit(), NewRelicDeliveryType.DETAILED)
					.addBundleValue(NewRelicValuesType.DATAPPOINTCOUNT, metric.getMetricData().getValuesCount())
					.addBundleValue(NewRelicValuesType.SUM, metric.getMetricData().getSum())
					.addBundleValue(NewRelicValuesType.SUMSQ, metric.getMetricData().getSumOfSquares())
					.addBundleValue(NewRelicValuesType.MIN, metric.getMetricData().getMin())
					.addBundleValue(NewRelicValuesType.MAX, metric.getMetricData().getMax())
					.addBundleValue(NewRelicValuesType.MEAN, metric.getMetricData().getAverage());

					if (metric.getMetricData() instanceof ExtentedMetricData) {
						ExtentedMetricData metricData = (ExtentedMetricData) metric.getMetricData();

						bundle.addBundleValue(NewRelicValuesType.MEDIAN,metricData.getPercentile(50));
						bundle.addBundleValue(NewRelicValuesType.P95, metricData.getPercentile(95));
					}

					packagedMetrics.add(bundle);
				}
			}

			//after the looping is done...let's package the temp buffers
			AbstractMetric[] aggregatedMetrics = clientsStatsAggregateBuffer.getAllMetricsAndReset();
			if(null != aggregatedMetrics){
				for(AbstractMetric metric : aggregatedMetrics){
					NewRelicDeliveryBundle bundle = new NewRelicDeliveryBundle(metric.getName(), metric.getUnit(), NewRelicDeliveryType.DETAILED)
					.addBundleValue(NewRelicValuesType.DATAPPOINTCOUNT, metric.getMetricData().getValuesCount())
					.addBundleValue(NewRelicValuesType.SUM, metric.getMetricData().getSum())
					.addBundleValue(NewRelicValuesType.SUMSQ, metric.getMetricData().getSumOfSquares())
					.addBundleValue(NewRelicValuesType.MIN, metric.getMetricData().getMin())
					.addBundleValue(NewRelicValuesType.MAX, metric.getMetricData().getMax())
					.addBundleValue(NewRelicValuesType.MEAN, metric.getMetricData().getAverage());

					if (metric.getMetricData() instanceof ExtentedMetricData) {
						ExtentedMetricData metricData = (ExtentedMetricData) metric.getMetricData();

						bundle.addBundleValue(NewRelicValuesType.MEDIAN,metricData.getPercentile(50));
						bundle.addBundleValue(NewRelicValuesType.P95, metricData.getPercentile(95));
					}

					packagedMetrics.add(bundle);
				}
			}

			//find out of these metrics the counts useful for global ratio
			aggregatedMetrics = ehcacheStatsAggregateBuffer.getAllMetricsAndReset();
			if(null != aggregatedMetrics){
				for(AbstractMetric metric : aggregatedMetrics){
					if(metric.getShortName().equals(String.format("%s/%s", "Hits", "Total"))){

					} else if(metric.getShortName().equals(String.format("%s/%s", "Hits", "LocalHeap"))){

					} else if(metric.getShortName().equals(String.format("%s/%s", "Hits", "LocalOffheap"))){

					} else if(metric.getShortName().equals(String.format("%s/%s", "Hits", "LocalDiskOrRemote"))){

					} else if(metric.getShortName().equals(String.format("%s/%s", "Misses", "Total"))){

					} else if(metric.getShortName().equals(String.format("%s/%s", "Misses", "LocalHeap"))){

					} else if(metric.getShortName().equals(String.format("%s/%s", "Misses", "LocalOffheap"))){

					} else if(metric.getShortName().equals(String.format("%s/%s", "Misses", "LocalDiskOrRemote"))){

					}


					NewRelicDeliveryBundle bundle = new NewRelicDeliveryBundle(metric.getName(), metric.getUnit(), NewRelicDeliveryType.DETAILED)
					.addBundleValue(NewRelicValuesType.DATAPPOINTCOUNT, metric.getMetricData().getValuesCount())
					.addBundleValue(NewRelicValuesType.SUM, metric.getMetricData().getSum())
					.addBundleValue(NewRelicValuesType.SUMSQ, metric.getMetricData().getSumOfSquares())
					.addBundleValue(NewRelicValuesType.MIN, metric.getMetricData().getMin())
					.addBundleValue(NewRelicValuesType.MAX, metric.getMetricData().getMax())
					.addBundleValue(NewRelicValuesType.MEAN, metric.getMetricData().getAverage());

					if (metric.getMetricData() instanceof ExtentedMetricData) {
						ExtentedMetricData metricData = (ExtentedMetricData) metric.getMetricData();

						bundle.addBundleValue(NewRelicValuesType.MEDIAN,metricData.getPercentile(50));
						bundle.addBundleValue(NewRelicValuesType.P95, metricData.getPercentile(95));
					}

					packagedMetrics.add(bundle);
				}
			}
		}
	}
	 */
	//method called by internal scheduled thread
	//should always return something...if JMX is not accessible, should return empty metrics
	public void addMetrics(MetricsBuffer metricsbuf) {
		if(null == metricsbuf)
			throw new IllegalArgumentException("The buffer may not be null");

		//check in that method if JMX is alive and well...
		if(null == jmxTCClient || !jmxTCClient.initialize()){
			log.error("JMX connection could not be initialized...sending null metrics");

			//send error state
			addServerState(metricsbuf, L2RuntimeState.ERROR.getStateIntValue());

			//send null values for transactions stats
			addL2RuntimeMetrics(metricsbuf, null);

			//send null values for used space
			addL2DataMetrics(metricsbuf, null);

			if(learningMode){
				//send null values for clients
				addClientCountConnected(metricsbuf, null);

				addClientRuntimeMetrics(metricsbuf, null, null); //for learning if the "All" format
				addClientRuntimeMetrics(metricsbuf, "*", null); //for learning if the "id/<text>" format

				addEhcacheAllMetrics(metricsbuf, null, null, null, null); //for learning if the "All" format
				addEhcacheAllMetrics(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format

				addEhcacheClientCountStatsEnabled(metricsbuf, null, null, null, null); //for learning if the "All" format
				addEhcacheClientCountStatsEnabled(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format
			}
		} else {
			L2ProcessInfo l2ProcessInfo = jmxTCClient.getL2ProcessInfo();
			L2TransactionsStats txStats = jmxTCClient.getL2TransactionsStats();
			L2DataStats dataStats = jmxTCClient.getL2DataStats();
			L2RuntimeStatus statusInfo = jmxTCClient.getL2RuntimeStatus();

			log.info(String.format("Getting L2 Metrics From Server %s", l2ProcessInfo.getServerInfoSummary()));

			//send state
			addServerState(metricsbuf, statusInfo.getState().getStateIntValue());

			//Adding Server transactions stats
			addL2RuntimeMetrics(metricsbuf, txStats);

			//Adding Server used memory/disk space
			addL2DataMetrics(metricsbuf, dataStats);

			//Adding client metrics
			if(jmxTCClient.isNodeActive()){
				log.debug(String.format("Node %s is Active...fetching registered client metrics", l2ProcessInfo.getServerInfoSummary()));

				//list that contains the clientIDs that have ehcache Mbeans tunnelled and registered
				List<L2ClientID> clientsWithTunneledBeansRegistered = null;

				//get client Details now
				L2ClientRuntimeInfo[] runtimeStatsClientsArray = jmxTCClient.getClients();
				if(null != runtimeStatsClientsArray && runtimeStatsClientsArray.length > 0){
					addClientCountConnected(metricsbuf, runtimeStatsClientsArray.length);
					for(L2ClientRuntimeInfo clientRuntimeInfo : runtimeStatsClientsArray){

						//add all the metrics for each client (important: no wildcard here - we will do aggregation later if needed)
						String clientId = clientRuntimeInfo.getClientID().getRemoteAddress();

						//track unique clients
						addClientRuntimeMetrics(metricsbuf, clientId, clientRuntimeInfo);

						//average metrics for all clients
						addClientRuntimeMetrics(metricsbuf, null, clientRuntimeInfo);

						if(clientRuntimeInfo.getClientID().isTunneledBeansRegistered()){
							if(null == clientsWithTunneledBeansRegistered)
								clientsWithTunneledBeansRegistered = new ArrayList<L2ClientID>();

							clientsWithTunneledBeansRegistered.add(clientRuntimeInfo.getClientID());
						}
					}
				} else {
					log.debug(String.format("Node %s does not have any registered clients...sending null client metrics", l2ProcessInfo.getServerInfoSummary()));

					addClientCountConnected(metricsbuf, null);
					addClientRuntimeMetrics(metricsbuf, null, null); //for learning if the "All" format
					addClientRuntimeMetrics(metricsbuf, "*", null); //for learning if the "id/<text>" format
				}

				//if it's not null, means that there are indeed some tunneled ehcache mbeans here...let's jump into ehcache stats then!!
				if(null != clientsWithTunneledBeansRegistered){
					log.debug(String.format("Node %s has %d clients registered with ehcache mbeans", l2ProcessInfo.getServerInfoSummary(), clientsWithTunneledBeansRegistered.size()));

					// Loop over CacheManagers
					Map<String, CacheManagerInfo> cacheManagerInfo = jmxTCClient.getCacheManagerInfo();
					Iterator<Entry<String, CacheManagerInfo>> iter = cacheManagerInfo.entrySet().iterator();
					while (iter.hasNext()) {
						Entry<String, CacheManagerInfo> cmInfoElem = iter.next();
						CacheManagerInfo cmInfo = cmInfoElem.getValue();

						for(String cacheName : cmInfo.getCaches()){
							if(log.isDebugEnabled())
								log.debug(String.format("Node %s does not have any ehcache mbeans...", l2ProcessInfo.getServerInfoSummary()));

							int cacheStatsClientEnabledCount = 0;
							for(String clientId : cmInfo.getClientMbeansIDs()){
								if(log.isDebugEnabled())
									log.debug(String.format("Node %s - Getting stats for cache=[%s] and client=[%s]", l2ProcessInfo.getServerInfoSummary(), cacheName, clientId));

								//enable statistics if specified
								CacheStats cacheStats = null;
								if(enableEhcacheStats || disableEhcacheStats){
									// if arrays are null, stats should be modified for all
									//strings that are in the arrays are lowercase, so let's make sure the searched string are also lower case
									if((ehcacheStatsFilterCaches == null || (ehcacheStatsFilterCaches != null && Arrays.binarySearch(ehcacheStatsFilterCaches, cacheName.toLowerCase()) >=0)) &&
											(ehcacheStatsFilterClients == null || (ehcacheStatsFilterClients != null && Arrays.binarySearch(ehcacheStatsFilterClients, clientId.toLowerCase()) >=0)))
									{
										//let's have disableStats win in case both are true
										if(disableEhcacheStats){
											cacheStats = jmxTCClient.getCacheStats(cmInfo.getCmName(), cacheName, clientId);
										} else if(enableEhcacheStats){
											cacheStats = jmxTCClient.enableStatisticsAndGetCacheStats(cmInfo.getCmName(), cacheName, clientId);
										}
									} else {
										cacheStats = jmxTCClient.getCacheStats(cmInfo.getCmName(), cacheName, clientId);
									}
								} else {
									cacheStats = jmxTCClient.getCacheStats(cmInfo.getCmName(), cacheName, clientId);
								}

								if(null != cacheStats){
									if(cacheStats.isEnabled() && cacheStats.isStatsEnabled()){
										cacheStatsClientEnabledCount++;

										//add all the metrics for each cache/client (important: no wildcard here - we will do aggregation later if needed)
										addEhcacheAllMetrics(metricsbuf, clientId, cmInfo.getCmName(), cacheName, cacheStats);

										//average metrics for all clients
										addEhcacheAllMetrics(metricsbuf, null, cmInfo.getCmName(), cacheName, cacheStats);
									}
								} else {
									log.error(String.format("Could not get cache stats for %s-%s-%s", cmInfo.getCmName(), cacheName, clientId));
								}
							}

							addEhcacheClientCountStatsEnabled(metricsbuf, null, cmInfo.getCmName(), cacheName, cacheStatsClientEnabledCount);
						}
					}
				} else {
					if(log.isDebugEnabled())
						log.debug(String.format("Node %s does not have any ehcache mbeans...", l2ProcessInfo.getServerInfoSummary()));

					if(learningMode){
						log.debug(String.format("Sending null ehcache client metrics"));

						//this node has not ehcache mbeans...so send null values...
						addEhcacheAllMetrics(metricsbuf, null, null, null, null); //for learning if the "All" format
						addEhcacheAllMetrics(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format

						addEhcacheClientCountStatsEnabled(metricsbuf, null, null, null, null); //for learning if the "All" format
						addEhcacheClientCountStatsEnabled(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format
					}
				}
			} else {
				log.debug(String.format("Node %s is not active...no client info available.", l2ProcessInfo.getServerInfoSummary()));
				if(learningMode){
					log.debug(String.format("Sending null client metrics"));

					//this node is not active...it does not have any client stats...so send null values...
					addClientCountConnected(metricsbuf, null);

					addClientRuntimeMetrics(metricsbuf, null, null); //for learning if the "All" format
					addClientRuntimeMetrics(metricsbuf, "*", null); //for learning if the "id/<text>" format

					addEhcacheAllMetrics(metricsbuf, null, null, null, null); //for learning if the "All" format
					addEhcacheAllMetrics(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format

					addEhcacheClientCountStatsEnabled(metricsbuf, null, null, null, null); //for learning if the "All" format
					addEhcacheClientCountStatsEnabled(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format
				}
			}
		}
	}

	private void addServerState(MetricsBuffer metrics, int state){
		log.info("begin addServerState");

		metrics.addMetric(new ServerMetric("State", MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createCustomMax()), state);
	}

	private void addClientCountConnected(MetricsBuffer metrics, Integer connectedCount){
		log.info("begin addClientCountConnected");

		metrics.addMetric(new ServerMetric(String.format("%s", "ConnectedClients"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (connectedCount == null)?0:connectedCount);
	}

	private void addClientRuntimeMetrics(MetricsBuffer metrics, String clientID, L2ClientRuntimeInfo clientRuntimeInfo){
		log.info("begin addClientRuntimeMetrics");

		metrics.addMetric(new ClientMetric(String.format("%s/%s", "Transactions", "Total"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID), (null == clientRuntimeInfo)?0:clientRuntimeInfo.getTransactionRate());
		metrics.addMetric(new ClientMetric(String.format("%s/%s", "Transactions", "Faults"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID), (null == clientRuntimeInfo)?0:clientRuntimeInfo.getObjectFaultRate());
		metrics.addMetric(new ClientMetric(String.format("%s/%s", "Transactions", "Flushes"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID), (null == clientRuntimeInfo)?0:clientRuntimeInfo.getObjectFlushRate());
		metrics.addMetric(new ClientMetric(String.format("%s/%s", "Transactions", "Pending"), MetricUnit.Count, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID), (null == clientRuntimeInfo)?0:clientRuntimeInfo.getPendingTransactionsCount());
	}

	private void addEhcacheAllMetrics(MetricsBuffer metrics, String clientID, String cacheManagerName, String cacheName, CacheStats cacheStats){
		addEhcacheCountsMetrics(metrics, clientID, cacheManagerName, cacheName, cacheStats);
		addEhcacheSizeMetrics(metrics, clientID, cacheManagerName, cacheName, cacheStats);
		addEhcacheRatesMetrics(metrics, clientID, cacheManagerName, cacheName, cacheStats);
	}

	private void addEhcacheClientCountStatsEnabled(MetricsBuffer metrics, String clientID, String cacheManagerName, String cacheName, Integer statsEnabledClientCount){
		log.info("begin addEhcacheClientCountStatsEnabled");

		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "StatsEnabled"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (statsEnabledClientCount == null)?0:statsEnabledClientCount);
	}

	private void addEhcacheCountsMetrics(MetricsBuffer metrics, String clientID, String cacheManagerName, String cacheName, CacheStats cacheStats){
		log.info("begin addEhcacheCountsMetrics");
		if(null == clientID || cacheManagerName == null || cacheName == null){
			log.info("This method does not support null values for clientID, cacheManagerName, or cacheName. Doing nothing.");
			return;
		}

		//add counts
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Puts"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getPutCount());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Removes"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getRemovedCount());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Evictions"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getEvictedCount());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Expirations"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getExpiredCount());

		//here we compute the counter differences between each measure point (i.e. for each client what was added since last check)
		EhcacheClientMetric hitsTotal = (EhcacheClientMetric)metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "Total"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getCacheHitCount());
		EhcacheClientMetric missTotal = (EhcacheClientMetric)metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "Total"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getCacheMissCount());
		EhcacheClientMetric hitsHeap = (EhcacheClientMetric)metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalHeap"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getInMemoryHitCount());
		EhcacheClientMetric missHeap = (EhcacheClientMetric)metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalHeap"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getInMemoryMissCount());
		EhcacheClientMetric hitsOffheap = (EhcacheClientMetric)metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalOffheap"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOffHeapHitCount());
		EhcacheClientMetric missOffheap =(EhcacheClientMetric)metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalOffheap"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOffHeapMissCount());
		EhcacheClientMetric hitsDisk = (EhcacheClientMetric)metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalDiskOrRemote"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOnDiskHitCount());
		EhcacheClientMetric missDisk = (EhcacheClientMetric)metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalDiskOrRemote"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOnDiskMissCount());

		//check for aggregation counts for all clients (clientID=null)
		addEhcacheCalculatedHitRatio(metrics, String.format("%s/%s", "HitRatio", "Total"), cacheManagerName, cacheName, hitsTotal.getMetricDataResults().get(ReturnValueType.SUM), missTotal.getMetricDataResults().get(ReturnValueType.SUM));
		addEhcacheCalculatedHitRatio(metrics, String.format("%s/%s", "HitRatio", "LocalHeap"), cacheManagerName, cacheName, hitsHeap.getMetricDataResults().get(ReturnValueType.SUM), missHeap.getMetricDataResults().get(ReturnValueType.SUM));
		addEhcacheCalculatedHitRatio(metrics, String.format("%s/%s", "HitRatio", "LocalOffheap"), cacheManagerName, cacheName, hitsOffheap.getMetricDataResults().get(ReturnValueType.SUM), missOffheap.getMetricDataResults().get(ReturnValueType.SUM));
		addEhcacheCalculatedHitRatio(metrics, String.format("%s/%s", "HitRatio", "LocalDiskOrRemote"), cacheManagerName, cacheName, hitsDisk.getMetricDataResults().get(ReturnValueType.SUM), missDisk.getMetricDataResults().get(ReturnValueType.SUM));
	}
	
	private void addEhcacheCalculatedHitRatio(MetricsBuffer metrics, String metricName, String cacheManagerName, String cacheName, Number hits, Number miss){
		if(null != hits && null != miss){
			double dHits=hits.doubleValue();
			double dMisses=miss.doubleValue();

			if(dHits != Double.NaN && dMisses != Double.NaN){
				if(dHits != 0.0D && dMisses != 0.0D){
					metrics.addMetric(new EhcacheClientMetric(metricName, MetricUnit.Percent, AggregationType.SUMMARY, MetricResultDefinition.createCustomAverage(), null, cacheManagerName, cacheName), hits.doubleValue() / (hits.doubleValue() + miss.doubleValue()));
				} else {
					log.info("hits/miss counts are 0...recording that.");
					metrics.addMetric(new EhcacheClientMetric(metricName, MetricUnit.Percent, AggregationType.SUMMARY, MetricResultDefinition.createCustomAverage(), null, cacheManagerName, cacheName), 0.0D);
				}
			} else{
				log.info("Could not add the ratio metric because the hits/miss were NaN");
			}
		} else {
			log.info("Could not add the ratio metric because the hits/miss were null");
		}
	}

	private void addEhcacheSizeMetrics(MetricsBuffer metrics, String clientID, String cacheManagerName, String cacheName, CacheStats cacheStats){
		log.info("begin addEhcacheSizeMetrics");

		//add sizes		
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Size", "Total"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getCacheSize());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Size", "LocalHeap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getLocalHeapSize());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Size", "LocalOffheap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getLocalOffHeapSize());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Size", "LocalDiskOrRemote"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getLocalDiskSize());
	}

	private void addEhcacheRatesMetrics(MetricsBuffer metrics, String clientID, String cacheManagerName, String cacheName, CacheStats cacheStats){
		log.info("begin addEhcacheRatesMetrics");

		//add this metric only if client is specified...otherwise does not make sense to average the hitratios
		if(null != clientID)
			metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "HitRatio", "Total"), MetricUnit.Percent, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getCacheHitRatio());

		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "Total"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getCacheHitRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalHeap"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOnHeapHitRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalOffheap"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOffHeapHitRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalDiskOrRemote"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOnDiskHitRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "Total"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getCacheMissRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalHeap"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOnHeapMissRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalOffheap"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOffHeapMissRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalDiskOrRemote"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getOnDiskMissRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Puts"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getCachePutRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Removes"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getRemoveRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Evictions"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getEvictionRate());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Expirations"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName), (null == cacheStats)?0:cacheStats.getExpirationRate());
	}

	private void addL2RuntimeMetrics(MetricsBuffer metrics, L2TransactionsStats txStats){
		log.info("begin addL2RuntimeMetrics");

		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Transactions", "Tiers", "Heap", "Faults"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed()), (null == txStats)?0:txStats.getOnHeapFaultRate());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Transactions", "Tiers", "Heap", "Flushes"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed()), (null == txStats)?0:txStats.getOnHeapFlushRate());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Transactions", "Tiers", "OffHeap", "Faults"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed()), (null == txStats)?0:txStats.getOffHeapFaultRate());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Transactions", "Tiers", "OffHeap", "Flushes"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed()), (null == txStats)?0:txStats.getOffHeapFlushRate());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Transactions", "Tiers", "Disk", "Faults"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed()), (null == txStats)?0:txStats.getL2DiskFaultRate());
		metrics.addMetric(new ServerMetric(String.format("%s/%s", "Transactions", "Total"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed()), (null == txStats)?0:txStats.getTransactionRate());
		metrics.addMetric(new ServerMetric(String.format("%s", "LockRecalls"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed()), (null == txStats)?0:txStats.getGlobalLockRecallRate());
	}

	private void addL2DataMetrics(MetricsBuffer metrics, L2DataStats dataStats){
		log.info("begin addL2DataMetrics");

		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s", "Data", "Objects", "Total"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getLiveObjectCount());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s", "Data", "Objects", "OffHeap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getOffheapObjectCachedCount());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s", "Data", "Objects", "Heap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getCachedObjectCount());

		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Data", "Tiers", "Heap", "UsedSize"), MetricUnit.Bytes, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getUsedHeap());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Data", "Tiers", "Heap", "MaxSize"), MetricUnit.Bytes, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getMaxHeap());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Data", "Tiers", "OffHeap", "KeyMapSize"), MetricUnit.Bytes, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getOffheapMapAllocatedMemory());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Data", "Tiers", "OffHeap", "MaxSize"), MetricUnit.Bytes, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getOffheapMaxDataSize());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Data", "Tiers", "OffHeap", "ObjectSize"), MetricUnit.Bytes, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getOffheapObjectAllocatedMemory());
		metrics.addMetric(new ServerMetric(String.format("%s/%s/%s/%s", "Data", "Tiers", "OffHeap", "TotalUsedSize"), MetricUnit.Bytes, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (null == dataStats)?0:dataStats.getOffheapTotalAllocatedSize());
	}
}
