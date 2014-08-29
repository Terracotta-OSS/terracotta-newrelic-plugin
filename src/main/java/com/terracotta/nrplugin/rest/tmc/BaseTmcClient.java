package com.terracotta.nrplugin.rest.tmc;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

/**
 * Created by Jeff on 8/11/2014.
 */
public class BaseTmcClient {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	private RestTemplate restTemplate = null;

	@Value("${com.saggs.terracotta.nrplugin.tmc.url}")
	protected String tmcUrl;

	@Value("${com.saggs.terracotta.nrplugin.tmc.username}")
	protected String tmcUsername;

	@Value("${com.saggs.terracotta.nrplugin.tmc.password}")
	protected String tmcPassword;

	public void resetRestTemplate() {
		restTemplate = null;
	}

	public RestTemplate getRestTemplate() throws Exception {
		if (restTemplate == null) {
			log.info("Attempting to log in to TMC at '" + tmcUrl + "'...");
			BasicCookieStore cookieStore = new BasicCookieStore();
			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setRedirectStrategy(new LaxRedirectStrategy())
					.setHostnameVerifier(new AllowAllHostnameVerifier())
					.build();
			String loginUrl = tmcUrl + "/login.jsp";
			HttpUriRequest login = RequestBuilder.post()
					.setUri(new URI(loginUrl))
					.addParameter("username", tmcUsername)
					.addParameter("password", tmcPassword)
					.build();
			CloseableHttpResponse loginResponse = httpclient.execute(login);
			HttpEntity loginResponseEntity = loginResponse.getEntity();
			EntityUtils.consume(loginResponseEntity);
			if (HttpStatus.SC_OK == loginResponse.getStatusLine().getStatusCode()) {
				restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpclient));
			}
			else throw new IOException("Could not authenticate to TMC.");
		}
		return restTemplate;
	}

}
