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

public class MetricsFetcher {
	private static Logger log = LoggerFactory.getLogger(MetricsFetcher.class);

	private final TCL2JMXClient jmxTCClient;
	private final L2ProcessInfo l2ProcessInfo;

	private static final String METRICS_FAMILY_TC = "Terracotta";
	private static final String METRICS_FAMILY_MASTERNODE = "MasterNode";
	private static final String METRICS_FAMILY_EHCACHE = "Ehcache";
	private static final String METRICS_FAMILY_CLIENTS = "Clients";
	private static final String METRICS_FAMILY_TC_STRIPES = "Stripes";
	private static final String METRICS_FAMILY_TC_NODES = "Servers";
	private static final String METRICS_GROUPING_L2STORAGE = "Storage";
	private static final String METRICS_GROUPING_L2RUNTIME = "Runtime";
	private static final String METRICS_GROUPING_L2STATE = "l2status";

	public MetricsFetcher(TCL2JMXClient jmxTCClient) {
		super();

		if(null == jmxTCClient){
			throw new IllegalArgumentException("JMX connection could not be initialized.");
		}

		//check that everything worked ok
		this.l2ProcessInfo = jmxTCClient.getL2ProcessInfo();
		if(l2ProcessInfo == null || !jmxTCClient.isInitialized()){
			throw new IllegalArgumentException("JMX connection could not be initialized.");
		}
		this.jmxTCClient = jmxTCClient;
	}

	public TCL2JMXClient getJmxTCClient() {
		return jmxTCClient;
	}

	public L2ProcessInfo getL2ProcessInfo() {
		return l2ProcessInfo;
	}

