package com.terracotta.nrplugin.rest.tmc;

import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.tmc.CacheStatistics;
import com.terracotta.nrplugin.pojo.tmc.ClientStatistics;
import com.terracotta.nrplugin.pojo.tmc.ServerStatistics;
import com.terracotta.nrplugin.pojo.tmc.Topologies;
import com.terracotta.nrplugin.rest.StateManager;
import com.terracotta.nrplugin.util.MetricUtil;
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
    final Logger tmcResponseLog = LoggerFactory.getLogger("tmcResponseLog");

    List<NameValuePair> cacheNames = new ArrayList<NameValuePair>();

    @Autowired
    MetricUtil metricUtil;

    @Autowired
    StateManager stateManager;

    int numRelogAttempts = 0;

    @Value("${com.saggs.terracotta.nrplugin.restapi.authentication.numRelogAttempts}")
    int maxRelogAttempts;

//	@Autowired
//	AgentService agentService;

    @PostConstruct
    private void init() throws Exception {
        for (String statName : metricUtil.getCacheStatsNames()) {
            cacheNames.add(new BasicNameValuePair(MetricUtil.PARAMETER_SHOW, statName));
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

    public Object doGet(String url, Class clazz, List<NameValuePair> requestParams) throws Exception {
        if (requestParams != null) {
            url = buildUrl(url, requestParams);
        }
        Object payload = null;
        try {
            if (log.isDebugEnabled())
                log.debug("Executing HTTP GET to '{}'", url);

            if (tmcResponseLog.isDebugEnabled())
                tmcResponseLog.debug("Executing HTTP GET to '{}'", url);

            payload = getRestTemplate().getForObject(url, clazz);

            if (tmcResponseLog.isDebugEnabled())
                tmcResponseLog.debug("" + payload);

            stateManager.setTmcState(StateManager.TmcState.available);
        } catch (HttpClientErrorException e) {
            log.error("Received error HTTP response code {} for url '{}'", e.getStatusCode(), url);
            stateManager.setTmcState(StateManager.TmcState.unavailable);
            if (org.springframework.http.HttpStatus.FORBIDDEN == e.getStatusCode()) {
                log.error("Login error - Will try to re-login {} more times", maxRelogAttempts - numRelogAttempts);
                resetRestTemplate();
                numRelogAttempts++;
                if (maxRelogAttempts <= numRelogAttempts) {
                    numRelogAttempts = 0;
                    throw new Exception("Exceeded maximum re-login attempts.");
                } else {
                    return doGet(url, clazz, requestParams);
                }
            }
        }
        return payload;
    }

    public Object doGet(String url, Class clazz) throws Exception {
        return doGet(url, clazz, null);
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
        return getCachesUrl();
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
