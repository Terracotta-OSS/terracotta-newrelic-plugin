package com.terracotta.nrplugin.rest.tmc;

import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.tmc.CacheStatistics;
import com.terracotta.nrplugin.pojo.tmc.ClientStatistics;
import com.terracotta.nrplugin.pojo.tmc.ServerStatistics;
import com.terracotta.nrplugin.pojo.tmc.Topologies;
import com.terracotta.nrplugin.util.MetricUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 11:24 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class MetricFetcher extends BaseTmcClient {

	List<NameValuePair> cacheNames = new ArrayList<NameValuePair>();

	@Autowired
	MetricUtil metricUtil;

	int numRelogAttempts = 0;

	@Value("${com.saggs.terracotta.nrplugin.tmc.numRelogAttempts}")
	int maxRelogAttempts;

	@PostConstruct
	private void init() throws Exception {
		for (String cacheName : metricUtil.getCacheStatsNames()) {
			cacheNames.add(new BasicNameValuePair(MetricUtil.PARAMETER_SHOW, cacheName));
		}
	}

	public Map<Metric.Source, String> getAllMetricData() throws Exception {
		Map<Metric.Source, String> metrics = new HashMap<Metric.Source, String>();
		metrics.put(Metric.Source.cache, getCacheStatisticsAsString());
		metrics.put(Metric.Source.client, getClientStatisticsAsString());
		metrics.put(Metric.Source.server, getServerStatisticsAsString());
		metrics.put(Metric.Source.topologies, getTopologiesAsString());
		return metrics;
	}

	public Object doGet(String uriPath, Class clazz, List<NameValuePair> requestParams) throws Exception {
		String url = tmcUrl + uriPath;
		if (requestParams != null) {
			url = buildUrl(url, requestParams);
		}
		Object payload = null;
		try {
			payload = getRestTemplate().getForObject(url, clazz);
		} catch (HttpClientErrorException e ) {
			if (org.springframework.http.HttpStatus.FORBIDDEN == e.getStatusCode()) {
				log.info("Received response code " + e.getStatusCode() + " for url '" + url + "'.");
				resetRestTemplate();
				numRelogAttempts++;
				if (maxRelogAttempts <= numRelogAttempts) {
					numRelogAttempts = 0;
					throw new Exception("Exceeded maximum relog attempts.");
				}
				else {
					return doGet(uriPath, clazz, requestParams);
				}
			}
		}
		return payload;
	}

	public Object doGet(String uriPath, Class clazz) throws Exception {
		return doGet(uriPath, clazz, null);
	}

	public String getServerStatisticsAsString() throws Exception {
		return (String) doGet("/api/agents/statistics/servers/", String.class);
	}

	public List<ServerStatistics> getServerStatistics() throws Exception {
		return (List<ServerStatistics>) doGet("/api/agents/statistics/servers/", List.class);
	}

	public String getClientStatisticsAsString() throws Exception {
		return (String) doGet("/api/agents/statistics/clients/", String.class);
	}

	public List<ClientStatistics> getClientStatistics() throws Exception {
		return (List<ClientStatistics>) doGet("/api/agents/statistics/clients/", List.class);
	}

	public String getCacheStatisticsAsString() throws Exception {
		return (String) doGet("/api/agents/cacheManagers/caches", String.class, cacheNames);
	}

	public List<CacheStatistics> getCacheStatistics() throws Exception {
		return (List<CacheStatistics>) doGet("/api/agents/cacheManagers/caches", List.class, cacheNames);
	}

	public List<Topologies> getTopologies() throws Exception {
		return (List<Topologies>) doGet("/api/agents/topologies/", List.class);
	}

	public String getTopologiesAsString() throws Exception {
		return (String) doGet("/api/agents/topologies/", String.class);
	}

	public String buildUrl(String url, List<NameValuePair> params) throws URISyntaxException {
		HttpUriRequest request = RequestBuilder.post()
				.setUri(new URI(url))
				.addParameters(params.toArray(new NameValuePair[params.size()]))
				.build();
		return request.getURI().toString();
	}

}
