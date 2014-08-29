package com.terracotta.nrplugin.app;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 9:17 AM
 * To change this template use File | Settings | File Templates.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.terracotta.nrplugin.rest"})
@PropertySource("classpath:application.properties")
public class RestConfig {

}
