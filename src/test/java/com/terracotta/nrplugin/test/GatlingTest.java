package com.terracotta.nrplugin.test;

//import io.gatling.jsonpath.JsonPath;
//import org.boon.json.implementation.JsonUTF8Parser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import scala.collection.Iterator;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 4/8/14
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class GatlingTest {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Test
    public void test() {
        String json = "[\n" +
                "    {\n" +
                "        \"version\": \"4.1.1\",\n" +
                "        \"agentId\": \"embedded\",\n" +
                "        \"sourceId\": \"2\",\n" +
                "        \"statistics\": {\n" +
                "            \"ReadRate\": 0,\n" +
                "            \"WriteRate\": 0\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"version\": \"4.1.1\",\n" +
                "        \"agentId\": \"embedded\",\n" +
                "        \"sourceId\": \"3\",\n" +
                "        \"statistics\": {\n" +
                "            \"ReadRate\": 0,\n" +
                "            \"WriteRate\": 0\n" +
                "        }\n" +
                "    }\n" +
                "]";
//        JsonUTF8Parser parser = new JsonUTF8Parser();
//        Object parsed = parser.parse(json);
//        JsonPath path = JsonPath.compile("$[*]").right().get();
//        Iterator<Object> rootArray = path.query(parsed);
//        while (rootArray.hasNext()) {
//            Object rootObject = rootArray.next();
//            Map map = (Map) rootObject;
//            log.info("Found " + map.toString());
//
//            JsonPath attributePath = JsonPath.compile("$.version").right().get();
//            Iterator<Object> result = attributePath.query(map);
//            if (result.hasNext()) log.info(result.next().toString());
//        }
    }


}
