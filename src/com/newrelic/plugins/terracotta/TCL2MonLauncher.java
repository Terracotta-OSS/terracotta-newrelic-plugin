package com.newrelic.plugins.terracotta;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class TCL2MonLauncher {	
    public static void main(String[] args) {
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
