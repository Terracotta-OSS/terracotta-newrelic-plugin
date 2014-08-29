package com.terracotta.nrplugin.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/25/14
 * Time: 9:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class Main implements CommandLineRunner {

    static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void run(String... args) {
        log.info("Initializing Terracotta NewRelic Plugin...");
    }

    public static void main(String[] args) {
        log.info("Loading Spring ApplicationContext...");
        SpringApplication app = new SpringApplication(AppConfig.class);
        app.setWebEnvironment(false);
        ApplicationContext ctx = app.run(args);
        log.info("Loaded Spring ApplicationContext.");
    }

}
