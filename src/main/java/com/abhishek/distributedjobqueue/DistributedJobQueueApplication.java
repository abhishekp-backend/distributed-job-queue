package com.abhishek.distributedjobqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DistributedJobQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedJobQueueApplication.class, args);
    }

}
