package com.terracotta.nrplugin.pojo.tmc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 9:57 AM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Topologies extends TmcBase {

    private static final long serialVersionUID = 8106596157713698078L;

    List<ServerGroupEntities> serverGroupEntities;
    List<ClientEntities> clientEntities;
    UnreadOperatorEventCount unreadOperatorEventCount;

    public List<ServerGroupEntities> getServerGroupEntities() {
        return serverGroupEntities;
    }

    public void setServerGroupEntities(List<ServerGroupEntities> serverGroupEntities) {
        this.serverGroupEntities = serverGroupEntities;
    }

    public List<ClientEntities> getClientEntities() {
        return clientEntities;
    }

    public void setClientEntities(List<ClientEntities> clientEntities) {
        this.clientEntities = clientEntities;
    }

    public UnreadOperatorEventCount getUnreadOperatorEventCount() {
        return unreadOperatorEventCount;
    }

    public void setUnreadOperatorEventCount(UnreadOperatorEventCount unreadOperatorEventCount) {
        this.unreadOperatorEventCount = unreadOperatorEventCount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("serverGroupEntities", serverGroupEntities)
                .append("clientEntities", clientEntities)
                .append("unreadOperatorEventCount", unreadOperatorEventCount)
                .toString();
    }
}
