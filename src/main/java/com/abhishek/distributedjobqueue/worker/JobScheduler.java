package com.abhishek.distributedjobqueue.worker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "worker")
@Component
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
public class JobScheduler {
    private final WorkerService workerService;

    @Value("${server.port}")
    private String port;

    @Scheduled(fixedDelayString = "${worker.poll-delay}")
    public void run() {
        int processed = 0;

        log.info("[Worker:{}] Looking for a job...", port);

        while (workerService.processNextJob()) {
            processed++;
        }

        log.info("[Worker:{}] Queue empty. Processed {} jobs in this cycle.", port, processed);
    }
}
