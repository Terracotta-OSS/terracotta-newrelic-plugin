package com.newrelic.plugins.terracotta;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class TCL2MonLauncher {	
    public static void main(String[] args) {
    	//System.getProperties().put("newrelic.platform.config.dir","test/config");
    	System.out.println(System.getProperty("newrelic.platform.config.dir"));
    	
    	System.setProperty("http.proxyHost", "webcache.example.com");
    	System.setProperty("http.proxyPort", "8080");
    	
    	Runner runner = new Runner();
    	runner.add(new TCL2AgentFactory());
    	
		try {
	    	//Never returns
	    	runner.setupAndRun();
		} catch (ConfigurationException e) {
			e.printStackTrace();
    		System.err.println("Error configuring");
    		System.exit(-1);
		}
    }
}
