package MikiMock.com.MikiMock.Notification.InterviewReminder;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewReminderScheduler {

    private final InterviewReminderService reminderService;

    /**
     * Runs every hour at HH:45
     *
     * 00:45
     * 01:45
     * ...
     * 23:45
     */

    @Scheduled(cron = "0 45 * * * *")
    public void sendInterviewReminders() {

        log.info("Interview reminder scheduler started.");

        reminderService.sendUpcomingInterviewReminders();

        log.info("Interview reminder scheduler completed.");
    }
}
