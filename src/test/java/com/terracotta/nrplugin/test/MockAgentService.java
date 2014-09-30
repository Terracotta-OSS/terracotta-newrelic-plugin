package com.terracotta.nrplugin.test;

import com.terracotta.nrplugin.rest.tmc.BaseAgentService;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Jeff on 9/23/2014.
 */
public class MockAgentService extends BaseAgentService {

	@Override
	public List<String> findAllEhcacheAgents() {
		return Arrays.asList("one", "two", "three", "four", "five");
	}

}
