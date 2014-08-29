package com.terracotta.nrplugin.test;

import com.terracotta.nrplugin.app.ReportingConfig;
import com.terracotta.nrplugin.app.RestConfig;
import com.terracotta.nrplugin.pojo.tmc.CacheStatistics;
import com.terracotta.nrplugin.pojo.tmc.ClientStatistics;
import com.terracotta.nrplugin.pojo.tmc.ServerStatistics;
import com.terracotta.nrplugin.rest.tmc.MetricFetcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 9:15 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ReportingConfig.class})
public class MetricFetcherTest {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MetricFetcher metricFetcher;

    @Test
    public void test() throws Exception {
        log.info("Getting server stats...");
        List<ServerStatistics> serverStatisticsList = metricFetcher.getServerStatistics();
        log.info("Getting client stats...");
        List<ClientStatistics> clientStatisticsList = metricFetcher.getClientStatistics();
        log.info("Getting cache stats...");
        List<CacheStatistics> cacheStatisticsList = metricFetcher.getCacheStatistics();
        log.info("Done.");
    }



//    @Test
    public void testUriBuilder() {
        MultiValueMap<String, String> cacheStatsRequestParams = new LinkedMultiValueMap<String, String>();
        cacheStatsRequestParams.add("p1", "stuff");
        cacheStatsRequestParams.add("p1", "bar");
        cacheStatsRequestParams.add("p2", "asd");

//        Map<String, String> cacheStatsRequestParams = new HashMap<String, String>();
//        cacheStatsRequestParams.put("p1", "stuff");
//        cacheStatsRequestParams.put("p1", "bar");
//        cacheStatsRequestParams.put("p2", "asd");

        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("http").host("example.com").path("/hotels?p1={p1}").build()
                .expand(cacheStatsRequestParams)
                .encode();
        URI uri = uriComponents.toUri();
        log.info(uri.toString());
    }

}
