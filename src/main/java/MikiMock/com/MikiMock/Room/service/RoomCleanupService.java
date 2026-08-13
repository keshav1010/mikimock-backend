package MikiMock.com.MikiMock.Room.service;

import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.dto.ForceLeaveEvent;
import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Interview.entity.RoomStatus;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomCleanupService {

    private final RoomRepository roomRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void forceLeaveExpiredRooms(
            boolean closeRoom
    ) {

        LocalDateTime from =
                LocalDateTime.now().minusMinutes(71);

        LocalDateTime to =
                LocalDateTime.now().minusMinutes(60);

        List<Room> rooms =
                roomRepository.findExpiredRooms(
                        from,
                        to
                );

        if (rooms.isEmpty()) {

            log.info("No expired rooms found.");

            return;
        }

        for (Room room : rooms) {

            log.info(
                    "Sending FORCE_LEAVE for room {}",
                    room.getRoomCode()
            );

            messagingTemplate.convertAndSend(

                    "/topic/room/"
                            + room.getRoomCode()
                            + "/force-leave",

                    ForceLeaveEvent.builder()

                            .eventType("FORCE_LEAVE")

                            .roomCode(
                                    room.getRoomCode()
                            )

                            .reason(
                                    "Interview time exceeded."
                            )

                            .build()
            );

            if (closeRoom) {

                room.setStatus(
                        RoomStatus.COMPLETED
                );

                room.setEndedAt(
                        LocalDateTime.now()
                );

                roomRepository.save(room);

                log.info(
                        "Room {} closed by scheduler.",
                        room.getRoomCode()
                );
            }
        }
    }
}



//
//import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity.AgoraSessionClosedBy;
//import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.service.AgoraSessionServiceInt;
//import MikiMock.com.MikiMock.Room.repository.RoomRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.scheduling.config.FixedRateTask;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class RoomCleanupScheduler {
//    private  final RoomRepository roomRepository;
//    private final AgoraSessionServiceInt agoraSessionService;
//
//
////    @Scheduled(fixedRate = 60000)
////    @Transactional
////    public void completeExpiredRooms(){
////        LocalDateTime now = LocalDateTime.now();
////        roomRepository.updateExpiredRooms(now);
////
////        log.info("Expired rooms updated");
//
////        agoraSessionService.endSession(
////                room,
////                AgoraSessionClosedBy.NORMAL
////        );
////    }
//}
