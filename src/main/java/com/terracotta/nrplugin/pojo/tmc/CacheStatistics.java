package com.terracotta.nrplugin.pojo.tmc;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 12:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheStatistics extends TmcBase {

    private static final long serialVersionUID = -6957149063340427251L;

    String name;
    String cacheManagerName;
    Map<String, Object> attributes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCacheManagerName() {
        return cacheManagerName;
    }

    public void setCacheManagerName(String cacheManagerName) {
        this.cacheManagerName = cacheManagerName;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
