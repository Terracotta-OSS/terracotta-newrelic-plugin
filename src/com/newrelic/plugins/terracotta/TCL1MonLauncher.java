package com.newrelic.plugins.terracotta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.jmxclient.TCL2JMXClient;
import org.terracotta.utils.jmxclient.beans.L2ClientID;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.tc.cli.CommandLineBuilder;

public class TCL1MonLauncher {	
	private static Logger log = LoggerFactory.getLogger(TCL1MonLauncher.class);

	public static void main(String[] args) {
		CommandLineBuilder commandLineBuilder = new CommandLineBuilder(TCL1MonLauncher.class.getName(), args);
		commandLineBuilder.addOption("n", "hostname", true, "The Terracotta Server instane hostname", String.class, true, "l2-hostname");
		commandLineBuilder.addOption("p", "jmxport", true, "Terracotta Server instance JMX port", Integer.class, true, "l2-jmx-port");
		commandLineBuilder.addOption("u", "username", true, "username", String.class, false);
		commandLineBuilder.addOption("w", "password", true, "password", String.class, false);
		commandLineBuilder.addOption("t", "interval", true, "Statistics Dump Interval in seconds", Integer.class, false, "interval");
		commandLineBuilder.addOption("h", "help", String.class, false);
		commandLineBuilder.parse();

		if (commandLineBuilder.hasOption('h')) {
			commandLineBuilder.usageAndDie();
		}

		String username = null;
		String password = null;
		if (commandLineBuilder.hasOption('u')) {
			username = commandLineBuilder.getOptionValue('u');
			if (commandLineBuilder.hasOption('w')) {
				password = commandLineBuilder.getOptionValue('w');
			} else {
				password = CommandLineBuilder.readPassword();
			}
		}

		String host = commandLineBuilder.getOptionValue('n');
		int port = 9520;
		try {
			port = Integer.parseInt(commandLineBuilder.getOptionValue('p'));
		} catch (Exception e) {
			log.warn("Port not valid...defaulting to 9520", e);
		}

		int interval = -1;
		if (commandLineBuilder.hasOption('t')){
			try {
				interval = Integer.parseInt(commandLineBuilder.getOptionValue('t'));
			} catch (Exception e) {
				log.error("", e);
			}
		}
		
		try {
			TCL2JMXClient jmxClient = new TCL2JMXClient(username, password, host, port);

			if(null == jmxClient || !jmxClient.initialize())
				throw new ConfigurationException("Could not JMX connect to any of the nodes specified in the config file...");

			Runner runner = new Runner();

			//let's assume for now that I'm connecting to an active node...
			//we need to register one agent per connected client
			L2ClientID[] clientIds = jmxClient.getClientIDs();
			if(null != clientIds){
				for(L2ClientID clientId : clientIds){
					runner.register(new TCL1Agent(jmxClient, clientId));
				}
			} else {
				throw new ConfigurationException("There is no client id connected for now...");
			}

			//Never returns
			runner.setupAndRun();
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.err.println("Error configuring");
			System.exit(-1);
		}
	}
}
