package com.terracotta.nrplugin.rest.tmc;

import org.apache.commons.lang.math.RandomUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Jeff on 9/23/2014.
 */
public abstract class BaseAgentService extends BaseTmcClient implements AgentService {

	@Override
	public List<String> findEhcacheAgentSample(double percentage) {
		if (percentage < 0 || percentage > 1) throw new IllegalArgumentException("percentage must be between 0 and 1");
		Set<String> agentsSample = new HashSet<String>();
		List<String> allAgents = findAllEhcacheAgents();
		int sampleSize = (int) (allAgents.size() * percentage);
		for (int i = 0; i < sampleSize; i++) {
			String sample;
			do {
				sample = allAgents.get(RandomUtils.nextInt(allAgents.size() - 1));
			}
			while (agentsSample.contains(sample));
			agentsSample.add(sample);
		}
		return new ArrayList<String>(agentsSample);
	}

}
