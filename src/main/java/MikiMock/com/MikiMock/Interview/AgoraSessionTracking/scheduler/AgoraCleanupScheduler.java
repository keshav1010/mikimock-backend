package MikiMock.com.MikiMock.Interview.AgoraSessionTracking.scheduler;

import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.service.AgoraSessionServiceInt;
import MikiMock.com.MikiMock.Room.service.RoomCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgoraCleanupScheduler {

    private final RoomCleanupService roomCleanupService;

    /*
        Every hour at xx:06
     */
    @Scheduled(cron = "0 6 * * * *")
    public void firstCleanup() {

        log.info("Agora Cleanup Scheduler (06)");

        roomCleanupService.forceLeaveExpiredRooms(false);

    }

    /*
        Every hour at xx:10
     */
    @Scheduled(cron = "0 10 * * * *")
    public void secondCleanup() {

        log.info("Agora Cleanup Scheduler (10)");

        roomCleanupService.forceLeaveExpiredRooms(true);

    }

}
