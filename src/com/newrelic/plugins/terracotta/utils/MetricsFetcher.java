package com.newrelic.plugins.terracotta.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.newrelic.plugins.terracotta.metrics.AbstractMetric;
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
	public Pattern ehcacheStatsFilterCaches = null;
	public Pattern ehcacheStatsFilterClients = null;

	public static final String PATTERN_SEPARATOR = "~~";

	public MetricsFetcher(TCL2JMXClient jmxTCClient, boolean trackUniqueCaches, boolean trackUniqueClients) {
		super();
		this.jmxTCClient = jmxTCClient;
		this.trackUniqueCaches = trackUniqueCaches;
		this.trackUniqueClients = trackUniqueClients;

		String filterCache = null;
		try{
			filterCache = PluginConfig.getInstance().getProperty("com.newrelic.plugins.terracotta.ehcache.statistics.enable.filter_caches");
			if(null != filterCache && !"".equals(filterCache.trim())){
				this.ehcacheStatsFilterCaches = Pattern.compile(filterCache.trim(), Pattern.CASE_INSENSITIVE);
			}
		} catch (Exception exc){
			log.error(String.format("An error occurred while compiling the regex pattern %s defined in filter_caches property", filterCache.trim()), exc);
		}
		String filterClients = null;
		try{
			filterClients = PluginConfig.getInstance().getProperty("com.newrelic.plugins.terracotta.ehcache.statistics.enable.filter_clients");
			if(null != filterClients && !"".equals(filterClients.trim())){
				this.ehcacheStatsFilterClients = Pattern.compile(filterClients.trim(), Pattern.CASE_INSENSITIVE);
			}
		} catch (Exception exc){
			log.error(String.format("An error occurred while compiling the regex pattern %s defined in filter_clients property", filterClients.trim()), exc);
		}
	}

	public TCL2JMXClient getJmxTCClient() {
		return jmxTCClient;
	}

	//method called by internal scheduled thread

	public void addMetrics(MetricsBuffer metricsbuf) {
		addMetrics(metricsbuf, Long.MIN_VALUE);
	}

	public void addMetrics(MetricsBuffer metricsbuf, long timeSpentSinceLastCall) {
		long startTime = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("addMetrics start - " + startTime);
			log.debug(String.format("Time (millis) since last call: %s", (Long.MIN_VALUE != timeSpentSinceLastCall)?new Long(timeSpentSinceLastCall).toString():"null"));
		}

		if(null == metricsbuf)
			throw new IllegalArgumentException("The buffer may not be null");

		//this is only useful for the ehcache values in learning mode 
		Map<String, Double> learningModeAggregateCounts = new HashMap<String, Double>();
		if(learningMode){
			if(log.isDebugEnabled())
				log.debug(String.format("Learning mode: Creating a learningModeAggregateCounts map with 0 values"));
			simulateAggregateCount(learningModeAggregateCounts);
		}

		//check in that method if JMX is alive and well...
		if(null == jmxTCClient || !jmxTCClient.initialize()){
			log.error("JMX connection could not be initialized...sending null metrics");

			//send error state
			addServerState(metricsbuf, null);

			//send null values for transactions stats
			addL2RuntimeMetrics(metricsbuf, null);

			//send null values for used space
			addL2DataMetrics(metricsbuf, null);

			if(learningMode){
				if(log.isDebugEnabled())
					log.debug(String.format("Learning mode: Sending null ehcache client metrics"));

				//send null values for clients
				addClientCountConnected(metricsbuf, null);

				addClientRuntimeMetrics(metricsbuf, null, null); //for learning if the "All" format
				addClientRuntimeMetrics(metricsbuf, "*", null); //for learning if the "id/<text>" format

				addEhcacheAggregatesMetrics(metricsbuf, learningModeAggregateCounts, null, null, null, 1L); //for learning if the "All" format
				addEhcacheAggregatesMetrics(metricsbuf, learningModeAggregateCounts, "*", "*", "*", 1L); //for learning if the "id/<text>" format
				addEhcacheSizeMetrics(metricsbuf, null, null, null, null);
				addEhcacheSizeMetrics(metricsbuf, "*", "*", "*", null);
				addEhcacheClientCountStatsEnabled(metricsbuf, null, null, null, null); //for learning if the "All" format
				addEhcacheClientCountStatsEnabled(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format
			}
		} else {
			L2ProcessInfo l2ProcessInfo = jmxTCClient.getL2ProcessInfo();
			L2TransactionsStats txStats = jmxTCClient.getL2TransactionsStats();
			L2DataStats dataStats = jmxTCClient.getL2DataStats();
			L2RuntimeStatus statusInfo = jmxTCClient.getL2RuntimeStatus();

			log.info(String.format("Node %s - Getting L2 Metrics", (null != l2ProcessInfo)?l2ProcessInfo.getServerInfoSummary():"null"));

			//send state
			addServerState(metricsbuf, statusInfo);

			//Adding Server transactions stats
			addL2RuntimeMetrics(metricsbuf, txStats);

			//Adding Server used memory/disk space
			addL2DataMetrics(metricsbuf, dataStats);

			//Adding client metrics
			if(jmxTCClient.isNodeActive()){
				if(log.isDebugEnabled())
					log.debug(String.format("Node %s - State = Active...fetching registered client metrics", (null != l2ProcessInfo)?l2ProcessInfo.getServerInfoSummary():"null"));

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
					if(log.isDebugEnabled())
						log.debug(String.format("Node %s - Does not have any registered clients...sending null client metrics", (null != l2ProcessInfo)?l2ProcessInfo.getServerInfoSummary():"null"));

					addClientCountConnected(metricsbuf, null);
					addClientRuntimeMetrics(metricsbuf, null, null); //for learning if the "All" format
					addClientRuntimeMetrics(metricsbuf, "*", null); //for learning if the "id/<text>" format
				}

				//if it's not null, means that there are indeed some tunneled ehcache mbeans here...let's jump into ehcache stats then!!
				if(null != clientsWithTunneledBeansRegistered){
					if(log.isDebugEnabled())
						log.debug(String.format("Node %s - %d clients registered with ehcache mbeans", (null != l2ProcessInfo)?l2ProcessInfo.getServerInfoSummary():"null", clientsWithTunneledBeansRegistered.size()));

					//the map to aggregate all the cachemanager/cache counts for all clients
					Map<String, Double> aggregateCounts = new HashMap<String, Double>();

					// Loop over CacheManagers
					Map<String, CacheManagerInfo> cacheManagerInfo = jmxTCClient.getCacheManagerInfo();
					Iterator<Entry<String, CacheManagerInfo>> iter = cacheManagerInfo.entrySet().iterator();
					while (iter.hasNext()) {
						Entry<String, CacheManagerInfo> cmInfoElem = iter.next();
						CacheManagerInfo cmInfo = cmInfoElem.getValue();

						for(String cacheName : cmInfo.getCaches()){
							int cacheStatsClientEnabledCount = 0;
							for(String clientId : cmInfo.getClientMbeansIDs()){
								if(log.isDebugEnabled())
									log.debug(String.format("Node %s - Getting stats for cache=[%s] and client=[%s]", (null != l2ProcessInfo)?l2ProcessInfo.getServerInfoSummary():"null", cacheName, clientId));

								//enable statistics if specified
								CacheStats cacheStats = null;
								if(enableEhcacheStats || disableEhcacheStats){
									// if regex expressions are null, stats should be modified for all
									//and if not, both patterns must be true to enter
									if((ehcacheStatsFilterCaches == null || isPatternMatch(ehcacheStatsFilterCaches, cacheName)) &&
											(ehcacheStatsFilterClients == null || isPatternMatch(ehcacheStatsFilterClients, clientId)))
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
										addEhcacheClientRawCountsMetrics(metricsbuf, aggregateCounts, clientId, cmInfo.getCmName(), cacheName, cacheStats);

										if(trackUniqueClients){
											addEhcacheSizeMetrics(metricsbuf, clientId, cmInfo.getCmName(), cacheName, cacheStats);
											addEhcacheAggregatesMetrics(metricsbuf, aggregateCounts, clientId, cmInfo.getCmName(), cacheName, timeSpentSinceLastCall);
										}

										//average size metrics for all clients - TODO: change that and add it to the aggregate
										addEhcacheSizeMetrics(metricsbuf, null, cmInfo.getCmName(), cacheName, cacheStats);
									}
								} else {
									log.error(String.format("Node %s - Could not get cache stats for %s-%s-%s", (null != l2ProcessInfo)?l2ProcessInfo.getServerInfoSummary():"null", cmInfo.getCmName(), cacheName, clientId));
								}
							}

							//submit metrics aggregated over all clients
							addEhcacheAggregatesMetrics(metricsbuf, aggregateCounts, null, cmInfo.getCmName(), cacheName, timeSpentSinceLastCall);
							addEhcacheClientCountStatsEnabled(metricsbuf, null, cmInfo.getCmName(), cacheName, cacheStatsClientEnabledCount);
						}

						//submit metrics aggregated over all clients and all caches in a cache manager
						//addEhcacheAggregatesMetrics(metricsbuf, aggregateCounts, null, cmInfo.getCmName(), null, timeSpentSinceLastCall);
					}
				} else {
					if(log.isDebugEnabled())
						log.debug(String.format("Node %s - Does not have any ehcache mbeans...", (null != l2ProcessInfo)?l2ProcessInfo.getServerInfoSummary():"null"));

					if(learningMode){
						if(log.isDebugEnabled())
							log.debug(String.format("Learning mode: Sending null ehcache client metrics"));

						//this node has not ehcache mbeans...so send null values...
						addEhcacheAggregatesMetrics(metricsbuf, learningModeAggregateCounts, null, null, null, 1L); //for learning if the "All" format
						addEhcacheAggregatesMetrics(metricsbuf, learningModeAggregateCounts, "*", "*", "*", 1L); //for learning if the "id/<text>" format
						addEhcacheSizeMetrics(metricsbuf, null, null, null, null);
						addEhcacheSizeMetrics(metricsbuf, "*", "*", "*", null);
						addEhcacheClientCountStatsEnabled(metricsbuf, null, null, null, null); //for learning if the "All" format
						addEhcacheClientCountStatsEnabled(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format
					}
				}
			} else {
				if(log.isDebugEnabled())
					log.debug(String.format("Node %s - State is not active...no client info available.", (null != l2ProcessInfo)?l2ProcessInfo.getServerInfoSummary():"null"));

				if(learningMode){
					if(log.isDebugEnabled())
						log.debug(String.format("Learning mode: Sending null client metrics"));

					//this node is not active...it does not have any client stats...so send null values...
					addClientCountConnected(metricsbuf, null);

					addClientRuntimeMetrics(metricsbuf, null, null); //for learning if the "All" format
					addClientRuntimeMetrics(metricsbuf, "*", null); //for learning if the "id/<text>" format

					addEhcacheAggregatesMetrics(metricsbuf, learningModeAggregateCounts, null, null, null, 1L); //for learning if the "All" format
					addEhcacheAggregatesMetrics(metricsbuf, learningModeAggregateCounts, "*", "*", "*", 1L); //for learning if the "id/<text>" format
					addEhcacheSizeMetrics(metricsbuf, null, null, null, null);
					addEhcacheSizeMetrics(metricsbuf, "*", "*", "*", null);
					addEhcacheClientCountStatsEnabled(metricsbuf, null, null, null, null); //for learning if the "All" format
					addEhcacheClientCountStatsEnabled(metricsbuf, "*", "*", "*", null); //for learning if the "id/<text>" format
				}
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("addMetrics end - Tiem spent (ms):" + (System.currentTimeMillis() - startTime));
		}
	}

	private boolean isPatternMatch(Pattern pat, String name){
		boolean match = false;
		if(null != pat){
			Matcher matcher = pat.matcher(name);
			match = matcher.find();
		}
		return match;
	}

	private void addServerState(MetricsBuffer metrics, L2RuntimeStatus statusInfo){
		log.info("begin addServerState");

		metrics.addMetric(new ServerMetric("State", MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createSingleMax()), (null == statusInfo)?L2RuntimeState.ERROR.getStateIntValue():statusInfo.getState().getStateIntValue());
	}

	private void addClientCountConnected(MetricsBuffer metrics, Integer connectedCount){
		log.info("begin addClientCountConnected");

		metrics.addMetric(new ServerMetric(String.format("%s", "ConnectedClients"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed()), (connectedCount == null)?0:connectedCount);
	}

	private void addClientRuntimeMetrics(MetricsBuffer metrics, String clientID, L2ClientRuntimeInfo clientRuntimeInfo){
		log.info("begin addClientRuntimeMetrics");

		metrics.addMetric(new ClientMetric(String.format("%s/%s", "Transactions", "Total"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, null == clientID || trackUniqueClients), (null == clientRuntimeInfo)?0:clientRuntimeInfo.getTransactionRate());
		metrics.addMetric(new ClientMetric(String.format("%s/%s", "Transactions", "Faults"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, null == clientID || trackUniqueClients), (null == clientRuntimeInfo)?0:clientRuntimeInfo.getObjectFaultRate());
		metrics.addMetric(new ClientMetric(String.format("%s/%s", "Transactions", "Flushes"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, null == clientID || trackUniqueClients), (null == clientRuntimeInfo)?0:clientRuntimeInfo.getObjectFlushRate());
		metrics.addMetric(new ClientMetric(String.format("%s/%s", "Transactions", "Pending"), MetricUnit.Count, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, null == clientID || trackUniqueClients), (null == clientRuntimeInfo)?0:clientRuntimeInfo.getPendingTransactionsCount());
	}

	//this is to be called after aggregateCounts has the right values in there
	private void addEhcacheAggregatesMetrics(MetricsBuffer metrics, Map<String, Double> aggregateCounts, String clientID, String cacheManagerName, String cacheName, long timeInMillisSinceLastCalled){
		addEhcacheAggregateCountsMetrics(metrics, aggregateCounts, clientID, cacheManagerName, cacheName);
		addEhcacheAggregateRatioMetrics(metrics, aggregateCounts, clientID, cacheManagerName, cacheName);
		addEhcacheAggregateRatesMetrics(metrics, aggregateCounts, clientID, cacheManagerName, cacheName, timeInMillisSinceLastCalled);
	}

	private void addEhcacheClientCountStatsEnabled(MetricsBuffer metrics, String clientID, String cacheManagerName, String cacheName, Integer statsEnabledClientCount){
		log.info("begin addEhcacheClientCountStatsEnabled");

		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "StatsEnabled"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients),  (statsEnabledClientCount == null)?0:statsEnabledClientCount);
	}

	private void addEhcacheSizeMetrics(MetricsBuffer metrics, String clientID, String cacheManagerName, String cacheName, CacheStats cacheStats){
		log.info("begin addEhcacheSizeMetrics");

		//add sizes		
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Size", "Total"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getCacheSize());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Size", "LocalHeap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getLocalHeapSize());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Size", "LocalOffheap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getLocalOffHeapSize());
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Size", "LocalDiskOrRemote"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getLocalDiskSize());
	}

	private void addEhcacheClientRawCountsMetrics(MetricsBuffer metrics, Map<String, Double> aggregateCounts, String clientID, String cacheManagerName, String cacheName, CacheStats cacheStats){
		log.info("begin addEhcacheCountsMetrics");
		if(!learningMode && (null == clientID || cacheManagerName == null || cacheName == null)){
			log.info("This method does not support null values for clientID, cacheManagerName, or cacheName. Doing nothing.");
			return;
		}

		if(metrics == null){
			log.info("The metrics object is null. This method must have a non-null metrics object to save the data...Doing nothing.");
			return;
		}

		//this just gather the counts...but not useful to publish as a stat...
		AbstractMetric puts = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Internal", "Puts"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getPutCount());
		AbstractMetric removes = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Internal", "Removes"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getRemovedCount());
		AbstractMetric evictions = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Internal", "Evictions"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getEvictedCount());
		AbstractMetric expirations = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Internal", "Expirations"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getExpiredCount());
		AbstractMetric hitsTotal = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s/%s", "Internal", "Hits", "Total"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getCacheHitCount());
		AbstractMetric missesTotal = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s/%s", "Internal", "Misses", "Total"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getCacheMissCount());
		AbstractMetric hitsLocalHeap = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s/%s", "Internal", "Hits", "LocalHeap"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getInMemoryHitCount());
		AbstractMetric missesLocalHeap = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s/%s", "Internal", "Misses", "LocalHeap"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getInMemoryMissCount());
		AbstractMetric hitsLocalOffheap = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s/%s", "Internal", "Hits", "LocalOffheap"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getOffHeapHitCount());
		AbstractMetric missesLocalOffheap = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s/%s", "Internal", "Misses", "LocalOffheap"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getOffHeapMissCount());
		AbstractMetric hitsLocalDiskOrRemote = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s/%s", "Internal", "Hits", "LocalDiskOrRemote"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getOnDiskHitCount());
		AbstractMetric missesLocalDiskOrRemote = metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s/%s", "Internal", "Misses", "LocalDiskOrRemote"), MetricUnit.Count, AggregationType.DIFFERENTIAL_SUMMARY, MetricResultDefinition.createSingleLastAdded(), clientID, cacheManagerName, cacheName, false), (null == cacheStats)?0:cacheStats.getOnDiskMissCount());

		//perform the aggregations
		if(null != aggregateCounts){
			for(int i=0; i<=2; i++){
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Puts"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, puts.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Removes"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, removes.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Evictions"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, evictions.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Expirations"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, expirations.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "Total"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, hitsTotal.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "Total"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, missesTotal.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalHeap"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, hitsLocalHeap.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalHeap"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, missesLocalHeap.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalOffheap"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, hitsLocalOffheap.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalOffheap"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, missesLocalOffheap.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalDiskOrRemote"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, hitsLocalDiskOrRemote.getMetricDataResults().get(ReturnValueType.LASTADDED));
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalDiskOrRemote"), (i>0)?null:clientID, cacheManagerName, (i>1)?null:cacheName, missesLocalDiskOrRemote.getMetricDataResults().get(ReturnValueType.LASTADDED));
			}
		}
	}

	/*This is for the learning mode*/
	private void simulateAggregateCount(Map<String, Double> aggregateCounts){
		//perform the aggregations
		if(null != aggregateCounts){
			for(int i=0; i<=1; i++){
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Puts"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Removes"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Evictions"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Expirations"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "Total"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "Total"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalHeap"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalHeap"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalOffheap"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalOffheap"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalDiskOrRemote"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
				addEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalDiskOrRemote"), (i>0)?null:"*", (i>0)?null:"*", (i>0)?null:"*", 0.0D);
			}
		}
	}

	private String getEhcacheAggregateCountKey(String metricName, String clientId, String cacheManagerName, String cacheName){
		return new StringBuilder(metricName).append("-").append(clientId).append("-").append(cacheManagerName).append("-").append(cacheName).toString();
	}

	private Double getEhcacheAggregateCounts(Map<String, Double> aggregateCounts, String metricName, String clientId, String cacheManagerName, String cacheName){
		return (null != aggregateCounts)?aggregateCounts.get(getEhcacheAggregateCountKey(metricName, clientId, cacheManagerName, cacheName)):((learningMode)?0.0D:null);
	}

	private void addEhcacheAggregateCounts(Map<String, Double> aggregateCounts, String metricName, String clientId, String cacheManagerName, String cacheName, Number newValue){
		log.info("begin addEhcacheAggregateCounts");

		if(null != newValue && newValue.doubleValue() != Double.NaN){
			String key = getEhcacheAggregateCountKey(metricName, clientId, cacheManagerName, cacheName);
			double dValue;
			if(!aggregateCounts.containsKey(key)){
				dValue = newValue.doubleValue();
			} else {
				dValue = aggregateCounts.get(key) + newValue.doubleValue();
			}

			if(log.isDebugEnabled())
				log.debug(String.format("Adding value %f to aggregate map - New Total for %s=%f", newValue.doubleValue(), key, dValue));

			aggregateCounts.put(key, dValue);
		} else {
			if(log.isDebugEnabled())
				log.debug(String.format("newValue is null or NaN. Not adding."));
		}
	}

	private void addEhcacheAggregateHitRatio(MetricsBuffer metrics, String metricName, String clientId, String cacheManagerName, String cacheName, Number hits, Number miss){
		log.info("begin addEhcacheAggregateHitRatio");
		
		Double ratio = null;
		if(null != hits && null != miss){
			double dHits=hits.doubleValue();
			double dMisses=miss.doubleValue();
			if(dHits != Double.NaN && dMisses != Double.NaN){
				if(dHits + dMisses != 0.0D){
					ratio = dHits * 100 / (dHits + dMisses);
				} else {
					if(log.isDebugEnabled())
						log.debug("hits/miss counts are 0...recording that.");
					ratio = 0.0D;
				}
			} else{
				if(log.isDebugEnabled())
					log.debug("hits/miss are NaN. Ratio = null");
				ratio = null;
			}
			
			if(log.isDebugEnabled())
				log.debug(String.format("Metric %s/%s/%s - Ratio: %f * 100 / (%f + %f) = %s", cacheManagerName, cacheName, metricName, dHits, dHits, dMisses, (null != ratio)?ratio.toString():"null"));
		} else {
			if(log.isDebugEnabled())
				log.debug(String.format("Metric %s/%s/%s - hits/miss are null. Ratio = null", cacheManagerName, cacheName, metricName));
			ratio = null;
		}

		if(null != ratio)
			metrics.addMetric(new EhcacheClientMetric(metricName, MetricUnit.Percent, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), ratio);
	}

	private void addEhcacheAggregateRate(MetricsBuffer metrics, String metricName, String clientId, String cacheManagerName, String cacheName, Number value, long timeSpentInMillis){
		log.info("begin addEhcacheAggregateRate");
		
		Double rate = null;
		if(null != value && Long.MIN_VALUE != timeSpentInMillis && timeSpentInMillis != 0){
			double dValue=value.doubleValue();
			if(dValue != Double.NaN){
				rate = dValue * 1000 / timeSpentInMillis;
			} else{
				if(log.isDebugEnabled())
					log.debug("value is NaN. Rate=null");
				rate = null;
			}
			
			if(log.isDebugEnabled())
				log.debug(String.format("Metric %s/%s/%s - Rate: %f * 1000 / %d = %s", cacheManagerName, cacheName, metricName, dValue, timeSpentInMillis, (null != rate)?rate.toString():"null"));
		} else {
			if(log.isDebugEnabled())
				log.debug(String.format("Metric %s/%s/%s - value[%s] or timeSpentInMillis[%s] is null. Rate=null", cacheManagerName, cacheName, metricName, (null != value)?value.toString():"null", (Long.MIN_VALUE != timeSpentInMillis)?new Long(timeSpentInMillis).toString():"null"));
			rate = null;
		}
		
		if(null != rate)
			metrics.addMetric(new EhcacheClientMetric(metricName, MetricUnit.Rate, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), rate);
	}


	private void addEhcacheAggregateCountsMetrics(MetricsBuffer metrics, Map<String, Double> aggregateCounts, String clientId, String cacheManagerName, String cacheName){
		log.info("begin addEhcacheAggregateCountsMetrics");

		//adding total of all client counts as metric datapoints
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "Total"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "Total"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalHeap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalHeap"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalOffheap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalOffheap"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalDiskOrRemote"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "Total"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "Total"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalHeap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalHeap"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalOffheap"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalOffheap"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalDiskOrRemote"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Puts"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Puts"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Removes"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Removes"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Evictions"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Evictions"), clientId, cacheManagerName, cacheName));
		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Expirations"), MetricUnit.Count, AggregationType.SUMMARY, MetricResultDefinition.createDetailed(), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Expirations"), clientId, cacheManagerName, cacheName));
	}

	private void addEhcacheAggregateRatioMetrics(MetricsBuffer metrics, Map<String, Double> aggregateCounts, String clientId, String cacheManagerName, String cacheName){
		log.info("begin addEhcacheAggregateRatioMetrics");

		//add the global cache hit ratio based on aggregated client counts
		addEhcacheAggregateHitRatio(metrics, String.format("%s/%s", "HitRatio", "Total"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "Total"), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "Total"), clientId, cacheManagerName, cacheName));
		addEhcacheAggregateHitRatio(metrics, String.format("%s/%s", "HitRatio", "LocalHeap"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalHeap"), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalHeap"), clientId, cacheManagerName, cacheName));
		addEhcacheAggregateHitRatio(metrics, String.format("%s/%s", "HitRatio", "LocalOffheap"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalOffheap"), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalOffheap"), clientId, cacheManagerName, cacheName));
		addEhcacheAggregateHitRatio(metrics, String.format("%s/%s", "HitRatio", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName), getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName));
	}

	//add the calculated rates as well
	private void addEhcacheAggregateRatesMetrics(MetricsBuffer metrics, Map<String, Double> aggregateCounts, String clientId, String cacheManagerName, String cacheName, long timeInMillisSinceLastCalled){
		log.info("begin addEhcacheAggregateRatesMetrics");

		addEhcacheAggregateRate(metrics, String.format("%s/%s", "Hits", "Total"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "Total"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s/%s", "Hits", "LocalHeap"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalHeap"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s/%s", "Hits", "LocalOffheap"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalOffheap"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s/%s", "Hits", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Hits", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s/%s", "Misses", "Total"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "Total"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s/%s", "Misses", "LocalHeap"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalHeap"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s/%s", "Misses", "LocalOffheap"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalOffheap"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s/%s", "Misses", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s/%s", "Misses", "LocalDiskOrRemote"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s", "Puts"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Puts"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s", "Removes"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Removes"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s", "Evictions"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Evictions"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
		addEhcacheAggregateRate(metrics, String.format("%s", "Expirations"), clientId, cacheManagerName, cacheName, getEhcacheAggregateCounts(aggregateCounts, String.format("%s", "Expirations"), clientId, cacheManagerName, cacheName), timeInMillisSinceLastCalled);
	}

	
	//	private void addEhcacheRatesMetrics(MetricsBuffer metrics, String clientID, String cacheManagerName, String cacheName, CacheStats cacheStats){
	//		log.info("begin addEhcacheRatesMetrics");
	//
	//		//add this metric only if client is specified...otherwise does not make sense to average the hitratios
	//		if(!learningMode && (null == clientID || cacheManagerName == null || cacheName == null)){
	//			log.info("This method does not support null values for clientID, cacheManagerName, or cacheName. Doing nothing.");
	//			return;
	//		}
	//
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "HitRatio", "Total"), MetricUnit.Percent, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getCacheHitRatio());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "HitRatio", "LocalHeap"), MetricUnit.Percent, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), 0);
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "HitRatio", "LocalOffheap"), MetricUnit.Percent, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), 0);
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "HitRatio", "LocalDiskOrRemote"), MetricUnit.Percent, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), 0);
	//
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "Total"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getCacheHitRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalHeap"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getOnHeapHitRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalOffheap"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getOffHeapHitRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Hits", "LocalDiskOrRemote"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getOnDiskHitRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "Total"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getCacheMissRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalHeap"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getOnHeapMissRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalOffheap"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getOffHeapMissRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s/%s", "Misses", "LocalDiskOrRemote"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getOnDiskMissRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Puts"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getCachePutRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Removes"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getRemoveRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Evictions"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getEvictionRate());
	//		metrics.addMetric(new EhcacheClientMetric(String.format("%s", "Expirations"), MetricUnit.Rate, AggregationType.EXTENDED, MetricResultDefinition.createDetailed(), clientID, cacheManagerName, cacheName, null == clientID || trackUniqueClients), (null == cacheStats)?0:cacheStats.getExpirationRate());
	//	}

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
