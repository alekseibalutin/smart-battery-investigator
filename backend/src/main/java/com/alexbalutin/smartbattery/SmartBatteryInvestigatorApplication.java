
package com.alexbalutin.smartbattery;

import com.alexbalutin.smartbattery.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class SmartBatteryInvestigatorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SmartBatteryInvestigatorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SmartBatteryInvestigatorApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Smart Battery Investigator started");
    }
}
