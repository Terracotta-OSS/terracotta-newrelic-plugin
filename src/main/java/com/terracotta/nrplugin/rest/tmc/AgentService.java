package com.terracotta.nrplugin.rest.tmc;

import java.util.List;

/**
 * Created by Jeff on 9/23/2014.
 */
public interface AgentService {

	public List<String> findEhcacheAgentSample(double percentage);

	public List<String> findAllEhcacheAgents();

}
