package com.terracotta.nrplugin.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.MetricDataset;
import com.terracotta.nrplugin.pojo.RatioMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/12/14
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class MetricUtil {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String PARAMETER_SHOW = "show";

	// Server metrics
	public static final String METRIC_LIVE_OBJECT_COUNT = "LiveObjectCount";
	public static final String METRIC_WRITE_OPERATION_RATE = "WriteOperationRate";
	public static final String METRIC_READ_OPERATION_RATE = "ReadOperationRate";
	public static final String METRIC_EVICTION_RATE = "EvictionRate";
	public static final String METRIC_EXPIRATION_RATE = "ExpirationRate";
	public static final String METRIC_OFFHEAP_USED_SIZE = "OffheapUsedSize";
	public static final String METRIC_OFFHEAP_MAX_SIZE = "OffheapMaxSize";

	// Client metrics
	public static final String METRIC_READ_RATE = "ReadRate";
	public static final String METRIC_WRITE_RATE = "WriteRate";

	// Cache metrics
	public static final String METRIC_EVICTED_COUNT = "EvictedCount";
	public static final String METRIC_EXPIRED_COUNT = "ExpiredCount";
	public static final String METRIC_CACHE_ON_DISK_HIT_RATE = "CacheOnDiskHitRate";
	public static final String METRIC_CACHE_IN_MEMORY_HIT_RATE = "CacheInMemoryHitRate";
	public static final String METRIC_CACHE_OFF_HEAP_HIT_RATE = "CacheOffHeapHitRate";
	public static final String METRIC_CACHE_HIT_RATE = "CacheHitRate";
	public static final String METRIC_ON_DISK_HIT_COUNT = "OnDiskHitCount";
	public static final String METRIC_IN_MEMORY_HIT_COUNT = "InMemoryHitCount";
	public static final String METRIC_OFF_HEAP_HIT_COUNT = "OffHeapHitCount";
	public static final String METRIC_CACHE_HIT_COUNT = "CacheHitCount";
	public static final String METRIC_ON_DISK_MISS_COUNT = "OnDiskMissCount";
	public static final String METRIC_IN_MEMORY_MISS_COUNT = "InMemoryMissCount";
	public static final String METRIC_OFF_HEAP_MISS_COUNT = "OffHeapMissCount";
	public static final String METRIC_CACHE_MISS_COUNT = "CacheMissCount";
	public static final String METRIC_PUT_COUNT = "PutCount";
	public static final String METRIC_REMOVED_COUNT = "RemovedCount";

	// these are the actual ehcache settings for local values
	public static final String METRIC_MAX_LOCAL_HEAP_SIZE_BYTES = "MaxBytesLocalHeap";
	public static final String METRIC_MAX_LOCAL_HEAP_SIZE_COUNT = "MaxEntriesLocalHeap";
	public static final String METRIC_MAX_LOCAL_OFFHEAP_SIZE_BYTES = "MaxBytesLocalOffHeap";
	public static final String METRIC_MAX_LOCAL_DISK_SIZE_COUNT = "MaxEntriesLocalDisk";
	public static final String METRIC_MAX_LOCAL_DISK_SIZE_BYTES = "MaxBytesLocalDisk";
	public static final String METRIC_MAX_TOTAL_SIZE_COUNT = "MaxEntriesInCache";

	//used counts
	public static final String METRIC_USED_LOCAL_HEAP_SIZE_COUNT = "LocalHeapSize";
	public static final String METRIC_USED_LOCAL_OFFHEAP_SIZE_COUNT = "LocalOffHeapSize";
	public static final String METRIC_USED_LOCAL_OFFHEAP_SIZE_BYTES = "LocalOffHeapSizeInBytes";
	public static final String METRIC_USED_LOCAL_DISK_SIZE_COUNT = "LocalDiskSize";
	public static final String METRIC_USED_LOCAL_DISK_SIZE_BYTES = "LocalDiskSizeInBytes";
	public static final String METRIC_USED_TOTAL_SIZE_COUNT = "Size";
	public static final String METRIC_ENABLED = "Enabled";

	// Cache ratio metrics
	public static final String METRIC_CACHE_HIT_RATIO = "CacheHitRatio";
	public static final String METRIC_ON_DISK_HIT_RATIO = "OnDiskHitRatio";
	public static final String METRIC_IN_MEMORY_HIT_RATIO = "InMemoryHitRatio";
	public static final String METRIC_OFF_HEAP_HIT_RATIO = "OffHeapHitRatio";
	public static final String METRIC_CACHE_MISS_RATIO = "CacheMissRatio";
	public static final String METRIC_ON_DISK_MISS_RATIO = "OnDiskMissRatio";
	public static final String METRIC_IN_MEMORY_MISS_RATIO = "InMemoryMissRatio";
	public static final String METRIC_OFF_HEAP_MISS_RATIO = "OffHeapMissRatio";

	// Topologies
	public static final String METRIC_NUM_CONNECTED_CLIENTS = "NumConnectedClients";
	public static final String METRIC_SERVER_STATE = "State";

	// NewRelic constants

	public static final String NEW_RELIC_PATH_SEPARATOR = "/";
	public static final String NEW_RELIC_MIN = "min";
	public static final String NEW_RELIC_MAX = "max";
	public static final String NEW_RELIC_TOTAL = "total";
	public static final String NEW_RELIC_COUNT = "count";
	public static final String NEW_RELIC_SUM_OF_SQUARES = "sum_of_squares";

	final List<String> cacheStatsNames = new ArrayList<String>();
	final List<Metric> metrics = new ArrayList<Metric>();

	// Base paths
	String tc = toMetricPath("Component", "Terracotta");
	final String servers = toMetricPath(tc, "Servers");
	final String clients = toMetricPath(tc, "Clients");
	final String ehcache = toMetricPath(tc, "Ehcache");
//    final Map<String, String> varReplaceMap = ImmutableMap.of(
//            CACHE_STATS_VARIABLE_CACHE_NAME_KEY, CACHE_STATS_VARIABLE_CACHE_NAME_VALUE,
//            CACHE_STATS_VARIABLE_CACHE_MANAGER_NAME_KEY, CACHE_STATS_VARIABLE_CACHE_MANAGER_NAME_VALUE
//    );

	@PostConstruct
	private void init() {
		// Server metrics
		metrics.add(constructServerMetric(METRIC_LIVE_OBJECT_COUNT, toMetricPath(servers, "Data", "Objects", METRIC_LIVE_OBJECT_COUNT), Metric.Unit.Count));
		metrics.add(constructServerMetric(METRIC_WRITE_OPERATION_RATE, toMetricPath(servers, "Data", "Rates", METRIC_WRITE_OPERATION_RATE), Metric.Unit.Rate));
		metrics.add(constructServerMetric(METRIC_READ_OPERATION_RATE, toMetricPath(servers, "Data", "Rates", METRIC_READ_OPERATION_RATE), Metric.Unit.Rate));
		metrics.add(constructServerMetric(METRIC_EVICTION_RATE, toMetricPath(servers, "Data", "Rates", METRIC_EVICTION_RATE), Metric.Unit.Rate));
		metrics.add(constructServerMetric(METRIC_EXPIRATION_RATE, toMetricPath(servers, "Data", "Rates", METRIC_EXPIRATION_RATE), Metric.Unit.Rate));
		metrics.add(constructServerMetric(METRIC_OFFHEAP_USED_SIZE, toMetricPath(servers, "OffHeap", "Bytes", METRIC_OFFHEAP_USED_SIZE), Metric.Unit.Bytes));
		metrics.add(constructServerMetric(METRIC_OFFHEAP_MAX_SIZE, toMetricPath(servers, "OffHeap", "Bytes", METRIC_OFFHEAP_MAX_SIZE), Metric.Unit.Bytes));

		// Client metrics
//        metrics.add(new Metric("$[?].statistics." + METRIC_READ_RATE,
//                toMetricPath(clients, METRIC_READ_RATE), Metric.Source.client, Metric.Unit.Rate));
//        metrics.add(new Metric("$[?].statistics." + METRIC_WRITE_RATE,
//                toMetricPath(clients, METRIC_WRITE_RATE), Metric.Source.client, Metric.Unit.Rate));

		// Cache metrics
		metrics.add(constructCacheMetric(METRIC_EVICTED_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric(METRIC_EXPIRED_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric(METRIC_CACHE_ON_DISK_HIT_RATE, Metric.Unit.Rate));
		metrics.add(constructCacheMetric(METRIC_CACHE_IN_MEMORY_HIT_RATE, Metric.Unit.Rate));
		metrics.add(constructCacheMetric(METRIC_CACHE_OFF_HEAP_HIT_RATE, Metric.Unit.Rate));
		metrics.add(constructCacheMetric(METRIC_CACHE_HIT_RATE, Metric.Unit.Rate));
		metrics.add(constructCacheMetric(METRIC_CACHE_HIT_COUNT, Metric.Unit.Count, Metric.Type.regular, Metric.RatioType.hit));
		metrics.add(constructCacheMetric(METRIC_ON_DISK_HIT_COUNT, Metric.Unit.Count, Metric.Type.regular, Metric.RatioType.hit));
		metrics.add(constructCacheMetric(METRIC_IN_MEMORY_HIT_COUNT, Metric.Unit.Count, Metric.Type.regular, Metric.RatioType.hit));
		metrics.add(constructCacheMetric(METRIC_OFF_HEAP_HIT_COUNT, Metric.Unit.Count, Metric.Type.regular, Metric.RatioType.hit));
		metrics.add(constructCacheMetric(METRIC_ON_DISK_MISS_COUNT, Metric.Unit.Count, Metric.Type.regular, Metric.RatioType.miss));
		metrics.add(constructCacheMetric(METRIC_IN_MEMORY_MISS_COUNT, Metric.Unit.Count, Metric.Type.regular, Metric.RatioType.miss));
		metrics.add(constructCacheMetric(METRIC_OFF_HEAP_MISS_COUNT, Metric.Unit.Count, Metric.Type.regular, Metric.RatioType.miss));
		metrics.add(constructCacheMetric(METRIC_CACHE_MISS_COUNT, Metric.Unit.Count, Metric.Type.regular, Metric.RatioType.miss));
		metrics.add(constructCacheMetric(METRIC_PUT_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric(METRIC_REMOVED_COUNT, Metric.Unit.Count));

		metrics.add(constructCacheMetric("LocalHeapEntries", METRIC_MAX_LOCAL_HEAP_SIZE_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric("LocalOffHeapSize", METRIC_MAX_LOCAL_OFFHEAP_SIZE_BYTES, Metric.Unit.Bytes));
		metrics.add(constructCacheMetric("LocalDiskEntries", METRIC_MAX_LOCAL_DISK_SIZE_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric("LocalDiskSize", METRIC_MAX_LOCAL_DISK_SIZE_BYTES, Metric.Unit.Bytes));
		metrics.add(constructCacheMetric("TotalEntries", METRIC_MAX_TOTAL_SIZE_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric("LocalHeapSize",METRIC_MAX_LOCAL_HEAP_SIZE_BYTES, Metric.Unit.Count));

		metrics.add(constructCacheMetric("LocalHeapEntries", METRIC_USED_LOCAL_HEAP_SIZE_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric("LocalOffHeapSize", METRIC_USED_LOCAL_OFFHEAP_SIZE_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric("LocalDiskEntries", METRIC_USED_LOCAL_OFFHEAP_SIZE_BYTES, Metric.Unit.Bytes));
		metrics.add(constructCacheMetric("LocalDiskSize", METRIC_USED_LOCAL_DISK_SIZE_BYTES, Metric.Unit.Bytes));
		metrics.add(constructCacheMetric("TotalEntries", METRIC_USED_TOTAL_SIZE_COUNT, Metric.Unit.Count));
		metrics.add(constructCacheMetric("LocalHeapSize", METRIC_USED_LOCAL_DISK_SIZE_COUNT, Metric.Unit.Count));

		// Cache Ratio metrics
		RatioMetric cacheHitRatio = constructRatioMetric(METRIC_CACHE_HIT_RATIO, null, METRIC_CACHE_HIT_COUNT, METRIC_CACHE_MISS_COUNT);
		RatioMetric onDiskHitRatio = constructRatioMetric(METRIC_ON_DISK_HIT_RATIO, null, METRIC_ON_DISK_HIT_COUNT, METRIC_ON_DISK_MISS_COUNT);
		RatioMetric inMemoryHitRatio = constructRatioMetric(METRIC_IN_MEMORY_HIT_RATIO, null, METRIC_IN_MEMORY_HIT_COUNT, METRIC_IN_MEMORY_MISS_COUNT);
		RatioMetric offHeapHitRatio = constructRatioMetric(METRIC_OFF_HEAP_HIT_RATIO, null, METRIC_OFF_HEAP_HIT_COUNT, METRIC_OFF_HEAP_MISS_COUNT);
		RatioMetric cacheMissRatio = constructRatioMetric(METRIC_CACHE_MISS_RATIO, cacheHitRatio, METRIC_CACHE_MISS_COUNT, METRIC_CACHE_HIT_COUNT);
		RatioMetric onDiskMissRatio = constructRatioMetric(METRIC_ON_DISK_MISS_RATIO, onDiskHitRatio, METRIC_ON_DISK_MISS_COUNT, METRIC_ON_DISK_HIT_COUNT);
		RatioMetric inMemoryMissRatio = constructRatioMetric(METRIC_IN_MEMORY_MISS_RATIO, inMemoryHitRatio, METRIC_IN_MEMORY_MISS_COUNT, METRIC_IN_MEMORY_HIT_COUNT);
		RatioMetric offHeapMissRatio = constructRatioMetric(METRIC_OFF_HEAP_MISS_RATIO, offHeapHitRatio, METRIC_OFF_HEAP_MISS_COUNT, METRIC_OFF_HEAP_HIT_COUNT);
		cacheHitRatio.setPair(cacheMissRatio);
		onDiskHitRatio.setPair(onDiskMissRatio);
		inMemoryHitRatio.setPair(inMemoryMissRatio);
		offHeapHitRatio.setPair(offHeapMissRatio);
		metrics.addAll(Arrays.asList(cacheHitRatio, onDiskHitRatio, inMemoryHitRatio, offHeapHitRatio,
				cacheMissRatio, onDiskMissRatio, inMemoryMissRatio, offHeapMissRatio));

		// Topologies metrics
//        metrics.add(new Metric(METRIC_NUM_CONNECTED_CLIENTS, "$.clientEntities", toMetricPath(
//		        clients, METRIC_NUM_CONNECTED_CLIENTS), Metric.Source.topologies, Metric.Unit.Count));

		// Special Metrics
		metrics.add(new Metric(METRIC_NUM_CONNECTED_CLIENTS, null, toMetricPath(servers, METRIC_NUM_CONNECTED_CLIENTS),
				Metric.Source.topologies, Metric.Unit.Count, Metric.Type.special, 1));
		metrics.add(new Metric(METRIC_SERVER_STATE, null, toMetricPath(servers, METRIC_SERVER_STATE),
				Metric.Source.topologies, Metric.Unit.Count, Metric.Type.special, 1));

		// Populate cache stat names list
		for (Metric metric : metrics) {
			if (Metric.Source.cache.equals(metric.getSource())) {
				cacheStatsNames.add(metric.getName());
			}
		}
	}

	private Metric constructServerMetric(String attribute, String reportedPath, Metric.Unit unit) {
		return new Metric(attribute, "$[?].statistics." + attribute, reportedPath, Metric.Source.server, unit);
	}

	private Metric constructCacheMetric(String attribute, Metric.Unit unit) {
		return constructCacheMetric(attribute, unit, null, null);
	}

	private Metric constructCacheMetric(String prefix, String attribute, Metric.Unit unit) {
		return constructCacheMetric(prefix, attribute, unit, null, null);
	}

	private Metric constructCacheMetric(String attribute, Metric.Unit unit, Metric.Type type, Metric.RatioType ratioType) {
		return new Metric(attribute, "$[?].attributes." + attribute, toMetricPath(ehcache, attribute),
				Metric.Source.cache, unit, type, ratioType);
	}

	private Metric constructCacheMetric(String prefix, String attribute, Metric.Unit unit, Metric.Type type,
	                                    Metric.RatioType ratioType) {
		return new Metric(attribute, "$[?].attributes." + attribute, toMetricPath(ehcache, prefix, attribute),
				Metric.Source.cache, unit, type, ratioType);
	}

	private RatioMetric constructRatioMetric(String attribute, RatioMetric pair, String numeratorCount,
	                                         String denominatorCount) {
		return new RatioMetric(attribute, toMetricPath(ehcache, attribute), Metric.Source.cache, Metric.Unit.CountSecond,
				pair, numeratorCount, denominatorCount);
	}

	public List<Metric> getMetrics() {
		return metrics;
	}

	public List<Metric> getRatioMetrics() {
		return doFilterMetrics(Metric.Type.ratio);
	}

	public List<Metric> getRegularMetrics() {
		return doFilterMetrics(Metric.Type.regular);
	}

	public List<Metric> getSpecialMetrics() {
		return doFilterMetrics(Metric.Type.special);
	}

	private List<Metric> doFilterMetrics(Metric.Type type) {
		List<Metric> includedMetrics = new ArrayList<Metric>();
		for (Metric metric : metrics) {
			if (metric.getType().equals(type)) {
				includedMetrics.add(metric);
			}
		}
		return includedMetrics;
	}

	public List<String> getCacheStatsNames() {
		return cacheStatsNames;
	}

	public Map<String, Object> metricsAsJson(Collection<MetricDataset> metrics) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (MetricDataset metricDataset : metrics) {
			try {
				Map.Entry<String, Map<String, Number>> entry = metricAsJson(metricDataset);
				log.trace("Deserializing " + entry.getKey());
				map.put(entry.getKey(), entry.getValue());
			} catch (Exception e) {
				log.error("Error marshalling metric to JSON.", e);
			}
		}
		return map;
	}

	public Map.Entry<String, Map<String, Number>> metricAsJson(MetricDataset metricDataset)
			throws JsonProcessingException {
		Map<String, Number> values = new HashMap<String, Number>();
		values.put(NEW_RELIC_MIN, metricDataset.getStatistics().getMin());
		values.put(NEW_RELIC_MAX, metricDataset.getStatistics().getMax());
		values.put(NEW_RELIC_TOTAL, metricDataset.getStatistics().getSum());
		values.put(NEW_RELIC_COUNT, metricDataset.getStatistics().getN());
		values.put(NEW_RELIC_SUM_OF_SQUARES, metricDataset.getStatistics().getSumsq());
		return new AbstractMap.SimpleEntry<String, Map<String, Number>>(metricDataset.getReportingPath(), values);
	}

	public String toMetricPath(String... values) {
		String path = "";
		Iterator<String> i = Arrays.asList(values).iterator();
		while (i.hasNext()) {
			path += i.next();
			if (i.hasNext()) path += NEW_RELIC_PATH_SEPARATOR;
		}
		return path;
	}

	public int toStateCode(String stateString) {
		if (stateString.startsWith("ACTIVE")) return 0;
		else if (stateString.startsWith("PASSIVE")) return 2;
		else if (stateString.startsWith("INITIALIZING")) return 4;
		else return 8;
	}

}
