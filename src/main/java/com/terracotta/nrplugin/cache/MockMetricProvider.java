package com.terracotta.nrplugin.cache;

import com.terracotta.nrplugin.pojo.Metric;
import com.terracotta.nrplugin.pojo.MetricBuilder;
import com.terracotta.nrplugin.pojo.MetricDataset;
import com.terracotta.nrplugin.pojo.nr.Agent;
import com.terracotta.nrplugin.pojo.nr.Component;
import com.terracotta.nrplugin.pojo.nr.NewRelicPayload;
import com.terracotta.nrplugin.util.MetricUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

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
		MetricDataset m1 = new MetricDataset(metric, "componentname");
//		MetricDataset m1 = metricDatasetFactory.construct(metric, "componentname");
		m1.addValue(100);
		m1.addValue(50);
		m1.addValue(300);
//		map.put(m1.getComponentName(), metricUtil.metricsAsJson(Collections.singletonList(m1)));
		return new NewRelicPayload(new Agent(),
				Collections.singletonList(new Component(m1.getComponentName(), "someguid", 30)));
	}
}
