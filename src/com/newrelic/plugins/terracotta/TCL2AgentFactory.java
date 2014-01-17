package com.newrelic.plugins.terracotta;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class TCL2AgentFactory extends AgentFactory {
	private static Logger log = LoggerFactory.getLogger(TCL2AgentFactory.class);

	private static final String PARAMNAME_NAME = "name";
	private static final String PARAMNAME_HOST = "jmx_host";
	private static final String PARAMNAME_PORT = "jmx_port";
	private static final String PARAMNAME_USER = "jmx_user";
	private static final String PARAMNAME_PWD = "jmx_password";
	private static final String PARAMNAME_NAMEDISCOVERY = "namediscovery";
	private static final String PARAMNAME_TRACKUNIQUECLIENTS = "trackUniqueClients";
	private static final String PARAMNAME_INTERVALINMILLIS = "intervalInMillis";

	private static final int PARAM_NULL_NUMBER = -1;
	
	public TCL2AgentFactory() {
		super("com.newrelic.plugins.terracotta.json");
	}
	
	@Override
	public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
		String name = (String) properties.get(PARAMNAME_NAME);
		String jmx_host = (String) properties.get(PARAMNAME_HOST);
		String jmx_user = (String) properties.get(PARAMNAME_USER);
		String jmx_pwd = (String) properties.get(PARAMNAME_PWD);
		if(null != jmx_user && "".equals(jmx_user.trim())){
			jmx_user = null;
		}
		
		boolean nameDiscovery = Boolean.parseBoolean((String) properties.get(PARAMNAME_NAMEDISCOVERY));
		boolean trackUniqueClients = Boolean.parseBoolean((String) properties.get(PARAMNAME_TRACKUNIQUECLIENTS));

		int jmx_port = PARAM_NULL_NUMBER;
		try {
			jmx_port = Integer.parseInt((String)properties.get(PARAMNAME_PORT));
		} catch (NumberFormatException e) {
			log.error("Could not parse jmx_port", e);
		}
		
		long intervalInMillis = PARAM_NULL_NUMBER;
		try {
			intervalInMillis = Long.parseLong((String)properties.get(PARAMNAME_INTERVALINMILLIS));
		} catch (NumberFormatException e) {
			log.error("Could not parse the interval number", e);
		}

		if(null == jmx_host || jmx_port == 0){
			throw new ConfigurationException(String.format("No Host/Port specified. This agent [%s] will not be started", name));
		}
		
		return new TCL2Agent(name, jmx_host, jmx_port, jmx_user, jmx_pwd, nameDiscovery, trackUniqueClients, intervalInMillis);
	}
}
