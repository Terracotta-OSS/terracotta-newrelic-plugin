package com.terracotta.nrplugin.test;

import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jeff on 9/9/2014.
 */
public class JsonPathTest {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	//	@Test
	public void testCacheStats() throws IOException {
		String json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("cache-details.json"));
		JSONArray jsonArray = JsonPath.read(json, "$[*]");
		JSONArray objects = JsonPath.read(jsonArray, "$[*].name");
		Set<String> caches = new HashSet<String>();
		Map<String, JSONArray> dataByCacheName = new ConcurrentHashMap<String, JSONArray>();

		for (Object object : objects) {
			caches.add((String) object);
		}
		for (String cache : caches) {
			JSONArray cacheData = JsonPath.read(jsonArray, "$[?]", Filter.filter(Criteria.where("name").is(cache)));
			dataByCacheName.put(cache, cacheData);
			Object o = JsonPath.read(cacheData, "$[*].attributes.InMemoryMissCount");
			Object values = JsonPath.read(cacheData, "$[?].attributes.InMemoryMissCount", Filter.filter(Criteria.where("name").is(cache)));
			log.info("Done.");
		}

	}


	//	@Test
	public void testServerStats() throws IOException {
		String json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("4.0/server-statistics.json"));
		JSONArray jsonArray = JsonPath.read(json, "$[*]");
		JSONArray objects = JsonPath.read(jsonArray, "$[*].sourceId");
		Set<String> servers = new HashSet<String>();
		Map<String, JSONArray> dataByCacheName = new ConcurrentHashMap<String, JSONArray>();

		for (Object object : objects) {
			servers.add((String) object);
		}
		for (String cache : servers) {
			JSONArray cacheData = JsonPath.read(jsonArray, "$[?]", Filter.filter(Criteria.where("sourceId").is(cache)));
			dataByCacheName.put(cache, cacheData);
//			Object o = JsonPath.read(cacheData, "$[*].attributes.InMemoryMissCount");
			JSONArray values = JsonPath.read(cacheData, "$[?].statistics.OffheapMaxSize", Filter.filter(Criteria.where("sourceId").is(cache)));
			for (Object value : values) {
				log.info("" + value);
			}
		}

	}

	@Test
	public void testSpecialMetrics() throws IOException {
		String json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("4.0/topologies.json"));
		JSONArray clientEntities = JsonPath.read(json, "$[*].clientEntities");
		JSONArray array = (JSONArray) clientEntities.get(0);
		log.info("Found " + array.size() + " client(s).");

		String serverName = "server1";
		JSONArray attributes = JsonPath.read(json, "$[*].serverGroupEntities.servers.attributes");
		JSONArray stateArray = JsonPath.read(attributes, "$[?].State", Filter.filter(Criteria.where("Name").is(serverName)));
		String state = (String) stateArray.get(0);
		log.info("State: " + state + " : " + toStateCode(state));
	}

	private int toStateCode(String stateString) {
		if (stateString.startsWith("ACTIVE")) return 0;
		else if (stateString.startsWith("PASSIVE")) return 2;
		else if (stateString.startsWith("INITIALIZING")) return 4;
		else return 8;
	}

}
