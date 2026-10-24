package com.studenthub;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class StudentHubApplication {

    public static void main(String[] args) {
        // All instants are handled in UTC on the server. Each student's own timezone
        // (stored on their profile) is applied explicitly where "today" matters.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(StudentHubApplication.class, args);
    }
}
