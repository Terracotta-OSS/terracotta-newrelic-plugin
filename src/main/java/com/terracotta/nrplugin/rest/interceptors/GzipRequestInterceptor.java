package com.terracotta.nrplugin.rest.interceptors;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Created by Jeff on 9/23/2014.
 */
public class GzipRequestInterceptor implements HttpRequestInterceptor {

	@Override
	public void process(HttpRequest request, HttpContext httpContext) throws HttpException, IOException {
		if (!request.containsHeader("Accept-Encoding")) {
			request.addHeader("Accept-Encoding", "gzip");
		}
	}

}
