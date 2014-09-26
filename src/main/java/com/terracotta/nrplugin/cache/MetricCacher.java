package com.terracotta.nrplugin.cache;

import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.terracotta.nrplugin.pojo.*;
import com.terracotta.nrplugin.rest.tmc.MetricFetcher;
import com.terracotta.nrplugin.util.MetricUtil;
import net.minidev.json.JSONArray;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

	Map<String, DescriptiveStatistics> lastDataSet = new HashMap<String, DescriptiveStatistics>();

	@Value("${com.saggs.terracotta.nrplugin.data.windowSize}")
	int windowSize;

//	@Autowired
//	MetricDatasetFactory metricDatasetFactory;

	@Scheduled(fixedDelayString = "${com.saggs.terracotta.nrplugin.tmc.executor.fixedDelay.milliseconds}", initialDelay = 500)
	public void cacheStats() throws Exception {
		log.info("Starting to cache all stats...");
		Map<Metric.Source, String> metricData = metricFetcher.getAllMetricData();
		Map<Metric.Source, JSONArray> jsonObjects = toJsonArray(metricData);

		// Get all cache names
		Set<String> cacheNames = getCacheNames(jsonObjects.get(Metric.Source.cache));
		Set<String> serverNames = getServerNames(jsonObjects.get(Metric.Source.server));

		log.info("Parsed metrics into JSONArrays...");
		for (Metric metric : metricUtil.getRegularMetrics()) {
			// Get all JSON data for this source
			JSONArray objects = jsonObjects.get(metric.getSource());

			if (Metric.Source.cache.equals(metric.getSource())) {
				for (String cacheName : cacheNames) {
					// Filter by cache name & metric
					JSONArray values = JsonPath.read(objects, metric.getDataPath(), Filter.filter(Criteria.where("name").is(cacheName)));
					MetricDataset metricDataset = getMetricDataset(metric, cacheName);
					log.trace("Extracting values for " + metricDataset.getKey());
					putValue(metricDataset, values);
				}
			}
			else if (Metric.Source.server.equals(metric.getSource())) {
				for (String serverName : serverNames) {
					// Filter by server name & metric
					JSONArray values = JsonPath.read(objects, metric.getDataPath(), Filter.filter(Criteria.where("sourceId").is(serverName)));
					MetricDataset metricDataset = getMetricDataset(metric, serverName);
					log.trace("Extracting values for " + metricDataset.getKey());
					putValue(metricDataset, values);
				}
			}
		}

		// Handle special metrics
		for (Metric metric : metricUtil.getSpecialMetrics()) {
			JSONArray objects = jsonObjects.get(metric.getSource());
			if (MetricUtil.METRIC_NUM_CONNECTED_CLIENTS.equals(metric.getName())) {
				JSONArray clientEntities = JsonPath.read(objects, "$[*].clientEntities");
				if (clientEntities.size() > 0) {
					JSONArray array = (JSONArray) clientEntities.get(0);
					for (String serverName : serverNames) {
						MetricDataset metricDataset = getMetricDataset(metric, serverName);
						putValue(metricDataset, array.size());
					}
				}
			}
			else if (MetricUtil.METRIC_SERVER_STATE.equals(metric.getName())) {
				for (String serverName : serverNames) {
					JSONArray attributes = JsonPath.read(objects, "$[*].serverGroupEntities.servers.attributes");
					JSONArray stateArray = JsonPath.read(attributes, "$[?].State", Filter.filter(Criteria.where("Name").is(serverName)));
					if (stateArray.size() > 0) {
						MetricDataset metricDataset = getMetricDataset(metric, serverName);
						putValue(metricDataset, metricUtil.toStateCode((String) stateArray.get(0)));
					}
				}
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
						String denominatorKey = ratioMetric.isHitRatio() ?
								metricDataset.getKey().replace("Hit", "Miss") :
								metricDataset.getKey().replace("Miss", "Hit");
						String ratioKey = metricDataset.getKey().replace("Count", "Ratio");
						Element denominatorElement = statsCache.get(denominatorKey);
						if (denominatorElement != null && denominatorElement.getObjectValue() instanceof MetricDataset) {
							MetricDataset denominatorDataset = (MetricDataset) denominatorElement.getObjectValue();
							double numerator = metricDataset.getStatistics().getSum();
							double denominator = (metricDataset.getStatistics().getSum() + denominatorDataset.getStatistics().getSum());
							double ratio = denominator > 0 ? 100 * numerator / denominator : 0;
							MetricDataset ratioDataset = getMetricDataset(ratioMetric, denominatorDataset.getComponentName());
							putValue(ratioDataset, ratio);
							log.trace(metricDataset.getKey() + " / " + denominatorKey + ": " + numerator + " / " + denominator + " = " + ratio);
						}
					}
				}
			}
		}
		log.info("Done caching stats.");
	}

	private Set<String> getCacheNames(JSONArray objects) {
		return getSet(objects, "name");
	}

	private Set<String> getServerNames(JSONArray objects) {
		return getSet(objects, "sourceId");
	}

	private Set<String> getSet(JSONArray objects, String attribute) {
		JSONArray nameArray = JsonPath.read(objects, "$[*]." + attribute);
		Set<String> names = new HashSet<String>();
		for (Object object : nameArray) {
			names.add((String) object);
		}
		return names;
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

	private void putValue(MetricDataset metricDataset, Object value) {
		if (metricDataset.getMetric().isCreateDiff()) putDiff(metricDataset);
		if (value instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) value;
			for (Object child : jsonArray) {
				putPrimitiveValue(metricDataset, child);
			}
		}
		else if (value instanceof Number) {
			putPrimitiveValue(metricDataset, value);
		}
	}

	private void putPrimitiveValue(MetricDataset metricDataset, Object value) {
		if (value instanceof Number) {
			putPrimitiveValue(metricDataset, ((Number) value).doubleValue());
		}
		else {
			log.warn("Class " + value.getClass() + " not numeric.");
		}
	}

	private void putPrimitiveValue(MetricDataset metricDataset, double value) {
		metricDataset.addValue(value);
		putMetricDataset(metricDataset);
	}

	public MetricDataset getMetricDataset(Metric metric, String componentName) {
		Element element = statsCache.get(MetricDataset.getKey(metric, componentName));
		if (element != null) return (MetricDataset) element.getObjectValue();
		else {
//			return metricDatasetFactory.construct(metric, componentName);
//			return (MetricDataset) beanFactory.getBean("metricDataset", metric, componentName, metric.getMaxWindowSize());
			return new MetricDataset(metric, componentName);
		}
	}

	public void putMetricDataset(MetricDataset metricDataset) {
		log.trace("Putting " + metricDataset.getKey() + " to statsCache.");
		statsCache.put(new Element(metricDataset.getKey(), metricDataset));
	}

	public Map<String, Number> getDiff(String key) {
		Element element = diffsCache.get(key);
		if (element != null) return (Map<String, Number>) element.getObjectValue();
		else return null;
	}

	private void putDiff(MetricDataset latest) {
		DescriptiveStatistics previousStatistics = lastDataSet.get(latest.getKey());
		if (previousStatistics == null) {
			log.debug("No previously cached data for metric " + latest.getKey());
		}
		else {
			log.debug("Latest SUM: " + latest.getStatistics().getSum() + ", Previous SUM: " + previousStatistics.getSum());

			Metric diffMetric = MetricBuilder.create(latest.getMetric().getName()).
					setReportingComponents(latest.getMetric().getReportingComponents()).
					setSource(latest.getMetric().getSource()).
					setUnit(latest.getMetric().getUnit()).
					setType(latest.getMetric().getType()).
					setDiff(true).
					build();
			MetricDataset diffDataSet = new MetricDataset(diffMetric, latest.getComponentName());
//			MetricDataset diffDataSet = metricDatasetFactory.construct(diffMetric, latest.getComponentName());
			Map.Entry<String, Map<String, Number>> diff = metricUtil.metricAsJson(diffMetric.getReportingPath(),
					toDouble(latest.getStatistics().getMin() - previousStatistics.getMin()),
					toDouble(latest.getStatistics().getMax() - previousStatistics.getMax()),
					toDouble(latest.getStatistics().getSum() - previousStatistics.getSum()),
					latest.getStatistics().getN() - previousStatistics.getN(),
					toDouble(latest.getStatistics().getSumsq() - previousStatistics.getSumsq()));

			String diffKey = diffDataSet.getKey();
			log.trace("Putting " + diffKey);
			diffsCache.put(new Element(diffKey, new DiffEntry(diffDataSet, diff.getValue())));
		}

		// Update lastDataSet after done
		log.debug("Updating key '" + latest.getKey() + "', SUM: " + latest.getStatistics().getSum());
		lastDataSet.put(latest.getKey(), new SynchronizedDescriptiveStatistics(
				(SynchronizedDescriptiveStatistics) latest.getStatistics()));
	}

	private double toDouble(double value) {
		if (Double.isNaN(value)) return 0;
		else return value;
	}

	public class DiffEntry {

		MetricDataset metricDataset;
		Map<String, Number> diffs;

		public DiffEntry() {
		}

		public DiffEntry(MetricDataset metricDataset, Map<String, Number> diffs) {
			this.metricDataset = metricDataset;
			this.diffs = diffs;
		}

		public Map<String, Number> getDiffs() {
			return diffs;
		}

		public void setDiffs(Map<String, Number> diffs) {
			this.diffs = diffs;
		}

		public MetricDataset getMetricDataset() {
			return metricDataset;
		}

		public void setMetricDataset(MetricDataset metricDataset) {
			this.metricDataset = metricDataset;
		}
	}
}
