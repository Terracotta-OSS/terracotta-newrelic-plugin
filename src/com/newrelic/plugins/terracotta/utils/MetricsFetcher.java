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

import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class MetricsFetcher {
	private static Logger log = LoggerFactory.getLogger(MetricsFetcher.class);

	public static final String METRICS_FAMILY_TC = "Terracotta";
	public static final String METRICS_FAMILY_MASTERNODE = "MasterNode";
	public static final String METRICS_FAMILY_EHCACHE = "Ehcache";
	public static final String METRICS_FAMILY_CLIENTS = "Clients";
	public static final String METRICS_FAMILY_TC_STRIPES = "Stripes";
	public static final String METRICS_FAMILY_TC_NODES = "Servers";
	public static final String METRICS_GROUPING_L2STORAGE = "Storage";
	public static final String METRICS_GROUPING_L2RUNTIME = "Runtime";
	public static final String METRICS_GROUPING_L2STATE = "l2status";
	public static final String METRICS_CLIENTS_ALL = "All";

	public static final String l2NodesPrefix = String.format("%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_TC_NODES);
	public static final String clientsPrefix = String.format("%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_CLIENTS);

	public static final String SERVER_STATE = String.format("%s/%s", l2NodesPrefix, "State");

	private final TCL2JMXClient jmxTCClient;
	public final boolean trackUniqueClients;

	public final boolean learningMode = PluginConfig.getInstance().getPropertyAsBoolean("com.newrelic.plugins.terracotta.learningmode", false);
	public final boolean disableEhcacheStats = PluginConfig.getInstance().getPropertyAsBoolean("com.newrelic.plugins.terracotta.ehcache.statistics.disable", false);
	public final boolean enableEhcacheStats = PluginConfig.getInstance().getPropertyAsBoolean("com.newrelic.plugins.terracotta.ehcache.statistics.enable", false);
	public final String[] ehcacheStatsFilterCaches;
	public final String[] ehcacheStatsFilterClients;

	public MetricsFetcher(TCL2JMXClient jmxTCClient, boolean trackUniqueClients) {
		super();
		this.jmxTCClient = jmxTCClient;
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

	//method called by internal scheduled thread
	//should always return something...if JMX is not accessible, should return empty metrics
	public Metric[] getMetricsFromServer() throws ConfigurationException {
		List<Metric> metrics = new ArrayList<Metric>();

		//check in that method if JMX is alive and well...
		if(null == jmxTCClient || !jmxTCClient.initialize()){
			log.error("JMX connection could not be initialized...sending null metrics");

			//send error state
			metrics.add(new Metric(SERVER_STATE, NewRelicMetricType.Count, L2RuntimeState.ERROR.getStateIntValue()));

			//send null values for transactions stats
			addL2RuntimeMetrics(metrics, null);

			//send null values for used space
			addL2DataMetrics(metrics, null);

			if(learningMode){
				//send null values for clients
				addClientCountConnected(metrics, null);
				addRuntimeMetricsForClient(metrics, METRICS_CLIENTS_ALL, null);
				addEhcacheClientCountStatsEnabled(metrics, METRICS_CLIENTS_ALL, "*", "*", null);
				addEhcacheMetricsForClient(metrics, METRICS_CLIENTS_ALL, "*", "*", null);
			}
		} else {
			L2ProcessInfo l2ProcessInfo = jmxTCClient.getL2ProcessInfo();
			L2TransactionsStats txStats = jmxTCClient.getL2TransactionsStats();
			L2DataStats dataStats = jmxTCClient.getL2DataStats();
			L2RuntimeStatus statusInfo = jmxTCClient.getL2RuntimeStatus();

			log.info(String.format("Getting L2 Metrics From Server %s", l2ProcessInfo.getServerInfoSummary()));

			//send state
			metrics.add(new Metric(SERVER_STATE, NewRelicMetricType.Count, statusInfo.getState().getStateIntValue()));

			//Adding Server transactions stats
			addL2RuntimeMetrics(metrics, txStats);

			//Adding Server used memory/disk space
			addL2DataMetrics(metrics, dataStats);

			//Adding client metrics
			if(jmxTCClient.isNodeActive()){
				log.debug(String.format("Node %s is in active state...fetching registered client metrics", l2ProcessInfo.getServerInfoSummary()));

				//list that contains the clientIDs that have ehcache Mbeans tunnelled and registered
				List<L2ClientID> clientsWithTunneledBeansRegistered = null;

				//get client Details now
				L2ClientRuntimeInfo[] runtimeStatsClientsArray = jmxTCClient.getClients();
				if(null != runtimeStatsClientsArray && runtimeStatsClientsArray.length > 0){
					addClientCountConnected(metrics, runtimeStatsClientsArray.length);
					for(L2ClientRuntimeInfo clientRuntimeInfo : runtimeStatsClientsArray){
						addRuntimeMetricsForClient(metrics, METRICS_CLIENTS_ALL, clientRuntimeInfo);

						if(trackUniqueClients){
							String clientId = clientRuntimeInfo.getClientID().getRemoteAddress();
							addRuntimeMetricsForClient(metrics, clientId, clientRuntimeInfo);
						}

						if(clientRuntimeInfo.getClientID().isTunneledBeansRegistered()){
							if(null == clientsWithTunneledBeansRegistered)
								clientsWithTunneledBeansRegistered = new ArrayList<L2ClientID>();

							clientsWithTunneledBeansRegistered.add(clientRuntimeInfo.getClientID());
						}
					}
				} else {
					log.debug(String.format("Node %s does not have any registered clients...sending null client metrics", l2ProcessInfo.getServerInfoSummary()));

					addClientCountConnected(metrics, null);
					addRuntimeMetricsForClient(metrics, METRICS_CLIENTS_ALL, null);
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
											cacheStats = jmxTCClient.getCacheStatsAndDisableStatistics(cmInfo.getCmName(), cacheName, clientId);
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

										addEhcacheMetricsForClient(metrics, METRICS_CLIENTS_ALL, cmInfo.getCmName(), cacheName, cacheStats);

										if(trackUniqueClients){
											addEhcacheMetricsForClient(metrics, clientId, cmInfo.getCmName(), cacheName, cacheStats);
										}
									}
								} else {
									log.error(String.format("Could not get cache stats for %s-%s-%s", cmInfo.getCmName(), cacheName, clientId));
								}
							}
							addEhcacheClientCountStatsEnabled(metrics, METRICS_CLIENTS_ALL, cmInfo.getCmName(), cacheName, cacheStatsClientEnabledCount);
						}
					}
				} else {
					if(log.isDebugEnabled())
						log.debug(String.format("Node %s does not have any ehcache mbeans...", l2ProcessInfo.getServerInfoSummary()));

					if(learningMode){
						log.debug(String.format("Sending null ehcache client metrics"));

						//this node has not ehcache mbeans...so send null values...
						addEhcacheMetricsForClient(metrics, METRICS_CLIENTS_ALL, "*", "*", null);
						addEhcacheClientCountStatsEnabled(metrics, METRICS_CLIENTS_ALL, "*", "*", null);
					}
				}
			} else {
				log.debug(String.format("Node %s is not in active state...", l2ProcessInfo.getServerInfoSummary()));
				if(learningMode){
					log.debug(String.format("Sending null client metrics"));

					//this node is not active...it does not have any client stats...so send null values...
					addClientCountConnected(metrics, null);
					addRuntimeMetricsForClient(metrics, METRICS_CLIENTS_ALL, null);

					addEhcacheMetricsForClient(metrics, METRICS_CLIENTS_ALL, "*", "*", null);
					addEhcacheClientCountStatsEnabled(metrics, METRICS_CLIENTS_ALL, "*", "*", null);
				}
			}
		}

		return (null != metrics)?metrics.toArray(new Metric[metrics.size()]):null;
	}

	private void addClientCountConnected(List<Metric> metrics, Integer connectedCount){
		metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Connected"), NewRelicMetricType.Count, (connectedCount == null)?0:connectedCount));
	}

	private void addEhcacheClientCountStatsEnabled(List<Metric> metrics, String clientID, String cacheManagerName, String cacheName, Integer statsEnabledClientCount){
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "StatsEnabled"), NewRelicMetricType.Count, (statsEnabledClientCount == null)?0:statsEnabledClientCount));
	}

	private void addRuntimeMetricsForClient(List<Metric> metrics, String clientID, L2ClientRuntimeInfo clientRuntimeInfo){
		metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientID, "Transactions", "All"), NewRelicMetricType.Rate, (null == clientRuntimeInfo)?0:clientRuntimeInfo.getTransactionRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientID, "Transactions", "Faults"), NewRelicMetricType.Rate, (null == clientRuntimeInfo)?0:clientRuntimeInfo.getObjectFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientID, "Transactions", "Flushes"), NewRelicMetricType.Rate, (null == clientRuntimeInfo)?0:clientRuntimeInfo.getObjectFlushRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientID, "Transactions", "Pending"), NewRelicMetricType.Count, (null == clientRuntimeInfo)?0:clientRuntimeInfo.getPendingTransactionsCount()));
	}

	private void addEhcacheMetricsForClient(List<Metric> metrics, String clientID, String cacheManagerName, String cacheName, CacheStats cacheStats){
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Size", "Total"), NewRelicMetricType.Count, (null == cacheStats)?0:cacheStats.getCacheSize()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Size", "LocalHeap"), NewRelicMetricType.Count, (null == cacheStats)?0:cacheStats.getLocalHeapSize()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Size", "LocalOffheap"), NewRelicMetricType.Count, (null == cacheStats)?0:cacheStats.getLocalOffHeapSize()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Size", "LocalDiskOrRemote"), NewRelicMetricType.Count, (null == cacheStats)?0:cacheStats.getLocalDiskSize()));

		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "HitRatio"), NewRelicMetricType.Percent, (null == cacheStats)?0:cacheStats.getCacheHitRatio()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Hits", "Total"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getCacheHitRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Hits", "LocalHeap"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getOnHeapHitRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Hits", "LocalOffheap"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getOffHeapHitRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Hits", "LocalDiskOrRemote"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getOnDiskHitRate()));

		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Misses", "Total"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getCacheMissRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Misses", "LocalHeap"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getOnHeapMissRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Misses", "LocalOffheap"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getOffHeapMissRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Misses", "LocalDiskOrRemote"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getOnDiskMissRate()));

		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Puts"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getCachePutRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Evictions"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getEvictionRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientID, METRICS_FAMILY_EHCACHE, cacheManagerName, cacheName, "Expirations"), NewRelicMetricType.Rate, (null == cacheStats)?0:cacheStats.getExpirationRate()));
	}

	private void addL2RuntimeMetrics(List<Metric> metrics, L2TransactionsStats txStats){
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Heap", "Faults"), NewRelicMetricType.Rate, (null == txStats)?0:txStats.getOnHeapFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Heap", "Flushes"), NewRelicMetricType.Rate, (null == txStats)?0:txStats.getOnHeapFlushRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "OffHeap", "Faults"), NewRelicMetricType.Rate, (null == txStats)?0:txStats.getOffHeapFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "OffHeap", "Flushes"), NewRelicMetricType.Rate, (null == txStats)?0:txStats.getOffHeapFlushRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Disk", "Faults"), NewRelicMetricType.Rate, (null == txStats)?0:txStats.getL2DiskFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s", l2NodesPrefix, "Transactions", "All"), NewRelicMetricType.Rate, (null == txStats)?0:txStats.getTransactionRate()));
		metrics.add(new Metric(String.format("%s/%s/%s", l2NodesPrefix, "Transactions", "LockRecalls"), NewRelicMetricType.Rate, (null == txStats)?0:txStats.getGlobalLockRecallRate()));
	}

	private void addL2DataMetrics(List<Metric> metrics, L2DataStats dataStats){
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "Total"), NewRelicMetricType.Count, (null == dataStats)?0:dataStats.getLiveObjectCount()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "OffHeap"), NewRelicMetricType.Count, (null == dataStats)?0:dataStats.getOffheapObjectCachedCount()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "Heap"), NewRelicMetricType.Count, (null == dataStats)?0:dataStats.getCachedObjectCount()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "Heap", "UsedSize"), NewRelicMetricType.Bytes, (null == dataStats)?0:dataStats.getUsedHeap()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "Heap", "MaxSize"), NewRelicMetricType.Bytes, (null == dataStats)?0:dataStats.getMaxHeap()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "KeyMapSize"), NewRelicMetricType.Bytes, (null == dataStats)?0:dataStats.getOffheapMapAllocatedMemory()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "MaxSize"), NewRelicMetricType.Bytes, (null == dataStats)?0:dataStats.getOffheapMaxDataSize()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "ObjectSize"), NewRelicMetricType.Bytes, (null == dataStats)?0:dataStats.getOffheapObjectAllocatedMemory()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "TotalUsedSize"), NewRelicMetricType.Bytes, (null == dataStats)?0:dataStats.getOffheapTotalAllocatedSize()));
	}
}
