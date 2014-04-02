package com.newrelic.plugins.terracotta;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.jmxclient.TCL2JMXClient;
import org.terracotta.utils.jmxclient.beans.CacheManagerInfo;

public class TCStatsUtil {
	private static Logger log = LoggerFactory.getLogger(TCStatsUtil.class);

	protected final String jmxUserName;
	protected final String jmxPassword;
	protected final String jmxHost;
	protected final int jmxPort;

	private Pattern ehcacheStatsFilterCaches = null;
	private Pattern ehcacheStatsFilterClients = null;
	
	public static final String PATTERN_SEPARATOR = "~~";

	private enum OPERATIONS {
		NOOP,
		ENABLESTATS,
		DISABLESTATS;
	}
	
	private TCStatsUtil(String jmxHost, int jmxPort, String jmxUserName, String jmxPassword, String filterCaches, String filterClients) {
		super();
		this.jmxUserName = jmxUserName;
		this.jmxPassword = jmxPassword;
		this.jmxHost = jmxHost;
		this.jmxPort = jmxPort;

		try{
			if(null != filterCaches && !"".equals(filterCaches.trim())){
				this.ehcacheStatsFilterCaches = Pattern.compile(filterCaches.trim(), Pattern.CASE_INSENSITIVE);
			}
		} catch (Exception exc){
			log.error(String.format("An error occurred while compiling the regex pattern %s defined in filter_caches property", (null != filterCaches)?filterCaches.trim():"null"), exc);
		}
		
		try{
			if(null != filterClients && !"".equals(filterClients.trim())){
				this.ehcacheStatsFilterClients = Pattern.compile(filterClients.trim(), Pattern.CASE_INSENSITIVE);
			}
		} catch (Exception exc){
			log.error(String.format("An error occurred while compiling the regex pattern %s defined in filter_clients property", (null != filterClients)?filterClients.trim():"null"), exc);
		}
	}

	private boolean isPatternMatch(Pattern pat, String name){
		boolean match = false;
		if(null != pat){
			Matcher matcher = pat.matcher(name);
			match = matcher.find();
		}
		return match;
	}

	private void execute(OPERATIONS op, boolean doExecute){
		TCL2JMXClient jmxTCClient = null;
		
		try {
			log.info(String.format("Connecting to JMX Server [%s:%d] with user=%s", jmxHost, jmxPort, jmxUserName));
			jmxTCClient = new TCL2JMXClient(jmxUserName, jmxPassword, jmxHost, jmxPort);

			Map<String, CacheManagerInfo> cacheManagerInfo = jmxTCClient.getCacheManagerInfo();
			Iterator<Entry<String, CacheManagerInfo>> iter = cacheManagerInfo.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, CacheManagerInfo> cmInfoElem = iter.next();
				CacheManagerInfo cmInfo = cmInfoElem.getValue();

				for(String cacheName : cmInfo.getCaches()){
					int cacheStatsClientEnabledCount = 0;
					for(String clientId : cmInfo.getClientMbeansIDs()){
						System.out.println(String.format("Cache stats for cache=[%s] and client=[%s] enabled? %s", 
								cacheName, 
								clientId,
								jmxTCClient.isCacheStatsEnabled(cmInfo.getCmName(), cacheName, clientId)));
						
						if(op != OPERATIONS.NOOP){
							// if regex expressions are null, do nothing.
							if((ehcacheStatsFilterCaches != null || ehcacheStatsFilterClients != null) &&
							   (ehcacheStatsFilterCaches == null || isPatternMatch(ehcacheStatsFilterCaches, cacheName)) &&
							   (ehcacheStatsFilterClients == null || isPatternMatch(ehcacheStatsFilterClients, clientId)))
							{
								if(doExecute){
									//let's have disableStats win in case both are true
									if(op == OPERATIONS.DISABLESTATS){
										System.out.println(String.format("Disabling cache stats for cache=[%s] and client=[%s]", cacheName, clientId));
										jmxTCClient.disableCacheStats(cmInfo.getCmName(), cacheName, clientId);
									} else if(op == OPERATIONS.ENABLESTATS){
										System.out.println(String.format("Enabling cache stats for cache=[%s] and client=[%s]", cacheName, clientId));
										jmxTCClient.enableCacheStats(cmInfo.getCmName(), cacheName, clientId);
									}
								} else {
									if(op == OPERATIONS.DISABLESTATS){
										System.out.println(String.format("NOOP/TEST ONLY: Disabling cache stats for cache=[%s] and client=[%s]", cacheName, clientId));
									} else if(op == OPERATIONS.ENABLESTATS){
										System.out.println(String.format("NOOP/TEST ONLY: Enabling cache stats for cache=[%s] and client=[%s]", cacheName, clientId));
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("An issue happened", e);
		} finally {
			if(null != jmxTCClient)
				jmxTCClient.close();
		}
	}

	public static void main(String[] args) {
		String clientFilter, cacheFilter;
		OPERATIONS op = OPERATIONS.NOOP;
		String jmxUserName;
		String jmxPassword;
		String jmxHost;
		int jmxPort;
		boolean doExecute;
		
		// create Options object
		Options options = new Options();
		options.addOption("help", false, "this message...");
		options.addOption("jmxUser", true, "jmx user");
		options.addOption("jmxPwd", true, "jmx password");
		options.addOption("jmxHost", true, "jmx host");
		options.addOption("jmxPort", true, "jmx port");

		options.addOption("clientFilter", true, "Regular expression for client filter");
		options.addOption("cacheFilter", true, "Regular expression for cache filter");
		options.addOption("enable", false, "Enable stats");
		options.addOption("disable", false, "Disable stats");
		options.addOption("execute", false, "Perform stats-changing operations");
		
		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse( options, args );
			if(line.hasOption("help")){
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "TCStatsUtil", options );
				System.exit(0);
			}
			
			jmxUserName = line.getOptionValue( "jmxUser", "");
			jmxPassword = line.getOptionValue( "jmxPwd", "");
			jmxHost = line.getOptionValue( "jmxHost", "");
			jmxPort = Integer.parseInt(line.getOptionValue( "jmxPort", "0"));
			
			clientFilter = line.getOptionValue( "clientFilter", "");
			cacheFilter = line.getOptionValue( "cacheFilter", "");
			
			if(line.hasOption("disable"))
				op = OPERATIONS.DISABLESTATS;
			
			//enable take over
			if(line.hasOption("enable"))
				op = OPERATIONS.ENABLESTATS;
			
			doExecute = line.hasOption("execute");
		}
		catch( ParseException exp ) {
			// oops, something went wrong
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			return;
		}
		
		TCStatsUtil statsUtil = new TCStatsUtil(jmxHost, jmxPort, jmxUserName, jmxPassword, cacheFilter, clientFilter);
		statsUtil.execute(op, doExecute);
		
		System.out.println("Completed");
		System.exit(0);
	}
}
