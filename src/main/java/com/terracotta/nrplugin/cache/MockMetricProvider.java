package com.terracotta.nrplugin.cache;

import com.terracotta.nrplugin.pojo.*;
import com.terracotta.nrplugin.pojo.nr.Agent;
import com.terracotta.nrplugin.pojo.nr.Component;
import com.terracotta.nrplugin.pojo.nr.NewRelicPayload;
import com.terracotta.nrplugin.util.MetricUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/25/14
 * Time: 8:26 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class MockMetricProvider implements MetricProvider {

    @Autowired
    MetricUtil metricUtil;

//	@Autowired
//	MetricDatasetFactory metricDatasetFactory;

    @Override
    public NewRelicPayload assemblePayload() throws Exception {
        String metricName = "FakeMetricName";
        Metric metric = MetricBuilder.create(metricName).
                addReportingPath(Arrays.asList("", "")).
                setSource(Metric.Source.cache).
                setUnit(Metric.Unit.Bytes).
                build();

        List<Component> components = new ArrayList<Component>();

        MetricDataset m1 = new MetricDataset(metric, new MetricDatasetCacheComponent("CacheName", "cacheMgr"));
//		MetricDataset m1 = metricDatasetFactory.construct(metric, "componentname");
        m1.addValue(100);
        m1.addValue(50);
        m1.addValue(300);

        Component c1 = new Component(m1.getComponentDetail().getComponentName(), "someguid", 30);
        c1.putMetric(m1, metricUtil, true);
        components.add(c1);

        MetricDataset m2 = new MetricDataset(metric, new MetricDatasetServerComponent("ServerName", "StripeName"));
        m2.addValue(100);
        m2.addValue(50);
        m2.addValue(300);
        Component c2 = new Component(m2.getComponentDetail().getComponentName(), "someotherguid", 30);
        c2.putMetric(m2, metricUtil, true);
        components.add(c2);

        return new NewRelicPayload(new Agent(), components);
    }

    @Override
    public void clearAllMetricData() {
        //do nothing
    }
}
