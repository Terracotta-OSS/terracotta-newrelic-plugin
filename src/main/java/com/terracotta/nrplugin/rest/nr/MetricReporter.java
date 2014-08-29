package com.terracotta.nrplugin.rest.nr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracotta.nrplugin.cache.MetricProvider;
import com.terracotta.nrplugin.pojo.nr.Agent;
import com.terracotta.nrplugin.pojo.nr.Component;
import com.terracotta.nrplugin.pojo.nr.NewRelicPayload;
import com.terracotta.nrplugin.util.MetricUtil;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;

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

	long pid;

	CloseableHttpClient httpClient;

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	MetricUtil metricUtil;

	@Autowired
	MetricProvider metricProvider;

	@Value("${com.saggs.terracotta.nrplugin.nr.agent.name}")
	String name;

	@Value("${com.saggs.terracotta.nrplugin.nr.agent.guid}")
	String guid;

//	@Value("${com.saggs.terracotta.nrplugin.nr.agent.hostname}")
	String hostname = "TC-NR-HOST";

	@Value("${com.saggs.terracotta.nrplugin.nr.agent.licenseKey}")
	String licenseKey;

	@Value("${com.saggs.terracotta.nrplugin.version}")
	String version;

	@Value("${com.saggs.terracotta.nrplugin.nr.proxy.host}")
	String proxyHostname;

	@Value("${com.saggs.terracotta.nrplugin.nr.proxy.port}")
	int proxyPort;

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

	@Value("${com.saggs.terracotta.nrplugin.nr.useProxy}")
	boolean useProxy;

	@PostConstruct
	private void init() {
		Sigar sigar = new Sigar();

		try {
			pid = sigar.getPid();
		} catch (Error e) {
			log.error("Could not infer PID.");
			pid = -1;
		}

		try {
			hostname = sigar.getNetInfo().getHostName();
		} catch (Error e) {
			log.error("Could not infer hostname.");
		} catch (Exception ex) {
			log.error("Could not infer hostname.");
		}

		if (useProxy) {
			HttpHost proxy  = new HttpHost(proxyHostname, proxyPort, proxyScheme);
			log.info("Configuring HttpClient with proxy '" + proxy.toString() + "'");
			httpClient = HttpClients.custom()
					.setProxy(proxy)
					.build();
		}
		else {
			log.info("Configuring default HttpClient.");
			httpClient = HttpClients.createDefault();
		}
	}

	@Scheduled(fixedDelayString = "${com.saggs.terracotta.nrplugin.nr.executor.fixedDelay.milliseconds}", initialDelay = 5000)
	public void reportMetrics() {
		try {
			NewRelicPayload newRelicPayload = new NewRelicPayload(
					new Agent(hostname, pid, version),
					Collections.singletonList(
							new Component(name, guid, 30, metricProvider.getAllMetrics())));
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
			log.info("Done reporting to NewRelic.");
		} catch (Exception e) {
			log.error("Error while attempting to publish stats to NewRelic.", e);
		}
	}

}
