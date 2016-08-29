package com.terracotta.nrplugin.pojo;

import java.io.Serializable;

/**
 * Created by fabien.sanglier on 8/25/16.
 */
public class MetricDatasetCacheComponent extends MetricDatasetComponent implements Serializable, Cloneable {

    private String cacheName;
    private String cacheManagerName;
    private State state = State.UNKNOWN;

    public static enum State {
        ENABLED("ENABLED"),
        DISABLED("DISABLED"),
        UNKNOWN("UNKNOWN");

        final private String m_name;

        private State(final String name) {
            m_name = name;
        }

        public String getName() {
            return m_name;
        }

        public static State parseString(String stateString) {
            if (null == stateString) throw new IllegalArgumentException("cannot parse null");

            if (stateString.equalsIgnoreCase(ENABLED.getName())) return ENABLED;
            else if (stateString.equalsIgnoreCase(DISABLED.getName())) return DISABLED;
            else return UNKNOWN;
        }
    }

    public MetricDatasetCacheComponent(String cacheName, String cacheManagerName, State state) {
        this.cacheName = cacheName;
        this.cacheManagerName = cacheManagerName;
        this.state = state;
    }

    public MetricDatasetCacheComponent(String cacheName, String cacheManagerName) {
        this.cacheName = cacheName;
        this.cacheManagerName = cacheManagerName;
    }

    @Override
    public String getComponentName() {
        return String.format("%s/%s", cacheManagerName, cacheName);
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getCacheManagerName() {
        return cacheManagerName;
    }

    public void setCacheManagerName(String cacheManagerName) {
        this.cacheManagerName = cacheManagerName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "MetricDatasetCacheComponent{" +
                "cacheName='" + cacheName + '\'' +
                ", cacheManagerName='" + cacheManagerName + '\'' +
                ", state=" + state +
                '}';
    }

    @Override
    public MetricDatasetCacheComponent clone() throws CloneNotSupportedException {
        return (MetricDatasetCacheComponent) super.clone();
    }
}
