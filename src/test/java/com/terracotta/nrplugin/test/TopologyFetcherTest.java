package com.terracotta.nrplugin.test;

import com.terracotta.nrplugin.app.RestConfig;
import com.terracotta.nrplugin.pojo.tmc.Topologies;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 9:15 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RestConfig.class})
public class TopologyFetcherTest {

//    final Logger log = LoggerFactory.getLogger(this.getClass());
//
//    @Autowired
//    TopologyFetcher topologyFetcher;
//
//    @Test
//    public void test() {
//        log.info("Getting topologies...");
//        List<Topologies> topologies = topologyFetcher.getTopologies();
//        log.info(topologies.toString());
//    }

}
