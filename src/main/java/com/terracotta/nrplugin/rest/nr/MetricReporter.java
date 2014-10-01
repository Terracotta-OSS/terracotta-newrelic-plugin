package com.terracotta.nrplugin.rest.nr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracotta.nrplugin.cache.LockManager;
import com.terracotta.nrplugin.cache.MetricProvider;
import com.terracotta.nrplugin.pojo.MetricDataset;
import com.terracotta.nrplugin.pojo.nr.NewRelicPayload;
import com.terracotta.nrplugin.rest.StateManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/21/14
 * Time: 3:02 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class MetricReporter {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String X_LICENSE_KEY = "X-License-Key";

	CloseableHttpClient httpClient;

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	MetricProvider metricProvider;

	@Autowired
	StateManager stateManager;

	@Value("${com.saggs.terracotta.nrplugin.nr.agent.licenseKey}")
	String licenseKey;

	@Value("${com.saggs.terracotta.nrplugin.nr.proxy.host}")
	String proxyHostname;

	@Value("${com.saggs.terracotta.nrplugin.nr.proxy.port}")
	String proxyPort;

	@Value("${com.saggs.terracotta.nrplugin.nr.proxy.scheme}")
	String proxyScheme;

	@Value("${com.saggs.terracotta.nrplugin.nr.scheme}")
	String nrScheme;

	@Value("${com.saggs.terracotta.nrplugin.nr.host}")
	String nrHost;

	@Value("${com.saggs.terracotta.nrplugin.nr.port}")
	int nrPort;

	@Value("${com.saggs.terracotta.nrplugin.nr.path}")
	String nrPath;

	@Value("${com.saggs.terracotta.nrplugin.nr.proxy.enabled}")
	boolean useProxy;

	@Value("${com.saggs.terracotta.nrplugin.nr.executor.fixedDelay.milliseconds}")
	long durationMillis;

	@Value("#{cacheManager.getCache('statsCache')}")
	Cache statsCache;

	@Autowired
	LockManager lockManager;

	@PostConstruct
	private void init() {
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(10000)
				.setSocketTimeout(10000)
				.setConnectionRequestTimeout(5000)
				.setStaleConnectionCheckEnabled(true)
				.build();
		HttpClientBuilder httpClientBuilder = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig);
		if (useProxy) {
            int parsedProxyPort = 8080;
            try {
                parsedProxyPort = Integer.parseInt(proxyPort);
            } catch (NumberFormatException e) {
                log.warn("Could not parse the proxyPort. Defaulting to 8080.");
                parsedProxyPort = 8080;
            }

            HttpHost proxy = new HttpHost(proxyHostname, parsedProxyPort, proxyScheme);
			httpClientBuilder.setProxy(proxy);
			log.info("Configuring HttpClient with proxy '" + proxy.toString() + "'");
		}
		httpClient = httpClientBuilder.build();
	}

	@Scheduled(fixedDelayString = "${com.saggs.terracotta.nrplugin.nr.executor.fixedDelay.milliseconds}", initialDelay = 5000)
	public void reportMetrics() {
		try {
			lockManager.lockCache();
			doReportMetrics();
		} finally {
			lockManager.unlockCache();
		}
	}

	private void doReportMetrics() {
		if (StateManager.TmcState.available.equals(stateManager.getTmcState())) {
			try {
				NewRelicPayload newRelicPayload = metricProvider.assemblePayload();
				log.info("Attempting to report stats to NewRelic...");
				if (log.isDebugEnabled()) {
					try {
						log.debug("Payload: " + new ObjectMapper().writeValueAsString(newRelicPayload));
					} catch (JsonProcessingException e) {
						log.error("Error serializing payload.", e);
					}
				}

				String json = mapper.writeValueAsString(newRelicPayload);
				HttpHost target = new HttpHost(nrHost, nrPort, nrScheme);
				HttpPost httpPost = new HttpPost(nrPath);
				httpPost.setEntity(new StringEntity(json));
				httpPost.setHeader(X_LICENSE_KEY, licenseKey);
				httpPost.setHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
				httpPost.setHeader(org.apache.http.HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
				CloseableHttpResponse response = httpClient.execute(target, httpPost);
				log.info("New Relic Response code: " + response.getStatusLine().getStatusCode());
				if (log.isDebugEnabled()) {
					log.debug("Received response: " + EntityUtils.toString(response.getEntity()));
				}
				EntityUtils.consumeQuietly(response.getEntity());
				log.info("Done reporting to NewRelic.");
				clearAllMetricData();
			} catch (Exception e) {
				log.error("Error while attempting to publish stats to NewRelic.", e);
			}
		}
		else {
			log.info("TMC State is '" + stateManager.getTmcState() + "', so disabling NR publication.");
		}
	}

	private void clearAllMetricData() {
		log.info("Clearing all metric data...");
		List<String> keys = statsCache.getKeys();
		for (String key : keys) {
			Element element = statsCache.get(key);
			MetricDataset metricDataset = (MetricDataset) element.getObjectValue();
			metricDataset.getStatistics().clear();
		}
	}

}
