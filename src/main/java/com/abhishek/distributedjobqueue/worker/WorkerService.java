package com.abhishek.distributedjobqueue.worker;

import com.abhishek.distributedjobqueue.execution.service.JobExecutor;
import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.service.JobService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "worker")
public class WorkerService {

    private final JobExecutor jobExecutor;
    private final JobService jobService;
    private final ThreadPoolTaskExecutor workerTaskExecutor;
    private final Semaphore workerSlots;

    public WorkerService(
            JobExecutor jobExecutor,
            JobService jobService,
            @Qualifier("workerTaskExecutor")
            ThreadPoolTaskExecutor workerTaskExecutor,
            Semaphore workerSlots
    ) {
        this.jobExecutor = jobExecutor;
        this.jobService = jobService;
        this.workerTaskExecutor = workerTaskExecutor;
        this.workerSlots = workerSlots;
    }

    @Value("${server.port}")
    private String port;

    private int batchSize;

    public boolean processNextJob() {

        List<Job> jobs = jobService.claimPendingJobs();

        if (jobs.isEmpty()) {
            return false;
        }

        log.info(
                "[Worker:{}] Claimed {} jobs | schedulerThread={}",
                port,
                jobs.size(),
                Thread.currentThread().getName()
        );

        for (Job job : jobs) {

            try {
                workerSlots.acquire();

                log.info(
                        "[Worker:{}] Submitting job {} | schedulerThread={} | availableSlots={}",
                        port,
                        job.getId(),
                        Thread.currentThread().getName(),
                        workerSlots.availablePermits()
                );

                try {

                    workerTaskExecutor.execute(() -> {

                        String threadName = Thread.currentThread().getName();
                        long start = System.currentTimeMillis();

                        log.info(
                                "[Worker:{}] START job {} | thread={}",
                                port,
                                job.getId(),
                                threadName
                        );

                        try {

                            jobExecutor.execute(job);

                            jobService.markCompleted(job.getId());

                            long duration =
                                    System.currentTimeMillis() - start;

                            log.info(
                                    "[Worker:{}] COMPLETED job {} | thread={} | duration={}ms",
                                    port,
                                    job.getId(),
                                    threadName,
                                    duration
                            );

                        } catch (Exception e) {

                            long duration =
                                    System.currentTimeMillis() - start;

                            log.error(
                                    "[Worker:{}] FAILED job {} | thread={} | duration={}ms",
                                    port,
                                    job.getId(),
                                    threadName,
                                    duration,
                                    e
                            );

                            try {
                                jobService.retryOrFail(job.getId());
                            } catch (Exception retryException) {
                                log.error(
                                        "[Worker:{}] Could not retry/fail job {}",
                                        port,
                                        job.getId(),
                                        retryException
                                );
                            }

                        } finally {

                            workerSlots.release();

                            log.info(
                                    "[Worker:{}] RELEASED slot for job {} | thread={} | availableSlots={}",
                                    port,
                                    job.getId(),
                                    threadName,
                                    workerSlots.availablePermits()
                            );
                        }
                    });

                } catch (RuntimeException e) {

                    workerSlots.release();

                    log.error(
                            "[Worker:{}] Could not submit job {} to executor",
                            port,
                            job.getId(),
                            e
                    );

                    throw e;
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                log.error(
                        "[Worker:{}] Interrupted while waiting for execution slot for job {}",
                        port,
                        job.getId(),
                        e
                );

                break;
            }
        }

        return true;
    }
}