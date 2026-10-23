package com.tpobuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring Boot version of TPO Buddy.
 * This class replaces the old embedded Tomcat ServerRunner.
 */
@SpringBootApplication
public class TpoBuddyApplication {
    public static void main(String[] args) {
        SpringApplication.run(TpoBuddyApplication.class, args);
    }
}
