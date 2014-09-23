package com.terracotta.nrplugin.rest.interceptors;

import org.apache.http.*;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Created by Jeff on 9/23/2014.
 */
public class GzipResponseInterceptor implements HttpResponseInterceptor {

	@Override
	public void process(HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			Header ceheader = entity.getContentEncoding();
			if (ceheader != null) {
				HeaderElement[] codecs = ceheader.getElements();
				for (HeaderElement codec : codecs) {
					if (codec.getName().equalsIgnoreCase("gzip")) {
						response.setEntity(
								new GzipDecompressingEntity(response.getEntity()));
						return;
					}
				}
			}
		}
	}

}
