package com.newrelic.plugins.terracotta;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class TCL2AgentFactory extends AgentFactory {
	private static Logger log = LoggerFactory.getLogger(TCL2AgentFactory.class);

	public TCL2AgentFactory() {
		super("com.newrelic.plugins.terracotta.json");
	}
	
	@Override
	public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
		String name = (String) properties.get("name");
		String jmx_host = (String) properties.get("jmx_host");
		String jmx_user = (String) properties.get("jmx_user");
		String jmx_pwd = (String) properties.get("jmx_password");
		if(null != jmx_user && "".equals(jmx_user.trim())){
			jmx_user = null;
		}
		
		boolean nameDiscovery = Boolean.parseBoolean((String) properties.get("namediscovery"));
		boolean trackUniqueClients = Boolean.parseBoolean((String) properties.get("trackUniqueClients"));

		int jmx_port = 0;
		try {
			jmx_port = Integer.parseInt((String)properties.get("jmx_port"));
		} catch (NumberFormatException e) {
			log.error("Could not parse jmx_port", e);
		}

		if(null == jmx_host || jmx_port == 0){
			throw new ConfigurationException(String.format("No Host/Port specified. This agent [%s] will not be started", name));
		}
		
		return new TCL2Agent(name, jmx_host, jmx_port, jmx_user, jmx_pwd, nameDiscovery, trackUniqueClients);
	}
}
