package com.newrelic.plugins.terracotta.metrics;

import com.newrelic.plugins.terracotta.utils.MetricUnit;

public class EhcacheClientMetric extends ClientMetric {
	public static final String METRICS_FAMILY_EHCACHE = "Ehcache";
	public static final String ehcachePrefix = String.format("%s/%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_EHCACHE, METRICS_FAMILY_CLIENTS);

	protected final String cacheManagerName;
	protected final String cacheName;
	
	public EhcacheClientMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition, String clientID, String cacheManagerName, String cacheName) {
		super(name, unit, aggregationType, resultDefinition, clientID);
		this.cacheManagerName = cacheManagerName;
		this.cacheName = cacheName;
	}
	
	public String getCacheManagerName() {
		return cacheManagerName;
	}

	public String getCacheName() {
		return cacheName;
	}

	@Override
	public EhcacheClientMetric clone() throws CloneNotSupportedException {
		EhcacheClientMetric clone = new EhcacheClientMetric(name, unit, aggregationType, resultDefinition, clientID, cacheManagerName, cacheName);
		if(null != getMetricData())
			clone.setMetricData(getMetricData().clone());
		return clone;
	}
	
	public String getPrefix(){
		return String.format("%s/%s/%s/%s", 
				ehcachePrefix, 
				(null==clientID)?METRICS_ALL:"id/"+sanitize(clientID),
				(null==cacheManagerName)?METRICS_ALL:"id/"+sanitize(cacheManagerName),
				(null==cacheName)?METRICS_ALL:"id/"+sanitize(cacheName)
				);
	}
}
