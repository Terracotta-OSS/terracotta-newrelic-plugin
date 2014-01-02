package com.newrelic.plugins.terracotta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class TCL2MonLauncher {	
	private static Logger log = LoggerFactory.getLogger(TCL2MonLauncher.class);

	public static void main(String[] args) {
    	Runner runner = new Runner();
    	runner.add(new TCL2AgentFactory());
    	
		try {
	    	//Never returns
	    	runner.setupAndRun();
		} catch (ConfigurationException e) {
			log.error("An error occurred during plugin setup", e);
    		System.exit(-1);
		}
    }
}