	public List<Metric> getMetricsFromServer() {
		System.out.println(String.format("Getting L2 Metrics From Server %s", l2ProcessInfo.getServerInfoSummary()));
		List<Metric> metrics = new ArrayList<Metric>();

		L2TransactionsStats txStats = jmxTCClient.getL2TransactionsStats();
		L2DataStats dataStats = jmxTCClient.getL2DataStats();
		L2RuntimeStatus statusInfo = jmxTCClient.getL2RuntimeStatus();

		//add client metrics
		//TODO: the client metrics should probably not be run wihtin each server agents...especially since there are the same accross server nodes...
		//But meh...let's see if that works!
		String clientsPrefix = String.format("%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_CLIENTS);
		if(jmxTCClient.isNodeActive()){
			//list that contains the clientIDs that have ehcache Mbeans tunnelled and registered
			List<L2ClientID> clientsWithTunneledBeansRegistered = null;

			//get client Details now
			L2ClientRuntimeInfo[] runtimeStatsClientsArray = jmxTCClient.getClients();
			if(null != runtimeStatsClientsArray){
				metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, "All", "Connected"), NewRelicMetricType.Count, runtimeStatsClientsArray.length));
				for(L2ClientRuntimeInfo clientRuntimeInfo : runtimeStatsClientsArray){
					String clientId = clientRuntimeInfo.getClientID().getRemoteAddress();

					metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientId, "Transactions", "All"), NewRelicMetricType.Rate, clientRuntimeInfo.getTransactionRate()));
					metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientId, "Transactions", "Faults"), NewRelicMetricType.Rate, clientRuntimeInfo.getObjectFaultRate()));
					metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientId, "Transactions", "Flushes"), NewRelicMetricType.Rate, clientRuntimeInfo.getObjectFlushRate()));
					metrics.add(new Metric(String.format("%s/%s/%s/%s", clientsPrefix, clientId, "Transactions", "Pending"), NewRelicMetricType.Count, clientRuntimeInfo.getPendingTransactionsCount()));

					if(clientRuntimeInfo.getClientID().isTunneledBeansRegistered()){
						if(null == clientsWithTunneledBeansRegistered)
							clientsWithTunneledBeansRegistered = new ArrayList<L2ClientID>();

						clientsWithTunneledBeansRegistered.add(clientRuntimeInfo.getClientID());
					}
				}
			}

			//if it's not null, means that there are indeed some tunnelled ehcache mbeans here...let's jump into ehcache stats then!!
			if(null != clientsWithTunneledBeansRegistered){
				// Loop over CacheManagers
				Map<String, CacheManagerInfo> cacheManagerInfo = jmxTCClient.getCacheManagerInfo();
				Iterator<Entry<String, CacheManagerInfo>> iter = cacheManagerInfo.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<String, CacheManagerInfo> cmInfoElem = iter.next();
					CacheManagerInfo cmInfo = cmInfoElem.getValue();

					//for each cacheName, average all client stats
					//TODO: a bit ugly...should make this better
					Metric cacheSize = null;
					Metric cacheHitRatio = null;
					Metric cacheHitRate = null;
					Metric cacheMissRate = null;
					Metric cachePutRate = null;
					for(String cacheName : cmInfo.getCaches()){
						cacheSize = new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, "All", METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "Size"), NewRelicMetricType.Count);
						cacheHitRatio = new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, "All", METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "HitRatio"), NewRelicMetricType.Percent);
						cacheHitRate = new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, "All", METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "HitRate"), NewRelicMetricType.Rate);
						cacheMissRate = new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, "All", METRICS_FAMILY_EHCACHE, cmInfo.getCmName (), cacheName, "MissRate"), NewRelicMetricType.Rate);
						cachePutRate = new Metric(String.format("%s/%s/%s/%s/%s/%s", clientsPrefix, "All", METRICS_FAMILY_EHCACHE, cmInfo.getCmName(), cacheName, "PutRate"), NewRelicMetricType.Rate);

						for(String clientID : cmInfo.getClientMbeansIDs()){
							CacheStats cacheStats = jmxTCClient.getCacheStats(cmInfo.getCmName(), cacheName, clientID);
							if(null != cacheStats){
								cacheSize.add(cacheStats.getCacheSize());
								cacheHitRatio.add(cacheStats.getCacheHitRatio());
								cacheHitRate.add(cacheStats.getCacheHitRate());
								cacheMissRate.add(cacheStats.getCacheMissRate());
								cachePutRate.add(cacheStats.getCachePutRate());
							}
						}

						if(cacheSize.getDataPointsCount() > 0)
							metrics.add(new Metric(cacheSize.getName(), cacheSize.getUnit(), cacheSize.getAverage()));

						if(cacheHitRatio.getDataPointsCount() > 0)
							metrics.add(new Metric(cacheHitRatio.getName(), cacheHitRatio.getUnit(), cacheHitRatio.getAverage()));

						if(cacheHitRate.getDataPointsCount() > 0)
							metrics.add(new Metric(cacheHitRate.getName(), cacheHitRate.getUnit(), cacheHitRate.getAverage()));

						if(cacheHitRate.getDataPointsCount() > 0)
							metrics.add(new Metric(cacheMissRate.getName(), cacheMissRate.getUnit(), cacheMissRate.getAverage()));

						if(cachePutRate.getDataPointsCount() > 0)
							metrics.add(new Metric(cachePutRate.getName(), cachePutRate.getUnit(), cachePutRate.getAverage()));
					}
				}
			}
		}

		//Global Metrics
		String l2NodesPrefix = String.format("%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_TC_NODES);

		//do not use that one...
		//String stripeNodePrefix = String.format("%s/%s/%s/%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_TC_STRIPES, l2ProcessInfo.getStripeName(), METRICS_FAMILY_TC_NODES, l2ProcessInfo.getNodeName());

		//transactions stats
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Heap", "Faults"), NewRelicMetricType.Rate, txStats.getOnHeapFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Heap", "Flushes"), NewRelicMetricType.Rate, txStats.getOnHeapFlushRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "OffHeap", "Faults"), NewRelicMetricType.Rate, txStats.getOffHeapFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "OffHeap", "Flushes"), NewRelicMetricType.Rate, txStats.getOffHeapFlushRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Transactions", "Tiers", "Disk", "Faults"), NewRelicMetricType.Rate, txStats.getL2DiskFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s", l2NodesPrefix, "Transactions", "All"), NewRelicMetricType.Rate, txStats.getTransactionRate()));
		metrics.add(new Metric(String.format("%s/%s/%s", l2NodesPrefix, "Transactions", "LockRecalls"), NewRelicMetricType.Rate, txStats.getGlobalLockRecallRate()));

		//used space
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "Live"), NewRelicMetricType.Count, dataStats.getLiveObjectCount()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, "Data", "Objects", "Cached"), NewRelicMetricType.Count, dataStats.getCachedObjectCount()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "TieredStorage", "Heap", "UsedSize"), NewRelicMetricType.Bytes, statusInfo.getUsedHeap()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "TieredStorage", "Heap", "MaxSize"), NewRelicMetricType.Bytes, statusInfo.getMaxHeap()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "TieredStorage", "OffHeap", "KeyMapSize"), NewRelicMetricType.Bytes, dataStats.getOffheapMapAllocatedMemory()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "TieredStorage", "OffHeap", "MaxSize"), NewRelicMetricType.Bytes, dataStats.getOffheapMaxDataSize()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "TieredStorage", "OffHeap", "ObjectSize"), NewRelicMetricType.Bytes, dataStats.getOffheapObjectAllocatedMemory()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s", l2NodesPrefix, "Data", "TieredStorage", "OffHeap", "TotalUsedSize"), NewRelicMetricType.Bytes, dataStats.getOffheapTotalAllocatedSize()));

		return metrics;
	}
}
