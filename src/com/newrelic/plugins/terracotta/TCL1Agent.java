package com.newrelic.plugins.terracotta;

import org.terracotta.utils.jmxclient.TCL2JMXClient;
import org.terracotta.utils.jmxclient.beans.L2ClientID;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class TCL1Agent extends Agent {
	//private final MetricsBufferingWorker metricsWorker;
	private final TCL2JMXClient jmxTCClient;
	private final L2ClientID clientID;
	
	public TCL1Agent(final TCL2JMXClient jmxTCClient, final L2ClientID clientID) throws ConfigurationException {
		super("org.terracotta.Terracotta-Clients", "1.0.2");
		this.clientID = clientID;
		this.jmxTCClient = jmxTCClient;

		//this.metricsWorker = new MetricsBufferingWorker(5000, new MetricsFetcher(jmxTCClient));
		//metricsWorker.startAndMoveOn();
	}

	@Override
	public String getComponentHumanLabel() {
			return String.format("%s", clientID.getRemoteAddress());
	}

	@Override
	public void pollCycle() {
		System.out.println(String.format("New Relic Agent[%s] - Pushing L2 Metrics to NewRelic Cloud", getComponentHumanLabel()));
	}
}