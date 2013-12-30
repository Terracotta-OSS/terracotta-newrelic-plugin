package com.newrelic.plugins.terracotta;

import java.util.Map;

import org.terracotta.utils.jmxclient.TCL2JMXClient;
import org.terracotta.utils.jmxclient.beans.L2ProcessInfo;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class TCL1sAgentFactory extends AgentFactory {

	public TCL1sAgentFactory() {
		super("com.newrelic.plugins.terracotta-ehcache.json");
	}

	@Override
	public String getAgentConfigurationFileName() {
		// TODO Auto-generated method stub
		return super.getAgentConfigurationFileName();
	}

//	@Override
//	//this method should return a list of all the clients connected
//	public JSONArray readJSONFile(String arg0) throws ConfigurationException {
//		//get the configuration
//		JSONArray json = super.readJSONFile(arg0);
//
//		TCL2JMXClient baseJMXClient = null;
//		for (int i = 0; i < json.size(); i++) {
//			JSONObject obj = (JSONObject) json.get(i);
//			@SuppressWarnings("unchecked")
//			Map<String, Object> map = obj;
//
//			//find at least one config that connects
//			baseJMXClient = getJMXClient(map);
//			if(null != baseJMXClient && baseJMXClient.initialize())
//				break;
//		}
//
//		if(null == baseJMXClient || !baseJMXClient.initialize())
//			throw new ConfigurationException("Could not JMX connect to any of the nodes specified in the config file...");
//
//		//first, check if that JMX connection has the ehcache mbeans
//		TCL2JMXClient jmxTCEhcache = null;
//		if(baseJMXClient.hasEhcacheMBeans()){
//			jmxTCEhcache = baseJMXClient;
//		} else {
//			//find the TC node that has the ehcache mbeans
//			jmxTCEhcache = getJMXClientWithEhcacheMBeans(baseJMXClient);
//		}
//
//		if(jmxTCEhcache == null || !jmxTCEhcache.initialize())
//			throw new ConfigurationException("Could not find a cluster node with tunnelled ehcache mbeans...");
//		
//		//close the JMX connections, as we no longer need it here since we found the right server.
//		jmxTCEhcache.close();
//		
//		
//		
//		
//		//let's package a new JSONArray based on the 
//		Map<String, Object> jmxTCEhcacheProperties = new HashMap<String, Object>();
//		jmxTCEhcacheProperties.put("jmx_host", jmxTCEhcache.getHost());
//		jmxTCEhcacheProperties.put("jmx_port", jmxTCEhcache.getPort());
//		jmxTCEhcacheProperties.put("jmx_user", jmxTCEhcache.getUsername());
//		jmxTCEhcacheProperties.put("jmx_password", jmxTCEhcache.getPassword());
//		
//		JSONObject newObj = new JSONObject(jmxTCEhcacheProperties);
//		
//	}

	public TCL2JMXClient getJMXClient(Map<String, Object> properties) throws ConfigurationException {
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

		System.out.println(String.format("Connecting to JMX Server [%s:%d] with user=%s", jmx_host, jmx_port, jmx_user));
		return new TCL2JMXClient(jmx_user, jmx_pwd, jmx_host, jmx_port);
	}

	public TCL2JMXClient getJMXClientWithEhcacheMBeans(final TCL2JMXClient baseJMXClient) throws ConfigurationException {
		//getting terracotta topology to identify which server is the right one to find the ehcache tunnelled mbeans
		L2ProcessInfo[] nodesInCluster = baseJMXClient.getAllL2Nodes();

		TCL2JMXClient TCEhCacheJMXClient = null;
		for(L2ProcessInfo nodeInfo : nodesInCluster){
			TCL2JMXClient TCTempJMXClient = new TCL2JMXClient(baseJMXClient.getUsername(), baseJMXClient.getPassword(), nodeInfo.getHostAddress(), nodeInfo.getHostPortJmxConnect());
			if(TCTempJMXClient.getClientCount() > -1){
				TCEhCacheJMXClient = TCTempJMXClient;
				break;
			} else {
				TCTempJMXClient.close(); //make sure to close all these JMX connections
			}
		}

		return TCEhCacheJMXClient;
	}

	@Override
	public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
		String name = (String) properties.get("name");
		String jmx_username = (String) properties.get("name");
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

		System.out.println(String.format("Connecting to JMX Server [%s:%d] with user=%s", jmx_host, jmx_port, jmx_username));
		TCL2JMXClient jmxTCClientStart = new TCL2JMXClient(jmx_username, jmx_pwd, jmx_host, jmx_port);

		//getting terracotta topology to identify which server is the right one to find the ehcache tunnelled mbeans
		L2ProcessInfo[] nodesInCluster = jmxTCClientStart.getAllL2Nodes();

		TCL2JMXClient TCEhCacheJMXClient = null;
		for(L2ProcessInfo nodeInfo : nodesInCluster){
			TCEhCacheJMXClient = new TCL2JMXClient(jmx_username, jmx_pwd, nodeInfo.getHostAddress(), nodeInfo.getHostPortJmxConnect());
			if(TCEhCacheJMXClient.hasEhcacheMBeans())
				break;
		}

		if(TCEhCacheJMXClient == null)
			throw new ConfigurationException("Could not find a cluster node with tunnelled ehcache mbeans...");

		//TODO: this needs work...bogus...
		return new TCL1Agent(TCEhCacheJMXClient, null);
	}
}
