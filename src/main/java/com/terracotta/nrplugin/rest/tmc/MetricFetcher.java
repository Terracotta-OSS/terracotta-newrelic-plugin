package com.terracotta.nrplugin.rest.tmc;

import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.tmc.CacheStatistics;
import com.terracotta.nrplugin.pojo.tmc.ClientStatistics;
import com.terracotta.nrplugin.pojo.tmc.ServerStatistics;
import com.terracotta.nrplugin.pojo.tmc.Topologies;
import com.terracotta.nrplugin.rest.StateManager;
import com.terracotta.nrplugin.util.MetricUtil;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 11:24 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class MetricFetcher extends BaseTmcClient {

	final Logger tmcResponseLog = LoggerFactory.getLogger("tmcResponseLog");

	List<NameValuePair> cacheNames = new ArrayList<NameValuePair>();

	@Autowired
	MetricUtil metricUtil;

	@Autowired
	StateManager stateManager;

	int numRelogAttempts = 0;

	@Value("${com.saggs.terracotta.nrplugin.restapi.numRelogAttempts}")
	int maxRelogAttempts;

	@Value("${com.saggs.terracotta.nrplugin.restapi.agents.sample.percentage}")
	protected double agentSamplePercentage;

	@Value("${com.saggs.terracotta.nrplugin.restapi.agents.sample.enabled}")
	boolean agentSampleEnabled;

    @Value("${com.saggs.terracotta.nrplugin.restapi.agents.idsPrefix.value}")
    String idsPrefix;

    @Value("${com.saggs.terracotta.nrplugin.restapi.agents.idsPrefix.enabled}")
    boolean idsPrefixEnabled;

    public static final String API_AGENTS_PREFIX = "/agents";

//	@Autowired
//	AgentService agentService;

	@PostConstruct
	private void init() throws Exception {
		for (String statName : metricUtil.getCacheStatsNames()) {
			cacheNames.add(new BasicNameValuePair(MetricUtil.PARAMETER_SHOW, statName));
		}
	}

    private String getApiPrefix() {
        if (idsPrefixEnabled) {
            return API_AGENTS_PREFIX + ";ids=" + idsPrefix;
        }
        else {
            return API_AGENTS_PREFIX;
        }
    }

    private String getClientStatsUrl() {
        return getApiPrefix() + "/statistics/clients/";
    }

    private String getServerStatsUrl() {
        return getApiPrefix() + "/statistics/servers/";
    }

    private String getCachesUrl() {
        return getApiPrefix() + "/cacheManagers/caches";
    }

    private String getTopologiesUrl() {
        return getApiPrefix() + "/topologies";
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
			log.debug("Executing HTTP GET to '" + url + "'");
			tmcResponseLog.debug("Executing HTTP GET to '" + url + "'");
		}
		Object payload = null;
		try {
			payload = getRestTemplate().getForObject(url, clazz);
//			log.debug("TMC Payload: " + payload);
			tmcResponseLog.debug("" + payload);
			stateManager.setTmcState(StateManager.TmcState.available);
		} catch (HttpClientErrorException e) {
			stateManager.setTmcState(StateManager.TmcState.unavailable);
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
		return (String) doGet(getServerStatsUrl(), String.class);
	}

	public List<ServerStatistics> getServerStatistics() throws Exception {
		return (List<ServerStatistics>) doGet(getServerStatsUrl(), List.class);
	}

	public String getClientStatisticsAsString() throws Exception {
		return (String) doGet(getClientStatsUrl(), String.class);
	}

	public List<ClientStatistics> getClientStatistics() throws Exception {
		return (List<ClientStatistics>) doGet(getClientStatsUrl(), List.class);
	}

	public String getCacheStatisticsAsString() throws Exception {
		return (String) doGet(constructCacheManagersUrl(), String.class, cacheNames);
	}

	public List<CacheStatistics> getCacheStatistics() throws Exception {
		return (List<CacheStatistics>) doGet(constructCacheManagersUrl(), List.class, cacheNames);
	}

	private String constructCacheManagersUrl() {
		if (agentSampleEnabled) {
//			List<String> agentIds = findEhcacheAgentSample();
//			String baseUrl = "/api/agents;ids=";
//			for (int i = 0; i < agentIds.size(); i++) {
//				String agentId = agentIds.get(i);
//				baseUrl += agentId;
//				if (i + 1 < agentIds.size()) baseUrl += ",";
//			}
//			baseUrl += "/cacheManagers/caches";
//			return baseUrl;
            log.warn("Sampling Disabled! Not filtering agents.");
            return getCachesUrl();
		}
		else return getCachesUrl();
	}

	public List<Topologies> getTopologies() throws Exception {
		return (List<Topologies>) doGet(getTopologiesUrl(), List.class);
	}

	public String getTopologiesAsString() throws Exception {
		return (String) doGet(getTopologiesUrl(), String.class);
	}

//	public List<String> findEhcacheAgentSample() {
//		log.info("Creating sample agent list...");
//		if (agentSamplePercentage < 0 || agentSamplePercentage > 1) {
//			throw new IllegalArgumentException("percentage must be between 0 and 1");
//		}
//		Set<String> agentsSample = new HashSet<String>();
//		List<String> allAgents = findAllEhcacheAgents();
//		if (allAgents.size() > 0) {
//			int sampleSize = (int) (allAgents.size() * agentSamplePercentage);
//			for (int i = 0; i < sampleSize; i++) {
//				String sample;
//				do {
//					sample = allAgents.get(RandomUtils.nextInt(allAgents.size()));
//				}
//				while (agentsSample.contains(sample));
//				agentsSample.add(sample);
//			}
//		}
//
//		log.info("Created list of " + agentsSample.size() + " agent(s).");
//		return new ArrayList<String>(agentsSample);
//	}
//
//	public List<String> findAllEhcacheAgents() {
//		List<String> agents = new ArrayList<String>();
//		try {
//			List<Map<String, Object>> payload = getRestTemplate().getForObject(tmcUrl + "/api/agents/info", List.class);
//			for (Map<String, Object> map : payload) {
//				if ("Ehcache".equals(map.get("agencyOf"))) {
//					agents.add((String) map.get("agentId"));
//				}
//			}
//		} catch (Exception e) {
//			log.error("Error: ", e);
//		}
//		return agents;
//	}

	public String buildUrl(String url, List<NameValuePair> params) throws URISyntaxException {
		HttpUriRequest request = RequestBuilder.get()
				.setUri(new URI(url))
				.addParameters(params.toArray(new NameValuePair[params.size()]))
				.build();
		return request.getURI().toString();
	}

}
