package com.abhishek.distributedjobqueue.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobScheduler {
    private final WorkerService workerService;

    @Value("${server.port}")
    private String port;

    @Scheduled(fixedDelay = 5000)
    public void run() {
        int processed = 0;

        log.info("[Worker:{}] Looking for a job...", port);

        while (workerService.processNextJob()) {
            processed++;
        }

        log.info("[Worker:{}] Queue empty. Processed {} jobs in this cycle.", port, processed);
    }
}
