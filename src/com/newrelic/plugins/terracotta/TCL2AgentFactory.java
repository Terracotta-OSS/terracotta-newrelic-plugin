package com.newrelic.plugins.terracotta;

import java.util.Map;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class TCL2AgentFactory extends AgentFactory {

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
		
		int jmx_port = 0;
		try {
			jmx_port = Integer.parseInt((String)properties.get("jmx_port"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return new TCL2Agent(name, jmx_host, jmx_port, jmx_user, jmx_pwd);
	}
}
