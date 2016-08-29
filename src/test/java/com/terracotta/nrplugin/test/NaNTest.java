package com.terracotta.nrplugin.test;

import com.terracotta.nrplugin.app.AppConfig;
import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.MetricBuilder;
import com.terracotta.nrplugin.pojo.MetricDataset;
import com.terracotta.nrplugin.pojo.MetricDatasetServerComponent;
import com.terracotta.nrplugin.util.MetricUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by jeffreysegal on 9/3/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AppConfig.class})
public class NaNTest {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MetricUtil metricUtil;

    @Test
    public void test() {
        MetricDataset fakeDataset = createMetricDataset();
//        fakeDataset.addValue(100);
//        fakeDataset.addValue(50);
//        fakeDataset.addValue(125);
        Map.Entry<String, Map<String, Number>> json = metricUtil.metricAsJson(fakeDataset, true);

        log.info("Printing keys...");
        for (Map.Entry<String, Number> entry : json.getValue().entrySet()) {
            log.info(entry.getKey() + "=" + entry.getValue());
        }
    }

    private MetricDataset createMetricDataset() {
        Metric metric = MetricBuilder.create("FakeMetric").
                setDisplayName("Max").
                setSource(Metric.Source.cache).
                setUnit(Metric.Unit.Count).
                setCreateDiff(false).
                setType(Metric.Type.regular).
                setMaxWindowSize(100).
                addReportingPath(Arrays.asList("Root", "Folder1")).
                build();
        return new MetricDataset(metric, new MetricDatasetServerComponent("FakeMetricServerName", "FakeMetricServerStripe"));
    }

}
