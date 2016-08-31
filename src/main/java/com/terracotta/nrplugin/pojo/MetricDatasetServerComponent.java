package com.terracotta.nrplugin.pojo;

import java.io.Serializable;

/**
 * Created by fabien.sanglier on 8/25/16.
 */
public class MetricDatasetServerComponent extends MetricDatasetComponent implements Serializable, Cloneable {

    private String serverName;
    private String stripeName;
    private State state = State.UNKNOWN;

    public static enum State {
        ACTIVE("ACTIVE-COORDINATOR"),
        PASSIVE("PASSIVE-STANDBY"),
        UNINITIALIZED("PASSIVE-UNINITIALIZED"),
        ERROR("ERROR"),
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

            if (stateString.equalsIgnoreCase(ACTIVE.getName())) return ACTIVE;
            else if (stateString.equalsIgnoreCase(PASSIVE.getName())) return PASSIVE;
            else if (stateString.equalsIgnoreCase(UNINITIALIZED.getName())) return UNINITIALIZED;
            else return UNKNOWN;
        }
    }

    public MetricDatasetServerComponent(String serverName, String stripeName) {
        this.serverName = serverName;
        this.stripeName = stripeName;
    }

    public MetricDatasetServerComponent(String serverName, String stripeName, State state) {
        this.serverName = serverName;
        this.stripeName = stripeName;
        this.state = state;
    }

    @Override
    public String getComponentName() {
        return String.format("%s/%s", stripeName, serverName);
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getStripeName() {
        return stripeName;
    }

    public void setStripeName(String stripeName) {
        this.stripeName = stripeName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "MetricDatasetServerComponent{" +
                "serverName='" + serverName + '\'' +
                ", stripeName='" + stripeName + '\'' +
                ", state=" + state +
                '}';
    }

    @Override
    public MetricDatasetServerComponent clone() throws CloneNotSupportedException {
        return (MetricDatasetServerComponent) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricDatasetServerComponent that = (MetricDatasetServerComponent) o;

        if (serverName != null ? !serverName.equals(that.serverName) : that.serverName != null) return false;
        if (state != that.state) return false;
        if (stripeName != null ? !stripeName.equals(that.stripeName) : that.stripeName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = serverName != null ? serverName.hashCode() : 0;
        result = 31 * result + (stripeName != null ? stripeName.hashCode() : 0);
        result = 31 * result + state.hashCode();
        return result;
    }
}
