package com.terracotta.nrplugin.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracotta.nrplugin.pojo.nr.NewRelicPayload;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;

/**
 * Created by Jeff on 8/29/2014.
 */
public class ProxyTest {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	HttpHost target = new HttpHost("platform-api.newrelic.com", 443, "https");
	HttpHost proxy = new HttpHost("localhost", 8085, "http");
	RequestConfig requestConfig;

	@Before
	public void init() {
		requestConfig = RequestConfig.custom()
//          .setProxy(proxy)
          .build();
	}

	@Test
	public void testHttpClient() throws IOException {
//		System.setProperty("http.proxyHost", "localhost");
//		System.setProperty("http.proxyPort", 8080 + "");
		CloseableHttpClient httpClient = HttpClients.createDefault();

		String json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("metric-publish-first.json"));
		HttpPost httpPost = new HttpPost("/platform/v1/metrics");
		httpPost.setEntity(new StringEntity(json));
		httpPost.setConfig(requestConfig);

//		HttpGet httpGet = new HttpGet("");
		CloseableHttpResponse response = httpClient.execute(target, httpPost);
		log.info(EntityUtils.toString(response.getEntity()));
	}

//	@Test
	public void testRestTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		InetSocketAddress address = new InetSocketAddress("localhost", 8080);
		Proxy proxy = new Proxy(Proxy.Type.HTTP,address);
//		requestFactory.setProxy(proxy);

//		CloseableHttpClient httpClient = HttpClients.custom()
//				.setProxy(proxy)
//				.setDefaultRequestConfig(requestConfig)
//				.build();
//		ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		HttpEntity<String> response = restTemplate.getForEntity("http://www.google.com", String.class);
		log.info(response.getBody());
	}

}
