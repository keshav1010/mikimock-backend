package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Interview.Repository.InterviewRepository;
import MikiMock.com.MikiMock.Interview.entity.InterviewSchedule;
import MikiMock.com.MikiMock.Interview.entity.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledInterviewLifecycleScheduler {

    private static final int JOIN_GRACE_MINUTES = 5;

    private static final int NO_SHOW_MINUTES = 65;

    private final InterviewRepository scheduledInterviewRepository;


    /*
     * Runs every 1 minute.
     *
     * Responsible only for scheduled-interview lifecycle.
     *
     * It does NOT perform matchmaking.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void processInterviewLifecycle() {

        LocalDateTime now = LocalDateTime.now();

        log.debug(
                "Scheduled interview lifecycle check started. now={}",
                now
        );

        expireScheduledInterviews(now);

        markExpiredInterviewsAsNoShow(now);

        completeJoinedInterviews(now);
    }


    /*
     * ------------------------------------------------------------
     * SCHEDULED -> EXPIRED
     *
     * Example:
     *
     * scheduled = 20:00
     * deadline  = 20:05
     *
     * after 20:05:
     * SCHEDULED -> EXPIRED
     * ------------------------------------------------------------
     */
    private void expireScheduledInterviews(
            LocalDateTime now
    ) {

        LocalDateTime deadline =
                now.minusMinutes(JOIN_GRACE_MINUTES);

        List<InterviewSchedule> interviews =
                scheduledInterviewRepository
                        .findScheduledInterviewsBefore(
                                deadline,
                                ScheduleStatus.SCHEDULED
                        );

        for (InterviewSchedule interview : interviews) {

            interview.setStatus(
                    ScheduleStatus.EXPIRED
            );

            log.info(
                    "Scheduled interview expired. interviewId={}, scheduledTime={}",
                    interview.getId(),
                    interview.getScheduledTime()
            );
        }
    }


    /*
     * ------------------------------------------------------------
     * EXPIRED -> NO_SHOW
     *
     * Example:
     *
     * scheduled = 20:00
     * no-show deadline = 21:00
     *
     * after 21:00:
     * EXPIRED -> NO_SHOW
     * ------------------------------------------------------------
     */
    private void markExpiredInterviewsAsNoShow(
            LocalDateTime now
    ) {

        LocalDateTime deadline =
                now.minusMinutes(NO_SHOW_MINUTES);

        List<InterviewSchedule> interviews =
                scheduledInterviewRepository
                        .findScheduledInterviewsBefore(
                                deadline,
                                ScheduleStatus.EXPIRED
                        );

        for (InterviewSchedule interview : interviews) {

            interview.setStatus(
                    ScheduleStatus.NO_SHOW
            );

            log.info(
                    "Scheduled interview marked NO_SHOW. interviewId={}, scheduledTime={}",
                    interview.getId(),
                    interview.getScheduledTime()
            );
        }
    }


    /*
     * ------------------------------------------------------------
     * JOINED -> COMPLETED
     *
     * Example:
     *
     * scheduled = 20:00
     * completion deadline = 21:00
     *
     * after 21:00:
     * JOINED -> COMPLETED
     * ------------------------------------------------------------
     */
    private void completeJoinedInterviews(
            LocalDateTime now
    ) {

        LocalDateTime deadline =
                now.minusMinutes(NO_SHOW_MINUTES);

        List<InterviewSchedule> interviews =
                scheduledInterviewRepository
                        .findScheduledInterviewsBefore(
                                deadline,
                                ScheduleStatus.JOINED
                        );

        for (InterviewSchedule interview : interviews) {

            interview.setStatus(
                    ScheduleStatus.COMPLETED
            );

            log.info(
                    "Joined interview marked COMPLETED. interviewId={}, scheduledTime={}",
                    interview.getId(),
                    interview.getScheduledTime()
            );
        }
    }
}