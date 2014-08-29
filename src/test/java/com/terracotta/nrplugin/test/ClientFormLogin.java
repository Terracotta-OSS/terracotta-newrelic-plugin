package com.terracotta.nrplugin.test;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.List;

/**
 * Created by Jeff on 8/11/2014.
 */
public class ClientFormLogin {

	public static void main(String[] args) throws Exception {
		BasicCookieStore cookieStore = new BasicCookieStore();
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultCookieStore(cookieStore)
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build();
		Cookie jsessionId = null;
		try {
			HttpGet httpget = new HttpGet("http://localhost:9889/tmc/api/agents/statistics/servers/");
			CloseableHttpResponse response1 = httpclient.execute(httpget);
			try {
				HttpEntity entity = response1.getEntity();

				System.out.println("Login form get: " + response1.getStatusLine());
				EntityUtils.consume(entity);

				System.out.println("Initial set of cookies:");
				List<Cookie> cookies = cookieStore.getCookies();
				if (cookies.isEmpty()) {
					System.out.println("None");
				}
				else {
					for (int i = 0; i < cookies.size(); i++) {
						Cookie cookie = cookies.get(i);
						System.out.println("- " + cookie.toString());
						if ("JSESSIONID".equals(cookie.getName())) {
							jsessionId = cookie;
						}
					}
				}
			} finally {
				response1.close();
			}


//			String url = "http://localhost:9889/tmc/login.jsp;JSESSIONID=" + jsessionId;
			String url = "http://localhost:9889/tmc/login.jsp";
			System.out.println("Posting to " + url);
			if (jsessionId != null) {
				HttpUriRequest login = RequestBuilder.post()
						.setUri(new URI(url))
						.addParameter("username", "operator")
						.addParameter("password", "operator")
						.build();
				CloseableHttpResponse response2 = httpclient.execute(login);
				try {
					HttpEntity entity = response2.getEntity();

					System.out.println("Login form get: " + response2.getStatusLine());
					EntityUtils.consume(entity);

					System.out.println("Post logon cookies:");
					List<Cookie> cookies = cookieStore.getCookies();
					if (cookies.isEmpty()) {
						System.out.println("None");
					}
					else {
						for (int i = 0; i < cookies.size(); i++) {
							Cookie cookie = cookies.get(i);
							System.out.println("- " + cookie.toString());
							if ("JSESSIONID".equals(cookie.getName())) {
								jsessionId = cookie;
							}
						}
					}
				} finally {
					response2.close();
				}
			}


//			cookieStore.clear();
//			cookieStore.addCookie(jsessionId);
			HttpUriRequest apiRequest = RequestBuilder.get()
					.setUri(new URI("http://localhost:9889/tmc/api/agents/statistics/servers/"))
					.build();
			CloseableHttpResponse response3 = httpclient.execute(apiRequest);
			System.out.println("API get: " + response3.getStatusLine());
			HttpEntity entity = response3.getEntity();
			String stats = EntityUtils.toString(entity);
			System.out.printf(stats);

		} finally {
			httpclient.close();
		}
	}
}