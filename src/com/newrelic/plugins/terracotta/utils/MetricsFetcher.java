package com.newrelic.plugins.terracotta.utils;

import java.util.ArrayList;
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

	public MetricsFetcher(TCL2JMXClient jmxTCClient, boolean trackUniqueClients) {
		super();
		this.jmxTCClient = jmxTCClient;
		this.trackUniqueClients = trackUniqueClients;
	}

	public TCL2JMXClient getJmxTCClient() {
		return jmxTCClient;
	}

	//method called by internal scheduled thread
	//should always return something...if JMX is not accessible, should return empty metrics
	public List<Metric> getMetricsFromServer() throws ConfigurationException {
		List<Metric> metrics = new ArrayList<Metric>();

		//check in that method if JMX is alive and well...
		if(null == jmxTCClient || !jmxTCClient.initialize()){
			log.error("JMX connection could not be initialized...sending null metrics");

			//send error state
			metrics.add(new Metric(SERVER_STATE, NewRelicMetricType.Count, L2RuntimeState.ERROR.getStateIntValue()));

			//send null values for transactions stats
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Heap", "Faults"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Heap", "Flushes"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "OffHeap", "Faults"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "OffHeap", "Flushes"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Disk", "Faults"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s", l2NodesPrefix, "Transactions", "All"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s", l2NodesPrefix, "Transactions", "LockRecalls"), NewRelicMetricType.Rate, 0));

			//send null values for used space
			metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "Total"), NewRelicMetricType.Count, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "OffHeap"), NewRelicMetricType.Count, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "Heap"), NewRelicMetricType.Count, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "Heap", "UsedSize"), NewRelicMetricType.Bytes, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "Heap", "MaxSize"), NewRelicMetricType.Bytes, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "KeyMapSize"), NewRelicMetricType.Bytes, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "MaxSize"), NewRelicMetricType.Bytes, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "ObjectSize"), NewRelicMetricType.Bytes, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "TotalUsedSize"), NewRelicMetricType.Bytes, 0));

			//send null values for clients
			metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Connected"), NewRelicMetricType.Count, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "All"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Faults"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Flushes"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Pending"), NewRelicMetricType.Count,0 ));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "Size"), NewRelicMetricType.Count, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "HitRatio"), NewRelicMetricType.Percent, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "HitRate"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "MissRate"), NewRelicMetricType.Rate, 0));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "PutRate"), NewRelicMetricType.Rate, 0));
		} else {
			L2ProcessInfo l2ProcessInfo = jmxTCClient.getL2ProcessInfo();
			L2TransactionsStats txStats = jmxTCClient.getL2TransactionsStats();
			L2DataStats dataStats = jmxTCClient.getL2DataStats();
			L2RuntimeStatus statusInfo = jmxTCClient.getL2RuntimeStatus();

			log.info(String.format("Getting L2 Metrics From Server %s", l2ProcessInfo.getServerInfoSummary()));

			//send state
			metrics.add(new Metric(SERVER_STATE, NewRelicMetricType.Count, statusInfo.getState().getStateIntValue()));

			//Adding Server transactions stats
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Heap", "Faults"), NewRelicMetricType.Rate, txStats.getOnHeapFaultRate()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Heap", "Flushes"), NewRelicMetricType.Rate, txStats.getOnHeapFlushRate()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "OffHeap", "Faults"), NewRelicMetricType.Rate, txStats.getOffHeapFaultRate()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "OffHeap", "Flushes"), NewRelicMetricType.Rate, txStats.getOffHeapFlushRate()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Disk", "Faults"), NewRelicMetricType.Rate, txStats.getL2DiskFaultRate()));
			metrics.add(new Metric(String.format("%s/%s/%s", l2NodesPrefix, "Transactions", "All"), NewRelicMetricType.Rate, txStats.getTransactionRate()));
			metrics.add(new Metric(String.format("%s/%s/%s", l2NodesPrefix, "Transactions", "LockRecalls"), NewRelicMetricType.Rate, txStats.getGlobalLockRecallRate()));

			//Adding Server used memory/disk space
			metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "Total"), NewRelicMetricType.Count, dataStats.getLiveObjectCount()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "OffHeap"), NewRelicMetricType.Count, dataStats.getOffheapObjectCachedCount()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "Heap"), NewRelicMetricType.Count, dataStats.getCachedObjectCount()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "Heap", "UsedSize"), NewRelicMetricType.Bytes, statusInfo.getUsedHeap()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "Heap", "MaxSize"), NewRelicMetricType.Bytes, statusInfo.getMaxHeap()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "KeyMapSize"), NewRelicMetricType.Bytes, dataStats.getOffheapMapAllocatedMemory()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "MaxSize"), NewRelicMetricType.Bytes, dataStats.getOffheapMaxDataSize()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "ObjectSize"), NewRelicMetricType.Bytes, dataStats.getOffheapObjectAllocatedMemory()));
			metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "Tiers", "OffHeap", "TotalUsedSize"), NewRelicMetricType.Bytes, dataStats.getOffheapTotalAllocatedSize()));

			//Adding client metrics
			if(jmxTCClient.isNodeActive()){
				log.debug(String.format("Node %s is in active state...fetching registered client metrics", l2ProcessInfo.getServerInfoSummary()));
				
				//list that contains the clientIDs that have ehcache Mbeans tunnelled and registered
				List<L2ClientID> clientsWithTunneledBeansRegistered = null;

				//get client Details now
				L2ClientRuntimeInfo[] runtimeStatsClientsArray = jmxTCClient.getClients();
				if(null != runtimeStatsClientsArray && runtimeStatsClientsArray.length > 0){
					metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Connected"), NewRelicMetricType.Count, runtimeStatsClientsArray.length));
					for(L2ClientRuntimeInfo clientRuntimeInfo : runtimeStatsClientsArray){
						String clientId = clientRuntimeInfo.getClientID().getRemoteAddress();

						metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "All"), NewRelicMetricType.Rate, clientRuntimeInfo.getTransactionRate()));
						metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Faults"), NewRelicMetricType.Rate, clientRuntimeInfo.getObjectFaultRate()));
						metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Flushes"), NewRelicMetricType.Rate, clientRuntimeInfo.getObjectFlushRate()));
						metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Pending"), NewRelicMetricType.Count, clientRuntimeInfo.getPendingTransactionsCount()));

						if(trackUniqueClients){
							metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientId, "Transactions", "All"), NewRelicMetricType.Rate, clientRuntimeInfo.getTransactionRate()));
							metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientId, "Transactions", "Faults"), NewRelicMetricType.Rate, clientRuntimeInfo.getObjectFaultRate()));
							metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientId, "Transactions", "Flushes"), NewRelicMetricType.Rate, clientRuntimeInfo.getObjectFlushRate()));
							metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientId, "Transactions", "Pending"), NewRelicMetricType.Count, clientRuntimeInfo.getPendingTransactionsCount()));
						}

						if(clientRuntimeInfo.getClientID().isTunneledBeansRegistered()){
							if(null == clientsWithTunneledBeansRegistered)
								clientsWithTunneledBeansRegistered = new ArrayList<L2ClientID>();

							clientsWithTunneledBeansRegistered.add(clientRuntimeInfo.getClientID());
						}
					}
				} else {
					log.debug(String.format("Node %s does not have any registered clients...sending null client metrics", l2ProcessInfo.getServerInfoSummary()));

					metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Connected"), NewRelicMetricType.Count, 0));
					metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "All"), NewRelicMetricType.Rate, 0));
					metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Faults"), NewRelicMetricType.Rate, 0));
					metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Flushes"), NewRelicMetricType.Rate, 0));
					metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Pending"), NewRelicMetricType.Count,0 ));
				}

				//if it's not null, means that there are indeed some tunnelled ehcache mbeans here...let's jump into ehcache stats then!!
				if(null != clientsWithTunneledBeansRegistered){
					log.debug(String.format("Node %s has %d clients registered with ehcache mbeans", l2ProcessInfo.getServerInfoSummary(), clientsWithTunneledBeansRegistered.size()));
					
					// Loop over CacheManagers
					Map<String, CacheManagerInfo> cacheManagerInfo = jmxTCClient.getCacheManagerInfo();
					Iterator<Entry<String, CacheManagerInfo>> iter = cacheManagerInfo.entrySet().iterator();
					while (iter.hasNext()) {
						Entry<String, CacheManagerInfo> cmInfoElem = iter.next();
						CacheManagerInfo cmInfo = cmInfoElem.getValue();

						for(String cacheName : cmInfo.getCaches()){
							for(String clientId : cmInfo.getClientMbeansIDs()){
								CacheStats cacheStats = jmxTCClient.getCacheStats(cmInfo.getCmName(), cacheName, clientId);

								metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "Size"), NewRelicMetricType.Count, cacheStats.getCacheSize()));
								metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "HitRatio"), NewRelicMetricType.Percent, cacheStats.getCacheHitRatio()));
								metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "HitRate"), NewRelicMetricType.Rate, cacheStats.getCacheHitRate()));
								metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "MissRate"), NewRelicMetricType.Rate, cacheStats.getCacheMissRate()));
								metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "PutRate"), NewRelicMetricType.Rate, cacheStats.getCachePutRate()));
								
								if(trackUniqueClients){
									metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientId, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "Size"), NewRelicMetricType.Count, cacheStats.getCacheSize()));
									metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientId, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "HitRatio"), NewRelicMetricType.Percent, cacheStats.getCacheHitRatio()));
									metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientId, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "HitRate"), NewRelicMetricType.Rate, cacheStats.getCacheHitRate()));
									metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientId, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "MissRate"), NewRelicMetricType.Rate, cacheStats.getCacheMissRate()));
									metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, clientId, METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "PutRate"), NewRelicMetricType.Rate, cacheStats.getCachePutRate()));
								}
							}
						}
					}
				} else {
					log.debug(String.format("Node %s does not have any ehcache mbeans...sending null ehcache client metrics", l2ProcessInfo.getServerInfoSummary()));

					//this node has not ehcache mbeans...so send null values...
					metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "Size"), NewRelicMetricType.Count, 0));
					metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "HitRatio"), NewRelicMetricType.Percent, 0));
					metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "HitRate"), NewRelicMetricType.Rate, 0));
					metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "MissRate"), NewRelicMetricType.Rate, 0));
					metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "PutRate"), NewRelicMetricType.Rate, 0));
				}
			} else {
				log.debug(String.format("Node %s is not in active state...sending null client metrics", l2ProcessInfo.getServerInfoSummary()));
				
				//this node is not active...it does not have any client stats...so send null values...
				metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Connected"), NewRelicMetricType.Count, 0));
				metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "All"), NewRelicMetricType.Rate, 0));
				metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Faults"), NewRelicMetricType.Rate, 0));
				metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Flushes"), NewRelicMetricType.Rate, 0));
				metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, "Transactions", "Pending"), NewRelicMetricType.Count,0 ));
				metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "Size"), NewRelicMetricType.Count, 0));
				metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "HitRatio"), NewRelicMetricType.Percent, 0));
				metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "HitRate"), NewRelicMetricType.Rate, 0));
				metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "MissRate"), NewRelicMetricType.Rate, 0));
				metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, METRICS_CLIENTS_ALL, METRICS_FAMILY_EHCACHE, "*", "*", "PutRate"), NewRelicMetricType.Rate, 0));
			}
		}
		return metrics;
	}
}
