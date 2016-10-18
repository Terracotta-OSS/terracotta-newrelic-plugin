package com.terracotta.nrplugin.util;

import com.terracotta.nrplugin.pojo.*;
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
    public static final String METRIC_DATA_USED_SIZE = "StorageStats.DATA.used";
    public static final String METRIC_DATA_MAX_SIZE = "StorageStats.DATA.max";
    public static final String METRIC_OFFHEAP_USED_SIZE = "StorageStats.OFFHEAP.used";
    public static final String METRIC_OFFHEAP_MAX_SIZE = "StorageStats.OFFHEAP.max";

    public static final String METRIC_DISPLAYNAME_DATA_USED_SIZE = "Used";
    public static final String METRIC_DISPLAYNAME_DATA_MAX_SIZE = "Max";
    public static final String METRIC_DISPLAYNAME_OFFHEAP_USED_SIZE = "Used";
    public static final String METRIC_DISPLAYNAME_OFFHEAP_MAX_SIZE = "Max";


    // Client metrics
    public static final String METRIC_READ_RATE = "ReadRate";
    public static final String METRIC_WRITE_RATE = "WriteRate";

    // Cache metrics
    public static final String METRIC_EVICTED_COUNT = "EvictedCount";
    public static final String METRIC_EXPIRED_COUNT = "ExpiredCount";
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

    // Cache Rates
    public static final String METRIC_CACHE_ON_DISK_HIT_RATE = "CacheOnDiskHitRate";
    public static final String METRIC_CACHE_IN_MEMORY_HIT_RATE = "CacheInMemoryHitRate";
    public static final String METRIC_CACHE_OFF_HEAP_HIT_RATE = "CacheOffHeapHitRate";
    public static final String METRIC_CACHE_HIT_RATE = "CacheHitRate";
    public static final String METRIC_CACHE_ON_DISK_MISS_RATE = "CacheOnDiskMissRate";
    public static final String METRIC_CACHE_IN_MEMORY_MISS_RATE = "CacheInMemoryMissRate";
    public static final String METRIC_CACHE_OFF_HEAP_MISS_RATE = "CacheOffHeapMissRate";
    public static final String METRIC_CACHE_MISS_RATE = "CacheMissRate";
    public static final String METRIC_CACHE_PUT_RATE = "CachePutRate";
    public static final String METRIC_CACHE_REMOVE_RATE = "CacheRemoveRate";
    public static final String METRIC_CACHE_EVICTION_RATE = "CacheEvictionRate";
    public static final String METRIC_CACHE_EXPIRATION_RATE = "CacheExpirationRate";


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
    public static final String METRIC_CONNECTED_CLIENTS_GROUP = "ConnectedClients";
    public static final String METRIC_CONNECTED_CLIENTS_TOTAL = "ConnectedClientsTotal";
    public static final String METRIC_CONNECTED_CLIENTS_UNIQUE_HOSTS = "ConnectedClientsUniqueHosts";
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
    final String storage = "Storage";
    final String usage = "Usage";
    final String ratio = "Ratio";
    final String off = "OffHeap";
    final String obj = "Objects";
    //	final String rates = "Rates";
    final String bytes = "Bytes";
    final String reads = "Reads";
    final String writes = "Writes";
    final String hits = "Hits";
    final String miss = "Miss";
    final String total = "Total";
    final String max = "Max";
    final String used = "Used";

    @PostConstruct
    private void init() {
        // Server metrics
        addServerMetric(METRIC_WRITE_OPERATION_RATE, null, Metric.Unit.Rate, Arrays.asList(usage, "ReadWrites", total));
        addServerMetric(METRIC_READ_OPERATION_RATE, null, Metric.Unit.Rate, Arrays.asList(usage, "ReadWrites", total));
        addServerMetric(METRIC_EVICTION_RATE, null, Metric.Unit.Rate, Arrays.asList(usage, "Evictions", total));
        addServerMetric(METRIC_EXPIRATION_RATE, null, Metric.Unit.Rate, Arrays.asList(usage, "Expirations", total));
        addServerMetric(METRIC_LIVE_OBJECT_COUNT, null, Metric.Unit.Count, Arrays.asList(storage, "Internal", total));
        addServerMetric(METRIC_DATA_USED_SIZE, METRIC_DISPLAYNAME_DATA_USED_SIZE, Metric.Unit.Bytes, Arrays.asList(storage, data));
        addServerMetric(METRIC_DATA_MAX_SIZE, METRIC_DISPLAYNAME_DATA_MAX_SIZE, Metric.Unit.Bytes, Arrays.asList(storage, data));
        addServerMetric(METRIC_OFFHEAP_USED_SIZE, METRIC_DISPLAYNAME_OFFHEAP_USED_SIZE, Metric.Unit.Bytes, Arrays.asList(storage, off));
        addServerMetric(METRIC_OFFHEAP_MAX_SIZE, METRIC_DISPLAYNAME_OFFHEAP_MAX_SIZE, Metric.Unit.Bytes, Arrays.asList(storage, off));

        // Client metrics - not used at this point...
//        metrics.add(new Metric("$[?].statistics." + METRIC_READ_RATE,
//                toMetricPath(clients, METRIC_READ_RATE), Metric.Source.client, Metric.Unit.Rate));
//        metrics.add(new Metric("$[?].statistics." + METRIC_WRITE_RATE,
//                toMetricPath(clients, METRIC_WRITE_RATE), Metric.Source.client, Metric.Unit.Rate));

        // Cache rate metrics -- not good to use - better to create diffs and check the rates
//		addCacheMetric(METRIC_CACHE_ON_DISK_HIT_RATE, Metric.Unit.Rate, false);
//		addCacheMetric(METRIC_CACHE_IN_MEMORY_HIT_RATE, Metric.Unit.Rate, false);
//		addCacheMetric(METRIC_CACHE_OFF_HEAP_HIT_RATE, Metric.Unit.Rate, false);
//		addCacheMetric(METRIC_CACHE_HIT_RATE, Metric.Unit.Rate, false);
//		addCacheMetric(METRIC_CACHE_MISS_RATE, Metric.Unit.Rate, false);
//		addCacheMetric(METRIC_CACHE_PUT_RATE, Metric.Unit.Rate, false);
//		addCacheMetric(METRIC_CACHE_REMOVE_RATE, Metric.Unit.Rate, false);
//		addCacheMetric(METRIC_CACHE_EVICTION_RATE, Metric.Unit.Rate, false);
//		addCacheMetric(METRIC_CACHE_EXPIRATION_RATE, Metric.Unit.Rate, false);

        addCacheMetric(METRIC_PUT_COUNT, "Puts", Metric.Unit.Count, Arrays.asList(usage, writes, total), true);
        addCacheMetric(METRIC_REMOVED_COUNT, "Removals", Metric.Unit.Count, Arrays.asList(usage, writes, total), true);
        addCacheMetric(METRIC_EXPIRED_COUNT, "Expirations", Metric.Unit.Count, Arrays.asList(usage, writes, total), true);
        addCacheMetric(METRIC_EVICTED_COUNT, "Evictions", Metric.Unit.Count, Arrays.asList(usage, writes, total), true);
        Metric cacheHitCount = addCacheMetric(METRIC_CACHE_HIT_COUNT, hits, Metric.Unit.Count, Arrays.asList(usage, reads, total), true, Metric.RatioType.hit);
        Metric cacheMissCount = addCacheMetric(METRIC_CACHE_MISS_COUNT, miss, Metric.Unit.Count, Arrays.asList(usage, reads, total), true, Metric.RatioType.miss);
        Metric cacheOnDiskHitCount = addCacheMetric(METRIC_ON_DISK_HIT_COUNT, hits, Metric.Unit.Count, Arrays.asList(usage, reads, "LocalDisk"), true, Metric.RatioType.hit);
        Metric cacheOnDiskMissCount = addCacheMetric(METRIC_ON_DISK_MISS_COUNT, miss, Metric.Unit.Count, Arrays.asList(usage, reads, "LocalDisk"), true, Metric.RatioType.miss);
        Metric cacheInMemoryHitCount = addCacheMetric(METRIC_IN_MEMORY_HIT_COUNT, hits, Metric.Unit.Count, Arrays.asList(usage, reads, "LocalHeap"), true, Metric.RatioType.hit);
        Metric cacheInMemoryMissCount = addCacheMetric(METRIC_IN_MEMORY_MISS_COUNT, miss, Metric.Unit.Count, Arrays.asList(usage, reads, "LocalHeap"), true, Metric.RatioType.miss);
        Metric cacheOffheapHitCount = addCacheMetric(METRIC_OFF_HEAP_HIT_COUNT, hits, Metric.Unit.Count, Arrays.asList(usage, reads, "LocalOffHeap"), true, Metric.RatioType.hit);
        Metric cacheOffheapMissCount = addCacheMetric(METRIC_OFF_HEAP_MISS_COUNT, miss, Metric.Unit.Count, Arrays.asList(usage, reads, "LocalOffHeap"), true, Metric.RatioType.miss);

        addCacheMetric(METRIC_MAX_LOCAL_HEAP_SIZE_COUNT, max, Metric.Unit.Count, Arrays.asList(storage, "LocalHeap", "Entries"), false);
        addCacheMetric(METRIC_USED_LOCAL_HEAP_SIZE_COUNT, used, Metric.Unit.Count, Arrays.asList(storage, "LocalHeap", "Entries"), false);
        addCacheMetric(METRIC_MAX_LOCAL_HEAP_SIZE_BYTES, max, Metric.Unit.Bytes, Arrays.asList(storage, "LocalHeap", "Bytes"), false);
        addCacheMetric(METRIC_USED_LOCAL_HEAP_SIZE_BYTES, used, Metric.Unit.Bytes, Arrays.asList(storage, "LocalHeap", "Bytes"), false);

        addCacheMetric(METRIC_USED_LOCAL_OFFHEAP_SIZE_COUNT, used, Metric.Unit.Count, Arrays.asList(storage, "LocalOffHeap", "Entries"), false);
        addCacheMetric(METRIC_MAX_LOCAL_OFFHEAP_SIZE_BYTES, max, Metric.Unit.Bytes, Arrays.asList(storage, "LocalOffHeap", "Bytes"), false);
        addCacheMetric(METRIC_USED_LOCAL_OFFHEAP_SIZE_BYTES, used, Metric.Unit.Bytes, Arrays.asList(storage, "LocalOffHeap", "Bytes"), false);

        addCacheMetric(METRIC_USED_LOCAL_DISK_SIZE_BYTES, used, Metric.Unit.Bytes, Arrays.asList(storage, "LocalDisk", "Bytes"), false);
        addCacheMetric(METRIC_MAX_LOCAL_DISK_SIZE_BYTES, max, Metric.Unit.Bytes, Arrays.asList(storage, "LocalDisk", "Bytes"), false);
        addCacheMetric(METRIC_MAX_LOCAL_DISK_SIZE_COUNT, max, Metric.Unit.Count, Arrays.asList(storage, "LocalDisk", "Entries"), false);
        addCacheMetric(METRIC_USED_LOCAL_DISK_SIZE_COUNT, used, Metric.Unit.Count, Arrays.asList(storage, "LocalDisk", "Entries"), false);

        addCacheMetric(METRIC_MAX_TOTAL_SIZE_COUNT, max, Metric.Unit.Count, Arrays.asList(storage, total, "Entries"), false);
        addCacheMetric(METRIC_USED_TOTAL_SIZE_COUNT, used, Metric.Unit.Count, Arrays.asList(storage, total, "Entries"), false);

        // Cache Ratio metrics
        RatioMetric cacheHitRatio = addCacheRatioMetric(METRIC_CACHE_HIT_RATIO, hits, Arrays.asList(usage, ratio, total), cacheHitCount, cacheMissCount, Metric.RollupType.sum);
        RatioMetric onDiskHitRatio = addCacheRatioMetric(METRIC_ON_DISK_HIT_RATIO, hits, Arrays.asList(usage, ratio, "LocalDisk"), cacheOnDiskHitCount, cacheOnDiskMissCount, Metric.RollupType.sum);
        RatioMetric inMemoryHitRatio = addCacheRatioMetric(METRIC_IN_MEMORY_HIT_RATIO, hits, Arrays.asList(usage, ratio, "LocalHeap"), cacheInMemoryHitCount, cacheInMemoryMissCount, Metric.RollupType.sum);
        RatioMetric offHeapHitRatio = addCacheRatioMetric(METRIC_OFF_HEAP_HIT_RATIO, hits, Arrays.asList(usage, ratio, "LocalOffHeap"), cacheOffheapHitCount, cacheOffheapMissCount, Metric.RollupType.sum);
        RatioMetric cacheMissRatio = addCacheRatioMetric(METRIC_CACHE_MISS_RATIO, miss, Arrays.asList(usage, ratio, total), cacheMissCount, cacheHitCount, Metric.RollupType.sum);
        RatioMetric onDiskMissRatio = addCacheRatioMetric(METRIC_ON_DISK_MISS_RATIO, miss, Arrays.asList(usage, ratio, "LocalDisk"), cacheOnDiskMissCount, cacheOnDiskHitCount, Metric.RollupType.sum);
        RatioMetric inMemoryMissRatio = addCacheRatioMetric(METRIC_IN_MEMORY_MISS_RATIO, miss, Arrays.asList(usage, ratio, "LocalHeap"), cacheInMemoryMissCount, cacheInMemoryHitCount, Metric.RollupType.sum);
        RatioMetric offHeapMissRatio = addCacheRatioMetric(METRIC_OFF_HEAP_MISS_RATIO, miss, Arrays.asList(usage, ratio, "LocalOffHeap"), cacheOffheapMissCount, cacheOffheapHitCount, Metric.RollupType.sum);

        // Special Metrics
        addMetric(METRIC_CONNECTED_CLIENTS_TOTAL, total, null, Metric.Type.special, Metric.Source.topologies, Metric.Unit.Count,
                false, Arrays.asList(cm, tc, srv, METRIC_CONNECTED_CLIENTS_GROUP), null, 1, Metric.RollupType.none);
        addMetric(METRIC_CONNECTED_CLIENTS_UNIQUE_HOSTS, "UniqueHosts", null, Metric.Type.special, Metric.Source.topologies, Metric.Unit.Count,
                false, Arrays.asList(cm, tc, srv, METRIC_CONNECTED_CLIENTS_GROUP), null, 1, Metric.RollupType.none);

        //add each server state in its own metric
        for (MetricDatasetServerComponent.State state : MetricDatasetServerComponent.State.values()) {
            if (state != MetricDatasetServerComponent.State.UNKNOWN) {
                addMetric(state.getName(), null, null, Metric.Type.special, Metric.Source.topologies, Metric.Unit.Count,
                        false, Arrays.asList(cm, tc, srv, METRIC_SERVER_STATE), null, 1, Metric.RollupType.sum);
            }
        }

        // Populate cache stat names list
        for (Metric metric : metrics) {
            if (Metric.Source.cache.equals(metric.getSource())) {
                cacheStatsNames.add(metric.getName());
            }
        }
    }

    private Metric addServerMetric(String name, String displayName, Metric.Unit unit, List<String> suffix) {
        List<String> reportingPathComponents = new ArrayList<String>(Arrays.asList(cm, tc, srv));

        if (null != suffix)
            reportingPathComponents.addAll(suffix);

        return addMetric(name, displayName, null, null, Metric.Source.server, unit, false, reportingPathComponents, null, null, Metric.RollupType.sum);
    }

    private Metric addCacheMetric(String name, Metric.Unit unit, List<String> reportingPathSuffix, boolean createDiff) {
        return addCacheMetric(name, null, null, unit, createDiff, reportingPathSuffix, null);
    }

    private Metric addCacheMetric(String name, String displayName, Metric.Unit unit, List<String> reportingPathSuffix, boolean createDiff, Metric.RatioType ratioType) {
        return addCacheMetric(name, displayName, null, unit, createDiff, reportingPathSuffix, ratioType);
    }

    private Metric addCacheMetric(String name, String displayName, Metric.Unit unit, List<String> reportingPathSuffix, boolean createDiff) {
        return addCacheMetric(name, displayName, null, unit, createDiff, reportingPathSuffix, null);
    }

    private Metric addCacheMetric(String name, String displayName, Metric.Type type, Metric.Unit unit, boolean createDiff,
                                  List<String> reportingPathSuffix, Metric.RatioType ratioType) {
        List<String> reportingPathComponents = new ArrayList<String>(Arrays.asList(cm, tc, eh));
        if (reportingPathSuffix != null && !reportingPathSuffix.isEmpty())
            reportingPathComponents.addAll(reportingPathSuffix);
        return addMetric(name, displayName, null, type, Metric.Source.cache, unit, createDiff, reportingPathComponents, ratioType, null, Metric.RollupType.sum);
    }

    private Metric addMetric(String name, String displayName, String dataPath, Metric.Type type, Metric.Source source, Metric.Unit unit,
                             boolean createDiff, List<String> reportingPathComponents, Metric.RatioType ratioType,
                             Integer maxWindowSize, Metric.RollupType rollupType) {
        Metric metric = MetricBuilder.create(name).
                setDisplayName(displayName).
                setSource(source).
                setDataPath(dataPath).
                setUnit(unit).
                setCreateDiff(createDiff).
                setType(type).
                setRatioType(ratioType).
                setRollupType(rollupType).
                setMaxWindowSize(maxWindowSize).
                addReportingPath(reportingPathComponents).
                build();
        metrics.add(metric);

        return metric;
    }

    private RatioMetric addCacheRatioMetric(String name, String displayName, List<String> reportingPathSuffix, Metric numeratorMetric, Metric denominatorMetric, Metric.RollupType rollupType) {
        List<String> reportingPathComponents = new ArrayList<String>(Arrays.asList(cm, tc, eh));
        if (reportingPathSuffix != null && !reportingPathSuffix.isEmpty())
            reportingPathComponents.addAll(reportingPathSuffix);

        RatioMetric metric = (RatioMetric) MetricBuilder.create(name).
                setDisplayName(displayName).
                setType(Metric.Type.ratio).
                setRollupType(rollupType).
                setSource(Metric.Source.cache).
                setUnit(Metric.Unit.Percent).
                setNumeratorMetric(numeratorMetric).
                setDenominatorMetric(denominatorMetric).
                addReportingPath(reportingPathComponents).
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

//	public Map<String, Object> metricsAsJson(Collection<MetricDataset> metrics) {
//		Map<String, Object> map = new HashMap<String, Object>();
//		for (MetricDataset metricDataset : metrics) {
//			try {
//				Map.Entry<String, Map<String, Number>> entry = metricAsJson(metricDataset);
//				log.trace("Deserializing " + entry.getKey());
//				map.put(entry.getKey(), entry.getValue());
//			} catch (Exception e) {
//				log.error("Error marshalling metric to JSON.", e);
//			}
//		}
//		return map;
//	}

    //returns a map with key = metric reporting path, and value = count, min, max, sum, sumsq, absolute
    public Map.Entry<String, Map<String, Number>> metricAsJson(MetricDataset metricDataset, boolean full) {
        if (full)
            return metricAsJson(metricDataset.getMetric().getReportingPath(), metricDataset.getStatistics().getMin(),
                    metricDataset.getStatistics().getMax(), metricDataset.getStatistics().getSum(),
                    metricDataset.getStatistics().getN(), metricDataset.getStatistics().getSumsq());
        else
            return metricAsJson(metricDataset.getMetric().getReportingPath(),
                    metricDataset.getStatistics().getSum(),
                    metricDataset.getStatistics().getN());
    }

    public Map.Entry<String, Map<String, Number>> metricAsJson(String path, Double min, Double max, Double sum,
                                                               Long count, Double sumsq) {
        Map<String, Number> values = new HashMap<String, Number>();
        values.put(NEW_RELIC_MIN, min.isNaN() ? 0 : min);
        values.put(NEW_RELIC_MAX, max.isNaN() ? 0 : max);
        values.put(NEW_RELIC_TOTAL, sum.isNaN() ? 0 : sum);
        values.put(NEW_RELIC_COUNT, count);
        values.put(NEW_RELIC_SUM_OF_SQUARES, sumsq.isNaN() ? 0 : sumsq);
        return new AbstractMap.SimpleEntry<String, Map<String, Number>>(path, values);
    }

    public Map.Entry<String, Map<String, Number>> metricAsJson(String path, Double sum, Long count) {
        Map<String, Number> values = new HashMap<String, Number>();
        values.put(NEW_RELIC_TOTAL, sum.isNaN() ? 0 : sum);
        values.put(NEW_RELIC_COUNT, count);
        return new AbstractMap.SimpleEntry<String, Map<String, Number>>(path, values);
    }

    public int toStateCode(String stateString) {
        if (stateString.startsWith("ACTIVE")) return 1;
        else if (stateString.startsWith("PASSIVE")) return 2;
        else if (stateString.startsWith("INITIALIZING")) return 4;
        else return 8;
    }
}