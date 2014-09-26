package com.terracotta.nrplugin.pojo;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Jeff on 9/26/2014.
 */
//@Component
//public class MetricDatasetFactory {
//
//	@Autowired
//	BeanFactory beanFactory;
//
//	public MetricDataset construct(Metric metric, String componentName) {
//		return (MetricDataset) beanFactory.getBean("metricDataset", metric, componentName);
//	}
//
//	public String getKey(Metric metric, String componentName) {
//		MetricDataset metricDataset = (MetricDataset) beanFactory.getBean("metricDataset", metric, componentName);
//		return metricDataset.getKey();
//	}
//
//}
