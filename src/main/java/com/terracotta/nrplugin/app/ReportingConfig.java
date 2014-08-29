package com.terracotta.nrplugin.app;

import com.terracotta.nrplugin.cache.MetricProvider;
import com.terracotta.nrplugin.cache.MockMetricProvider;
import com.terracotta.nrplugin.util.MetricUtil;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/25/14
 * Time: 8:23 AM
 * To change this template use File | Settings | File Templates.
 */
@Configuration
@Import({RestConfig.class})
public class ReportingConfig {

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public MetricProvider metricProvider() {
        return new MockMetricProvider();
    }

    @Bean
    public MetricUtil metricUtil() {
        return new MetricUtil();
    }

}
