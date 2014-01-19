package com.newrelic.plugins.terracotta.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PluginConfig {
	private static Logger log = LoggerFactory.getLogger(PluginConfig.class);

	public static final String CONFIGPATH_DEFAULT = "plugin.properties";
	public static final String CONFIGPATH_ENVPROP = "plugin.config.path";

	//singleton instance
	private static PluginConfig instance;

	private final Properties properties;

	private PluginConfig(String propertyFile) {
		this.properties = loadProperties(propertyFile);
	}

	public static PluginConfig getInstance() {
		if (instance == null)
		{
			synchronized(PluginConfig.class) {  //1
				if (instance == null){
					try {
						String location = null;
						if(null != System.getProperty(CONFIGPATH_ENVPROP)){
							location = System.getProperty(CONFIGPATH_ENVPROP);
							log.info(CONFIGPATH_ENVPROP + " environment property specified: Loading application configuration from " + location);
						}

						if(null == location){
							log.info("Loading application configuration from classpath " + CONFIGPATH_DEFAULT);
							location = CONFIGPATH_DEFAULT;
						}

						instance = new PluginConfig(location);
					} catch (Exception e) {
						log.error("Could not load the property file", e);
					}
				}
			}
		}
		return instance;
	}

	public String getProperty(String key){
		if(log.isDebugEnabled())
			log.debug("Getting key:" + key);

		String val = System.getProperty(key);
		if (val == null)
			val = properties.getProperty(key);

		if(log.isDebugEnabled())
			log.debug("value:" + val);

		return (val == null)?null:val.trim();
	}
	
	public Boolean getPropertyAsBoolean(String key){
		String val = getProperty(key);
		if (val == null)
			return null;

		return Boolean.parseBoolean(val);
	}

	public Integer getPropertyAsInt(String key){
		String val = getProperty(key);
		if (val == null)
			return null;

		try{
			return Integer.parseInt(val);
		} catch (NumberFormatException nfe){
			return null;
		}
	}

	public Long getPropertyAsLong(String key){
		String val = getProperty(key);
		if (val == null)
			return null;

		try{
			return Long.parseLong(val);
		} catch (NumberFormatException nfe){
			return null;
		}
	}

	public String getProperty(String key, String defaultVal){
		String val = getProperty(key);
		if (val == null)
			val = defaultVal;
		return val;
	}

	public Long getPropertyAsLong(String key, long defaultVal){
		Long val = getPropertyAsLong(key);
		if (val == null)
			return defaultVal;
		return val;
	}

	public Integer getPropertyAsInt(String key, int defaultVal){
		Integer val = getPropertyAsInt(key);
		if (val == null)
			return defaultVal;
		return val;
	}

	public Boolean getPropertyAsBoolean(String key, boolean defaultVal){
		Boolean val = getPropertyAsBoolean(key);
		if (val == null)
			return defaultVal;
		return val;
	}

	private Properties loadProperties(final String location) {
		Properties props = new Properties();
		InputStream inputStream = null;
		if(null != location){
			try {
				if(location.indexOf("file:") > -1){
					inputStream = new FileInputStream(location.substring("file:".length()));
				} else {
					inputStream = this.getClass().getClassLoader().getResourceAsStream(location);
				}

				if (inputStream == null) {
					throw new FileNotFoundException("Property file '" + location
							+ "' not found in the classpath");
				}

				props.load(inputStream);
			} catch (IOException e) {
				log.error("Unexpected error...", e);
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
					log.error("Unexpected error...", e);
				}
			}
		}
		return props;
	}
}
