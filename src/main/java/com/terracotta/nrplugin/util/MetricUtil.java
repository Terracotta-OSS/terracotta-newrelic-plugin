package com.terracotta.nrplugin.util;

import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.MetricBuilder;
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

	// used counts
	public static final String METRIC_USED_LOCAL_HEAP_SIZE_COUNT = "LocalHeapSize";
	public static final String METRIC_USED_LOCAL_HEAP_SIZE_BYTES = "LocalHeapSizeInBytes";
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
	public static final String NEW_RELIC_MIN = "min";
	public static final String NEW_RELIC_MAX = "max";
	public static final String NEW_RELIC_TOTAL = "total";
	public static final String NEW_RELIC_COUNT = "count";
	public static final String NEW_RELIC_SUM_OF_SQUARES = "sum_of_squares";
	public static final String NEW_RELIC_PATH_SEPARATOR = "/";

	final List<String> cacheStatsNames = new ArrayList<String>();
	final List<Metric> metrics = new ArrayList<Metric>();

	// Base paths
	final String cm = "Component";
	final String tc = "Terracotta";
	final String srv = "Servers";
	final String clnt = "Clients";
	final String eh = "Ehcache";
	final String data = "Data";
	final String off = "OffHeap";
	final String obj = "Objects";
	final String rates = "Rates";
	final String bytes = "Bytes";

