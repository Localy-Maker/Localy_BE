package org.example.localy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LocalyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalyApplication.class, args);
    }

}
