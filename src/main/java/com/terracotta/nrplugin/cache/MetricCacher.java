package com.terracotta.nrplugin.cache;

import com.jayway.jsonpath.JsonPath;
import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.MetricDataset;
import com.terracotta.nrplugin.pojo.RatioMetric;
import com.terracotta.nrplugin.rest.tmc.MetricFetcher;
import com.terracotta.nrplugin.util.MetricUtil;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/21/14
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class MetricCacher {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ExecutorService executorService;

    @Autowired
    MetricFetcher metricFetcher;

    @Autowired
    MetricUtil metricUtil;

    @Value("#{cacheManager.getCache('statsCache')}")
    Cache statsCache;

    @Value("#{cacheManager.getCache('diffsCache')}")
    Cache diffsCache;

    Map<String, MetricDataset> lastDataSet = new HashMap<String, MetricDataset>();

    @Value("${com.saggs.terracotta.nrplugin.data.windowSize}")
    int windowSize;

	@Scheduled(fixedDelayString = "${com.saggs.terracotta.nrplugin.tmc.executor.fixedDelay.milliseconds}", initialDelay = 500)
    public void cacheStats() throws Exception {
        log.info("Starting to cache all stats...");
        Map<Metric.Source, String> metricData = metricFetcher.getAllMetricData();
        Map<Metric.Source, JSONArray> jsonObjects = toJsonArray(metricData);

        log.info("Parsed metrics into JSONArrays...");
        for (Metric metric : metricUtil.getNonRatioMetrics()) {
            JSONArray objects = jsonObjects.get(metric.getSource());
            for (Object o : objects) {
                MetricDataset metricDataset = getMetricDataset(metric);
                log.trace("Extracting values for " + metricDataset.getKey());
                expandPathVariables(metricDataset, (JSONObject) o);

                // Put absolute value into cache
                putValue(metricDataset, (JSONObject) o);

                // Put diff value into cache
                putDiff(lastDataSet.get(metricDataset.getKey()), metricDataset);
            }
        }

        log.info("Starting to cache Ratio Metrics...");
        for (Metric metric : metricUtil.getRatioMetrics()) {
            RatioMetric ratioMetric = (RatioMetric) metric;
            for (Object key : statsCache.getKeys()) {
                Element element = statsCache.get((key));
                if (element != null && element.getObjectValue() instanceof MetricDataset) {
                    MetricDataset metricDataset = (MetricDataset) element.getObjectValue();
                    if (metricDataset.getKey().contains(ratioMetric.getNumeratorCount())) {
//                        log.info("Found match for '" + metricDataset.getKey() + "' and '"
//                                + ratioMetric.getNumeratorCount() + "'");
                        String denominatorKey = ratioMetric.isHitRatio() ?
                                metricDataset.getKey().replace("Hit", "Miss") :
                                metricDataset.getKey().replace("Miss", "Hit");
                        String ratioKey = metricDataset.getKey().replace("Count", "Ratio");
                        Element denominatorElement = statsCache.get(denominatorKey);
                        if (denominatorElement != null && denominatorElement.getObjectValue() instanceof MetricDataset) {
                            MetricDataset denominatorDataset = (MetricDataset) denominatorElement.getObjectValue();
                            double numerator = metricDataset.getStatistics().getSum();
                            double denominator = (metricDataset.getStatistics().getSum() + denominatorDataset.getStatistics().getSum());
                            double ratio = denominator > 0 ? numerator / denominator : 0;
                            MetricDataset ratioDataset = getMetricDataset(ratioMetric);
                            ratioDataset.setActualVarReplaceMap(metricDataset.getActualVarReplaceMap());
                            putValue(ratioDataset, ratio);
                            putDiff(lastDataSet.get(ratioDataset.getKey()), ratioDataset);
                            log.trace(metricDataset.getKey() + " / " + denominatorKey + ": " + numerator + " / " + denominator + " = " + ratio);
                        }
                    }
                }
            }
        }
        log.info("Done caching stats.");
    }

    private Map<Metric.Source, JSONArray> toJsonArray(Map<Metric.Source, String> metricData) {
        Map<Metric.Source, JSONArray> jsonObjects = new HashMap<Metric.Source, JSONArray>();
        for (Metric.Source source : Metric.Source.values()) {
            String json = metricData.get(source);
            JSONArray objects = JsonPath.read(json, "$[*]");
            jsonObjects.put(source, objects);
        }
        return jsonObjects;
    }

    private void putValue(MetricDataset metricDataset, JSONObject jsonObject) {
        Object value = JsonPath.read(jsonObject, metricDataset.getMetric().getDataPath());
        if (value instanceof Integer) putValue(metricDataset, (Integer) value);
        else if (value instanceof Double) putValue(metricDataset, (Double) value);
        else if (value instanceof Long) putValue(metricDataset, (Long) value);
        else if (value instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) value;
            putValue(metricDataset, jsonArray.size());
        }
        else {
            log.warn("Class " + value.getClass() + " not numeric.");
        }
//        putMetricDataset(metricDataset);
    }

    private void putValue(MetricDataset metricDataset, double value) {
        metricDataset.addValue(value);
        putMetricDataset(metricDataset);
//        putDiff(lastDataSet.get(metricDataset.getKey()), metricDataset);
    }

    public MetricDataset getMetricDataset(Metric metric) {
        Element element = statsCache.get(metric.getReportedPath());
        if (element != null) return (MetricDataset) element.getObjectValue();
        else return new MetricDataset(metric, windowSize);
    }

    public void putMetricDataset(MetricDataset metricDataset) {
        log.trace("Putting " + metricDataset.getKey() + " to statsCache.");
        statsCache.put(new Element(metricDataset.getKey(), metricDataset));
    }

    private void expandPathVariables(MetricDataset metricDataset, JSONObject jsonObject) {
        log.trace("Attempting to expand key " + metricDataset.getKey());
        for (Map.Entry<String, String> entry : metricDataset.getMetric().getDataPathVariables().entrySet()) {
            if (metricDataset.getActualVarReplaceMap().get(entry.getKey()) == null) {
                metricDataset.putVarReplace(entry.getKey(), (String) JsonPath.read(jsonObject, entry.getValue()));
            }
        }
    }

    public Map<String, Number> getDiff(String key) {
        Element element = diffsCache.get(key);
        if (element != null) return (Map<String, Number>) element.getObjectValue();
        else return null;
    }

    private void putDiff(MetricDataset previous, MetricDataset latest) {
        if (previous == null) {
            log.debug("No previously cached data for metric " + latest.getKey());
        }
        else {
            Map<String, Number> diffs = new HashMap<String, Number>();
            diffs.put(MetricUtil.NEW_RELIC_MIN, latest.getStatistics().getMin() - previous.getStatistics().getMin());
            diffs.put(MetricUtil.NEW_RELIC_MAX, latest.getStatistics().getMax() - previous.getStatistics().getMax());
            diffs.put(MetricUtil.NEW_RELIC_TOTAL, latest.getStatistics().getSum() - previous.getStatistics().getSum());
            diffs.put(MetricUtil.NEW_RELIC_COUNT, 1);
            diffs.put(MetricUtil.NEW_RELIC_SUM_OF_SQUARES, latest.getStatistics().getSumsq() - previous.getStatistics().getSumsq());

            // Generate new key for diff rather than absolute
            String newKey = new MetricDataset(latest.getMetric(), MetricDataset.Type.diff,
                    latest.getActualVarReplaceMap()).getKey();
            log.trace("Putting " + newKey);
            diffsCache.put(new Element(newKey, diffs));
        }

        // Update lastDataSet after done
        lastDataSet.put(latest.getKey(), latest);
    }

}
