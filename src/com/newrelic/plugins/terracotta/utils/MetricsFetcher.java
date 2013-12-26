package com.newrelic.plugins.terracotta.utils;

import java.util.ArrayList;
import java.util.List;

import org.terracotta.utils.jmxclient.TCL2JMXClient;
import org.terracotta.utils.jmxclient.beans.L2OffheapStats;
import org.terracotta.utils.jmxclient.beans.L2RuntimeInfo;
import org.terracotta.utils.jmxclient.beans.L2RuntimeStatus;
import org.terracotta.utils.jmxclient.beans.L2UsageStats;

public class MetricsFetcher {
	private final TCL2JMXClient jmxTCClient;
	private final L2RuntimeInfo l2RuntimeInfo;
	
	private static final String METRICS_FAMILY_TC = "Terracotta";
	private static final String METRICS_FAMILY_MASTERNODE = "MasterNode";
	private static final String METRICS_FAMILY_CLIENTS = "Clients";
	private static final String METRICS_FAMILY_TC_STRIPES = "Stripes";
	private static final String METRICS_FAMILY_TC_NODES = "L2Nodes";
	private static final String METRICS_GROUPING_L2STORAGE = "Storage";
	private static final String METRICS_GROUPING_L2RUNTIME = "Runtime";
	private static final String METRICS_GROUPING_L2STATE = "l2status";

	public MetricsFetcher(TCL2JMXClient jmxTCClient) {
		super();
		
		if(null == jmxTCClient){
			throw new IllegalArgumentException("JMX connection could not be initialized.");
		}

		//check that everything worked ok
		this.l2RuntimeInfo = jmxTCClient.getL2RuntimeInfo();
		if(l2RuntimeInfo == null || !jmxTCClient.isInitialized()){
			throw new IllegalArgumentException("JMX connection could not be initialized.");
		}
		this.jmxTCClient = jmxTCClient;
	}
	
	public TCL2JMXClient getJmxTCClient() {
		return jmxTCClient;
	}

	public L2RuntimeInfo getL2RuntimeInfo() {
		return l2RuntimeInfo;
	}
	
	public List<Metric> getMetricsFromServer() {
		System.out.println(String.format("Getting L2 Metrics From Server %s", l2RuntimeInfo.getServerInfoSummary()));
		List<Metric> metrics = new ArrayList<Metric>();
		
		L2UsageStats usageStats = jmxTCClient.getL2UsageStats();
		L2OffheapStats offheapStats = jmxTCClient.getL2OffheapStats();
		L2RuntimeStatus l2StatusInfo = jmxTCClient.getL2RuntimeStatus();

		//metrics.add(new Metric(String.format("%s/%s/%s", METRICS_FAMILY_TC, METRICS_GROUPING_L2STATE, "L2Role"), "value", l2StatusInfo.getRole()));

		//add client metrics
		String clientsPrefix = String.format("%s", METRICS_FAMILY_TC);
		/*
		if(clientsVisibility){
			metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_FAMILY_CLIENTS, "Connections"), NewRelicMetricType.Count, jmxTCClient.getClientCount()));
			
			List<L1UsageStats> l1UsageStatList = jmxTCClient.getAllL1sUsageStats();
			for(L1UsageStats l1UsageStats : l1UsageStatList){
				metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_FAMILY_CLIENTS, l1UsageStats.getRemoteAddress(), "OpsRates", "Transactions"), NewRelicMetricType.Rate, l1UsageStats.getTransactionRate()));
				metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_FAMILY_CLIENTS, l1UsageStats.getRemoteAddress(), "OpsRates", "Faults"), NewRelicMetricType.Rate, l1UsageStats.getObjectFaultRate()));
				metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_FAMILY_CLIENTS, l1UsageStats.getRemoteAddress(), "OpsRates", "Flushes"), NewRelicMetricType.Rate, l1UsageStats.getObjectFlushRate()));
				metrics.add(new Metric(String.format("%s/%s/%s", clientsPrefix, METRICS_FAMILY_CLIENTS, l1UsageStats.getRemoteAddress(), "OpsRates", "PendingTransactions"), NewRelicMetricType.Count, l1UsageStats.getPendingTransactionsCount()));
			}
		}
		*/
		
		//Global Metrics
		String l2NodesPrefix = String.format("%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_TC_NODES);

		//do not use that one...
		//String stripeNodePrefix = String.format("%s/%s/%s/%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_TC_STRIPES, l2RuntimeInfo.getStripeName(), METRICS_FAMILY_TC_NODES, l2RuntimeInfo.getNodeName());

		//transactions stats
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "ObjectCounts", "Live"), NewRelicMetricType.Count, usageStats.getLiveObjectCount()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "ObjectCounts", "Cached"), NewRelicMetricType.Count, usageStats.getCachedObjectCount()));
		
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "OpsRates", "TieredStorage", "Heap", "Faults"), NewRelicMetricType.Rate, usageStats.getOnHeapFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "OpsRates", "TieredStorage", "Heap", "Flushes"), NewRelicMetricType.Rate, usageStats.getOnHeapFlushRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "OpsRates", "TieredStorage", "OffHeap", "Faults"), NewRelicMetricType.Rate, usageStats.getOffHeapFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "OpsRates", "TieredStorage", "OffHeap", "Flushes"), NewRelicMetricType.Rate, usageStats.getOffHeapFlushRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "OpsRates", "TieredStorage", "Disk", "Faults"), NewRelicMetricType.Rate, usageStats.getL2DiskFaultRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "OpsRates", "Transactions"), NewRelicMetricType.Rate, usageStats.getTransactionRate()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "OpsRates", "LockRecalls"), NewRelicMetricType.Rate, usageStats.getGlobalLockRecallRate()));
		
		//used space by tier
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "DataSizes", "TieredStorage", "Heap", "UsedSize"), NewRelicMetricType.Bytes, l2StatusInfo.getUsedHeap()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "DataSizes", "TieredStorage", "Heap", "MaxSize"), NewRelicMetricType.Bytes, l2StatusInfo.getMaxHeap()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "DataSizes", "TieredStorage", "OffHeap", "KeyMapSize"), NewRelicMetricType.Bytes, offheapStats.getOffheapMapAllocatedMemory()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "DataSizes", "TieredStorage", "OffHeap", "MaxSize"), NewRelicMetricType.Bytes, offheapStats.getOffheapMaxDataSize()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "DataSizes", "TieredStorage", "OffHeap", "ObjectSize"), NewRelicMetricType.Bytes, offheapStats.getOffheapObjectAllocatedMemory()));
		metrics.add(new Metric(String.format("%s/%s/%s/%s/%s/%s", l2NodesPrefix, METRICS_GROUPING_L2RUNTIME, "DataSizes", "TieredStorage", "OffHeap", "TotalUsedSize"), NewRelicMetricType.Bytes, offheapStats.getOffheapTotalAllocatedSize()));
		
		return metrics;
	}
}
