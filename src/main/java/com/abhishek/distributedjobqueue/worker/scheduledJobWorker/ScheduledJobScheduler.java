package com.abhishek.distributedjobqueue.worker.scheduledJobWorker;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.scheduledjob.enums.ScheduledJobStatus;
import com.abhishek.distributedjobqueue.job.scheduledjob.repository.ScheduledJobRepository;
import com.abhishek.distributedjobqueue.job.service.JobService;
import com.abhishek.distributedjobqueue.job.scheduledjob.entity.ScheduledJob;
import com.abhishek.distributedjobqueue.job.scheduledjob.service.ScheduledJobService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "scheduler-worker")
public class ScheduledJobScheduler {

    private final ScheduledJobService scheduledJobService;
    private final ScheduledJobRepository scheduledJobRepository;
    private final JobService jobService;

    @Value("${server.port}")
    private String port;

    public boolean dispatchDueJobs() {

        List<ScheduledJob> scheduledJobs =
                scheduledJobService.claimDueJobs();

        if (scheduledJobs.isEmpty()) {
            return false;
        }

        log.info("[Scheduler:{}] Found {} due scheduled jobs.",
                port,
                scheduledJobs.size());

        for (ScheduledJob scheduledJob : scheduledJobs) {

            log.info(
                    "[Scheduler:{}] Dispatching scheduled job {}",
                    port,
                    scheduledJob.getId()
            );

            try {

                Job job = new Job();

                job.setType(scheduledJob.getType());
                job.setPayload(scheduledJob.getPayload());
                job.setPriority(scheduledJob.getPriority());

                jobService.createJob(job);

                onDispatchSuccess(job.getId());

                scheduledJobService.markCompleted(
                        scheduledJob.getId()
                );

                log.info(
                        "[Scheduler:{}] Scheduled job {} dispatched.",
                        port,
                        scheduledJob.getId()
                );

            } catch (Exception e) {

                log.error(
                        "[Scheduler:{}] Failed to dispatch scheduled job {}",
                        port,
                        scheduledJob.getId(),
                        e
                );

                scheduledJobService.retryOrStuck(
                        scheduledJob.getId()
                );
            }
        }

        return true;
    }

    @Transactional
    public void onDispatchSuccess(UUID id) {

        ScheduledJob job = scheduledJobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found!"));

        job.setLastRunAt(LocalDateTime.now());

        job.setExecutionCount(job.getExecutionCount() + 1);

        switch (job.getScheduleType()) {

            case ONE_TIME -> {
                job.setEnabled(false);
                job.setStatus(ScheduledJobStatus.COMPLETED);
            }

            case FIXED_DELAY -> {

                if (job.getMaxExecutions() != null &&
                        job.getExecutionCount() >= job.getMaxExecutions()) {

                    job.setEnabled(false);
                    job.setStatus(ScheduledJobStatus.COMPLETED);

                } else {

                    job.setNextRunAt(
                            LocalDateTime.now()
                                    .plusSeconds(job.getRepeatIntervalSeconds())
                    );

                    job.setStatus(ScheduledJobStatus.PENDING);
                }
            }

            case FIXED_RATE -> {

                job.setExecutionCount(job.getExecutionCount() + 1);
                job.setLastRunAt(LocalDateTime.now());

                if (job.getMaxExecutions() != null &&
                        job.getExecutionCount() >= job.getMaxExecutions()) {

                    job.setEnabled(false);
                    job.setStatus(ScheduledJobStatus.COMPLETED);

                } else {

                    // Advance from the previous scheduled time, not now.
                    job.setNextRunAt(
                            job.getNextRunAt()
                                    .plusSeconds(job.getRepeatIntervalSeconds())
                    );

                    job.setStatus(ScheduledJobStatus.PENDING);
                }
            }

//            case CRON -> {
//
//                job.setExecutionCount(job.getExecutionCount() + 1);
//                job.setLastRunAt(LocalDateTime.now());
//
//                if (job.getMaxExecutions() != null &&
//                        job.getExecutionCount() >= job.getMaxExecutions()) {
//
//                    job.setEnabled(false);
//                    job.setStatus(ScheduledJobStatus.COMPLETED);
//
//                } else {
//
//                    CronExpression cron =
//                            CronExpression.parse(job.getCronExpression());
//
//                    job.setNextRunAt(
//                            cron.next(LocalDateTime.now())
//                    );
//
//                    job.setStatus(ScheduledJobStatus.PENDING);
//                }
//            }
        }

        scheduledJobRepository.save(job);
    }
}