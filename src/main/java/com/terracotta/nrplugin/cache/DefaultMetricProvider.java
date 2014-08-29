package com.terracotta.nrplugin.cache;

import com.terracotta.nrplugin.pojo.MetricDataset;
import com.terracotta.nrplugin.util.MetricUtil;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/25/14
 * Time: 8:03 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class DefaultMetricProvider implements MetricProvider {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("#{cacheManager.getCache('statsCache')}")
    Cache statsCache;

    @Value("#{cacheManager.getCache('diffsCache')}")
    Cache diffsCache;

    @Autowired
    MetricUtil metricUtil;

    @Override
    public Map<String, Object> getAllMetrics() {
        log.debug("Gathering stats from cache...");
        Map<String, Object> metrics = new HashMap<String, Object>();
        List<MetricDataset> datasets = new ArrayList<MetricDataset>();

        // Get absolute metrics
        for (Object key : statsCache.getKeys()) {
            Element element = statsCache.get((key));
            if (element != null && element.getObjectValue() instanceof MetricDataset) {
                MetricDataset metricDataset = (MetricDataset) element.getObjectValue();
                datasets.add(metricDataset);
            }
        }
        metrics.putAll(metricUtil.metricsAsJson(datasets));

        // Get diff metrics
        for (Object key : diffsCache.getKeys()) {
            Element element = diffsCache.get((key));
            if (element != null && element.getObjectValue() instanceof Map) {
                metrics.put((String) element.getObjectKey(), element.getObjectValue());
            }
        }

        log.info("Returning " + metrics.size() + " metric(s) from cache.");
        return metrics;
    }

}
