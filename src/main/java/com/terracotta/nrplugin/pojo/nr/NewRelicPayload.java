package com.terracotta.nrplugin.pojo.nr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/24/14
 * Time: 11:14 AM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicPayload {

    Agent agent;
    List<Component> components;

    public NewRelicPayload() {
    }

    public NewRelicPayload(Agent agent, List<Component> components) {
        this.agent = agent;
        this.components = components;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public List<Component> getComponents() {
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
    }

}
