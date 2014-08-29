package com.terracotta.nrplugin.test;

import com.terracotta.nrplugin.app.ReportingConfig;
import com.terracotta.nrplugin.rest.nr.MetricReporter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/25/14
 * Time: 8:33 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ReportingConfig.class})
public class MetricReporterTest {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MetricReporter metricReporter;

    @Test
    public void test() {
        log.info("Testing " + metricReporter.getClass() + ".");
        metricReporter.reportMetrics();
    }

}
