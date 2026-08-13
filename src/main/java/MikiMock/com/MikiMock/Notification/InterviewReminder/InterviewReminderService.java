package MikiMock.com.MikiMock.Notification.InterviewReminder;

import MikiMock.com.MikiMock.Interview.Repository.InterviewRepository;
import MikiMock.com.MikiMock.Interview.entity.InterviewSchedule;
import MikiMock.com.MikiMock.Interview.entity.ScheduleStatus;
import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.dto.NotificationType;
import MikiMock.com.MikiMock.Notification.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewReminderService {

    private final InterviewRepository interviewRepository;

    private final NotificationProducer notificationProducer;

    public void sendUpcomingInterviewReminders() {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime next20Minutes =
                now.plusMinutes(20);

        List<InterviewSchedule> interviews =
                interviewRepository.findByscheduledTimeBetweenAndStatus(
                        now,
                        next20Minutes,
                        ScheduleStatus.SCHEDULED
                );

        log.info(
                "Upcoming interviews found: {}",
                interviews.size()
        );

        for (InterviewSchedule interview : interviews) {

            sendReminder(interview);

        }
    }

    private void sendReminder(
            InterviewSchedule interview
    ) {

        NotificationEvent event =NotificationEvent.builder()

                        .notificationType(
                                NotificationType.INTERVIEW_REMINDER
                        )

                        .toEmail(
                                interview.getUser().getEmail()
                        )

                        .subject(
                                "Interview Reminder"
                        )

                        .body(
                                """
                                Hello %s,

                                This is a reminder that your interview is scheduled in less than 20 minutes.

                                Interview Time:
                                %s

                                Please login to MikiMock a few minutes before the scheduled time.

                                Best of luck!

                                Team MikiMock
                                """
                                        .formatted(
                                                interview.getUser().getFullName(),
                                                interview.getScheduledTime()
                                        )
                        )

                        .build();

        notificationProducer.publish(event);

        log.info(
                "Reminder sent to {}",
                interview.getUser().getEmail()
        );
    }

}
