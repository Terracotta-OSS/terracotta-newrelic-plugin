package com.terracotta.nrplugin.rest.tmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Jeff on 9/23/2014.
 */
//@Service
public class DefaultAgentService extends BaseAgentService {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public List<String> findAllEhcacheAgents() {
		List<String> agents = new ArrayList<String>();
		try {
			List<Map<String, Object>> payload = getRestTemplate().getForObject(restApiUrl + "/api/agents/info", List.class);
			for (Map<String, Object> map : payload) {
				if ("Ehcache".equals(map.get("agencyOf"))) {
					agents.add((String) map.get("agentId"));
				}
			}
		} catch (Exception e) {
			log.error("Error: ", e);
		}
		return agents;
	}

}
