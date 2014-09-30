package com.terracotta.nrplugin.cache;

import com.terracotta.nrplugin.pojo.nr.NewRelicPayload;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/25/14
 * Time: 7:57 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MetricProvider {

//    public Map<String, Map<String, Object>> getAllMetrics();

	public NewRelicPayload assemblePayload() throws Exception;

}
