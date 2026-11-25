package com.homeenergymonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomeEnergyMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeEnergyMonitorApplication.class, args);
    }
}
