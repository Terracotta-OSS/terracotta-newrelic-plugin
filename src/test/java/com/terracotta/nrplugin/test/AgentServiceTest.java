package com.terracotta.nrplugin.test;

import com.terracotta.nrplugin.app.ReportingConfig;
import com.terracotta.nrplugin.rest.tmc.AgentService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Created by Jeff on 9/23/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ReportingConfig.class})
public class AgentServiceTest {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	AgentService agentService;

	@Test
	public void test() {
		List<String> agents = agentService.findAllEhcacheAgents();
		for (String agent : agents) {
			log.info(agent);
		}
	}

}
