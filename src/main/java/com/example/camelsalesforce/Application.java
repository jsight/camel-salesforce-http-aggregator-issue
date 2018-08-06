package com.example.camelsalesforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.camelsalesforce.config.SalesforceConfig;

@SpringBootApplication
@EnableConfigurationProperties({SalesforceConfig.class})
public class Application {

    /**
     * Main method to start the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}