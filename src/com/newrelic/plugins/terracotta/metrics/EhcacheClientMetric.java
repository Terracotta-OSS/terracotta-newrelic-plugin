package com.newrelic.plugins.terracotta.metrics;

import com.newrelic.plugins.terracotta.utils.MetricUnit;

public class EhcacheClientMetric extends ClientMetric {
	public static final String METRICS_FAMILY_EHCACHE = "Ehcache";
	public static final String ehcachePrefix = String.format("%s/%s/%s", METRICS_FAMILY_TC, METRICS_FAMILY_EHCACHE, METRICS_FAMILY_CLIENTS);

	protected String cacheManagerName;
	protected String cacheName;
	
	protected EhcacheClientMetric(EhcacheClientMetric metric){
		super(metric);
		this.cacheManagerName = metric.cacheManagerName;
		this.cacheName = metric.cacheName;
	}
	
	public EhcacheClientMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition, String clientID, String cacheManagerName, String cacheName) {
		super(name, unit, aggregationType, resultDefinition, clientID);
		this.cacheManagerName = cacheManagerName;
		this.cacheName = cacheName;
	}
	
	public EhcacheClientMetric(String name, MetricUnit unit, AggregationType aggregationType, MetricResultDefinition resultDefinition, String clientID, String cacheManagerName, String cacheName, boolean publishEnabled) {
		super(name, unit, aggregationType, resultDefinition, clientID, publishEnabled);
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
		EhcacheClientMetric cloned = new EhcacheClientMetric(this);
		if(null != this.getMetricData())
			cloned.setMetricData(this.getMetricData().clone());
		
		return cloned;
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
