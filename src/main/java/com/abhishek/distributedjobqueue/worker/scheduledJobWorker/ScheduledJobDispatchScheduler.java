package com.abhishek.distributedjobqueue.worker.scheduledJobWorker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobDispatchScheduler {

    private final ScheduledJobScheduler scheduledJobScheduler;

    /**
     * Periodically scans for scheduled jobs whose execution
     * time has arrived and dispatches them into the normal job queue.
     */
    @Scheduled(fixedDelay = 5000)
    public void dispatchScheduledJobs() {

        boolean dispatchedAny = false;

        while (scheduledJobScheduler.dispatchDueJobs()) {
            dispatchedAny = true;
        }

        if (dispatchedAny) {
            log.info("Finished dispatching all due scheduled jobs.");
        }
    }
}