//    final Map<String, String> varReplaceMap = ImmutableMap.of(
//            CACHE_STATS_VARIABLE_CACHE_NAME_KEY, CACHE_STATS_VARIABLE_CACHE_NAME_VALUE,
//            CACHE_STATS_VARIABLE_CACHE_MANAGER_NAME_KEY, CACHE_STATS_VARIABLE_CACHE_MANAGER_NAME_VALUE
//    );

	@PostConstruct
	private void init() {
		// Server metrics
		addServerMetric(METRIC_LIVE_OBJECT_COUNT, Metric.Unit.Count, data, obj);
//		addServerMetric(METRIC_WRITE_OPERATION_RATE, Metric.Unit.Count, data, rates);
//		addServerMetric(METRIC_READ_OPERATION_RATE, Metric.Unit.Count, data, rates);
//		addServerMetric(METRIC_EVICTION_RATE, Metric.Unit.Count, data, rates);
//		addServerMetric(METRIC_EXPIRATION_RATE, Metric.Unit.Count,data, rates);
		addServerMetric(METRIC_OFFHEAP_USED_SIZE, Metric.Unit.Bytes, off, bytes);
		addServerMetric(METRIC_OFFHEAP_MAX_SIZE, Metric.Unit.Bytes, off, bytes);

		// Client metrics
//        metrics.add(new Metric("$[?].statistics." + METRIC_READ_RATE,
//                toMetricPath(clients, METRIC_READ_RATE), Metric.Source.client, Metric.Unit.Rate));
//        metrics.add(new Metric("$[?].statistics." + METRIC_WRITE_RATE,
//                toMetricPath(clients, METRIC_WRITE_RATE), Metric.Source.client, Metric.Unit.Rate));

		// Cache metrics
//		addCacheMetric(METRIC_CACHE_ON_DISK_HIT_RATE, Metric.Unit.Rate);
//		addCacheMetric(METRIC_CACHE_IN_MEMORY_HIT_RATE, Metric.Unit.Rate);
//		addCacheMetric(METRIC_CACHE_OFF_HEAP_HIT_RATE, Metric.Unit.Rate);
//		addCacheMetric(METRIC_CACHE_HIT_RATE, Metric.Unit.Rate);
		addCacheMetric(METRIC_PUT_COUNT, Metric.Unit.Count, false);
		addCacheMetric(METRIC_REMOVED_COUNT, Metric.Unit.Count, false);
		addCacheMetric(METRIC_EXPIRED_COUNT, Metric.Unit.Count, false);
		addCacheMetric(METRIC_EVICTED_COUNT, Metric.Unit.Count, false);
		addCacheMetric(METRIC_CACHE_HIT_COUNT, Metric.Unit.Count, false, Metric.RatioType.hit);
		addCacheMetric(METRIC_CACHE_MISS_COUNT, Metric.Unit.Count, false, Metric.RatioType.miss);
		addCacheMetric(METRIC_ON_DISK_HIT_COUNT, Metric.Unit.Count, false, Metric.RatioType.hit);
		addCacheMetric(METRIC_ON_DISK_MISS_COUNT, Metric.Unit.Count, false, Metric.RatioType.miss);
		addCacheMetric(METRIC_IN_MEMORY_HIT_COUNT, Metric.Unit.Count, false, Metric.RatioType.hit);
		addCacheMetric(METRIC_IN_MEMORY_MISS_COUNT, Metric.Unit.Count, false, Metric.RatioType.miss);
		addCacheMetric(METRIC_OFF_HEAP_HIT_COUNT, Metric.Unit.Count, false, Metric.RatioType.hit);
		addCacheMetric(METRIC_OFF_HEAP_MISS_COUNT, Metric.Unit.Count, false, Metric.RatioType.miss);

		addCacheMetric(METRIC_MAX_LOCAL_HEAP_SIZE_COUNT, "Max", Metric.Unit.Count, Arrays.asList("LocalHeapEntries"));
		addCacheMetric(METRIC_USED_LOCAL_HEAP_SIZE_COUNT, "Used", Metric.Unit.Count, Arrays.asList("LocalHeapEntries"));
		addCacheMetric(METRIC_MAX_LOCAL_HEAP_SIZE_BYTES, "Max", Metric.Unit.Bytes, Arrays.asList("LocalHeapSize"));
		addCacheMetric(METRIC_USED_LOCAL_HEAP_SIZE_BYTES, "Used", Metric.Unit.Bytes, Arrays.asList("LocalHeapSize"));

		addCacheMetric(METRIC_USED_LOCAL_OFFHEAP_SIZE_COUNT, "Used", Metric.Unit.Count, Arrays.asList("LocalOffHeapEntries"));
		addCacheMetric(METRIC_MAX_LOCAL_OFFHEAP_SIZE_BYTES, "Max", Metric.Unit.Bytes, Arrays.asList("LocalOffHeapSize"));
		addCacheMetric(METRIC_USED_LOCAL_OFFHEAP_SIZE_BYTES, "Used", Metric.Unit.Bytes, Arrays.asList("LocalOffHeapSize"));

		addCacheMetric(METRIC_USED_LOCAL_DISK_SIZE_BYTES, "Used", Metric.Unit.Bytes, Arrays.asList("LocalDiskSize"));
		addCacheMetric(METRIC_MAX_LOCAL_DISK_SIZE_BYTES, "Max", Metric.Unit.Bytes, Arrays.asList("LocalDiskSize"));
		addCacheMetric(METRIC_MAX_LOCAL_DISK_SIZE_COUNT, "Max", Metric.Unit.Count, Arrays.asList("LocalDiskEntries"));
		addCacheMetric(METRIC_USED_LOCAL_DISK_SIZE_COUNT, "Used", Metric.Unit.Count, Arrays.asList("LocalDiskEntries"));

		addCacheMetric(METRIC_MAX_TOTAL_SIZE_COUNT, "Max", Metric.Unit.Count, Arrays.asList("TotalEntries"));
		addCacheMetric(METRIC_USED_TOTAL_SIZE_COUNT, "Used", Metric.Unit.Count, Arrays.asList("TotalEntries"));

		// Cache Ratio metrics
		RatioMetric cacheHitRatio = addRatioMetric(METRIC_CACHE_HIT_RATIO, null, METRIC_CACHE_HIT_COUNT, METRIC_CACHE_MISS_COUNT);
		RatioMetric onDiskHitRatio = addRatioMetric(METRIC_ON_DISK_HIT_RATIO, null, METRIC_ON_DISK_HIT_COUNT, METRIC_ON_DISK_MISS_COUNT);
		RatioMetric inMemoryHitRatio = addRatioMetric(METRIC_IN_MEMORY_HIT_RATIO, null, METRIC_IN_MEMORY_HIT_COUNT, METRIC_IN_MEMORY_MISS_COUNT);
		RatioMetric offHeapHitRatio = addRatioMetric(METRIC_OFF_HEAP_HIT_RATIO, null, METRIC_OFF_HEAP_HIT_COUNT, METRIC_OFF_HEAP_MISS_COUNT);
		RatioMetric cacheMissRatio = addRatioMetric(METRIC_CACHE_MISS_RATIO, cacheHitRatio, METRIC_CACHE_MISS_COUNT, METRIC_CACHE_HIT_COUNT);
		RatioMetric onDiskMissRatio = addRatioMetric(METRIC_ON_DISK_MISS_RATIO, onDiskHitRatio, METRIC_ON_DISK_MISS_COUNT, METRIC_ON_DISK_HIT_COUNT);
		RatioMetric inMemoryMissRatio = addRatioMetric(METRIC_IN_MEMORY_MISS_RATIO, inMemoryHitRatio, METRIC_IN_MEMORY_MISS_COUNT, METRIC_IN_MEMORY_HIT_COUNT);
		RatioMetric offHeapMissRatio = addRatioMetric(METRIC_OFF_HEAP_MISS_RATIO, offHeapHitRatio, METRIC_OFF_HEAP_MISS_COUNT, METRIC_OFF_HEAP_HIT_COUNT);

		cacheHitRatio.setPair(cacheMissRatio);
		onDiskHitRatio.setPair(onDiskMissRatio);
		inMemoryHitRatio.setPair(inMemoryMissRatio);
		offHeapHitRatio.setPair(offHeapMissRatio);

		// Special Metrics
		addMetric(METRIC_NUM_CONNECTED_CLIENTS, null, Metric.Type.special, Metric.Source.topologies, Metric.Unit.Count,
				false, Arrays.asList(cm, tc, srv), null, 1);
		addMetric(METRIC_SERVER_STATE, null, Metric.Type.special, Metric.Source.topologies, Metric.Unit.Count,
				false, Arrays.asList(cm, tc, srv), null, 1);

		// Populate cache stat names list
		for (Metric metric : metrics) {
			if (Metric.Source.cache.equals(metric.getSource())) {
				cacheStatsNames.add(metric.getName());
			}
		}
	}

	private void addServerMetric(String name, Metric.Unit unit, String... suffix) {
		List<String> reportingPathComponents = new ArrayList<String>(Arrays.asList(cm, tc, srv));
		reportingPathComponents.addAll(Arrays.asList(suffix));
		addMetric(name, null, null, Metric.Source.server, unit, false, reportingPathComponents, null, null);
	}

	private void addCacheMetric(String name, Metric.Unit unit, boolean createDiff) {
		addCacheMetric(name, null, null, unit, createDiff, null, null);
	}

	private void addCacheMetric(String name, Metric.Unit unit, boolean createDiff, Metric.RatioType ratioType) {
		addCacheMetric(name, null, null, unit, createDiff, null, ratioType);
	}

	private void addCacheMetric(String name, String displayName, Metric.Unit unit, List<String> suffix) {
		addCacheMetric(name, displayName, null, unit, false, suffix, null);
	}

	private void addCacheMetric(String name, String displayName, Metric.Type type, Metric.Unit unit, boolean createDiff,
	                            List<String> suffix, Metric.RatioType ratioType) {
		List<String> reportingPathComponents = new ArrayList<String>(Arrays.asList(cm, tc, eh));
		if (suffix != null && !suffix.isEmpty()) reportingPathComponents.addAll(suffix);
		addMetric(name, displayName, type, Metric.Source.cache, unit, createDiff, reportingPathComponents, ratioType, null);
	}

	private void addMetric(String name, String displayName, Metric.Type type, Metric.Source source, Metric.Unit unit,
	                       boolean createDiff, List<String> reportingPathComponents, Metric.RatioType ratioType,
	                       Integer maxWindowSize) {
		Metric metric = MetricBuilder.create(name).
				setDisplayName(displayName).
				setSource(source).
				setUnit(unit).
				setCreateDiff(createDiff).
				setType(type).
				setRatioType(ratioType).
				setMaxWindowSize(maxWindowSize).
				addReportingPath(reportingPathComponents).
				build();
		metrics.add(metric);
	}

	private RatioMetric addRatioMetric(String name, RatioMetric pair, String numeratorCount, String denominatorCount) {
		RatioMetric metric = (RatioMetric) MetricBuilder.create(name).
				setType(Metric.Type.ratio).
				setSource(Metric.Source.cache).
				setUnit(Metric.Unit.Percent).
				setPair(pair).
				setNumeratorCount(numeratorCount).
				setDenominatorCount(denominatorCount).
				addReportingPath(new ArrayList<String>(Arrays.asList(cm, tc, eh))).
				build();
		metrics.add(metric);
		return metric;
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

	public Map.Entry<String, Map<String, Number>> metricAsJson(MetricDataset metricDataset) {
		return metricAsJson(metricDataset.getMetric().getReportingPath(), metricDataset.getStatistics().getMin(),
				metricDataset.getStatistics().getMax(), metricDataset.getStatistics().getSum(),
				metricDataset.getStatistics().getN(), metricDataset.getStatistics().getSumsq());
	}

	public Map.Entry<String, Map<String, Number>> metricAsJson(String path, double min, double max, double sum,
	                                                           long count, double sumsq) {
		Map<String, Number> values = new HashMap<String, Number>();
		values.put(NEW_RELIC_MIN, min);
		values.put(NEW_RELIC_MAX, max);
		values.put(NEW_RELIC_TOTAL, sum);
		values.put(NEW_RELIC_COUNT, count);
		values.put(NEW_RELIC_SUM_OF_SQUARES, sumsq);
		return new AbstractMap.SimpleEntry<String, Map<String, Number>>(path, values);
	}

	public int toStateCode(String stateString) {
		if (stateString.startsWith("ACTIVE")) return 0;
		else if (stateString.startsWith("PASSIVE")) return 2;
		else if (stateString.startsWith("INITIALIZING")) return 4;
		else return 8;
	}

}
