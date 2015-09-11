package com.terracotta.nrplugin.rest.tmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by jeffreysegal on 8/12/15.
 */
@Component
public class NewRelicUserAgentInterceptor implements ClientHttpRequestInterceptor {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${com.saggs.terracotta.nrplugin.version}")
    String pluginVersion;

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
                                        ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        String userAgentKey = "User-Agent";
        String userAgentValue = "Terracotta BigMemory plugin for New Relic/" + pluginVersion +
                " (https://github.com/Terracotta-OSS/terracotta-newrelic-plugin; info@softwareag-gov.com)";
        log.trace("Appending header '" + userAgentKey + "=" + userAgentValue + "'");
        httpRequest.getHeaders().add(userAgentKey, userAgentValue);
        return clientHttpRequestExecution.execute(httpRequest, bytes);
    }

}

