package com.terracotta.nrplugin.test;

import com.terracotta.nrplugin.rest.tmc.AgentService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Jeff on 9/23/2014.
 */
public class MockAgentServiceTest {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	@Test
	public void test() {
		AgentService agentService = new MockAgentService();
		List<String> samples = agentService.findEhcacheAgentSample(0.25);
		log.info("Found " + samples.size() + " sample(s).");
		for (String sample : samples) {
			log.info(sample);
		}
	}


}
