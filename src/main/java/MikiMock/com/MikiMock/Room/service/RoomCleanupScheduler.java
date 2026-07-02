package MikiMock.com.MikiMock.Room.service;

import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomCleanupScheduler {
    private  final RoomRepository roomRepository;


    @Scheduled(fixedRate = 60000)
    @Transactional
    public void completeExpiredRooms(){
        LocalDateTime now = LocalDateTime.now();
        roomRepository.updateExpiredRooms(now);

        log.info("Expired rooms updated");
    }
}
