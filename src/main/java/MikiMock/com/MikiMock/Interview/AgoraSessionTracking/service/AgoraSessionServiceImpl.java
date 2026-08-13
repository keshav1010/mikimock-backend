package MikiMock.com.MikiMock.Interview.AgoraSessionTracking.service;


import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity.AgoraSession;
import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity.AgoraSessionClosedBy;
import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity.AgoraSessionStatus;
import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.repository.AgoraSessionRepository;
import MikiMock.com.MikiMock.Interview.entity.*;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgoraSessionServiceImpl
        implements AgoraSessionServiceInt {

    private final AgoraSessionRepository agoraSessionRepository;
    private final RoomRepository roomRepository;

    @Override
    public AgoraSession startSession(Room room) {

        agoraSessionRepository.findByRoomAndStatus(room, AgoraSessionStatus.ACTIVE)
                .ifPresent(session -> {

                    throw new BusinessException(
                            "Active Agora session already exists."
                    );

                });

        AgoraSession session =
                AgoraSession.builder()
                        .room(room)
                        .channelName(room.getRoomCode())
                        .startedAt(LocalDateTime.now())
                        .status(AgoraSessionStatus.ACTIVE)
                        .participantMinutes(0L)
                        .build();

        session =
                agoraSessionRepository.save(session);

        log.info(
                "Agora session started | roomCode={}",
                room.getRoomCode()
        );

        return session;
    }

    @Override
    @Transactional
    public void endSession(
            Room room,
            AgoraSessionClosedBy closedBy
    ) {

        AgoraSession session =
                agoraSessionRepository
                        .findByRoomAndStatus(
                                room,
                                AgoraSessionStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "No active Agora session found."
                                )
                        );

        LocalDateTime endTime = LocalDateTime.now();

        session.setEndedAt(endTime);

        session.setStatus(
                AgoraSessionStatus.COMPLETED
        );

        session.setClosedBy(closedBy);

        long durationMinutes =
                Duration.between(
                        session.getStartedAt(),
                        endTime
                ).toMinutes();

        if (durationMinutes < 1) {
            durationMinutes = 1;
        }

        /*
         * Agora bills per participant.
         *
         * For now we assume
         * 2 participants.
         */
        session.setParticipantMinutes(
                durationMinutes * 2
        );

        agoraSessionRepository.save(session);

        log.info(
                "Agora session closed | roomCode={} | participantMinutes={} | closedBy={}",
                room.getRoomCode(),
                session.getParticipantMinutes(),
                closedBy
        );
    }

    @Override
    public List<AgoraSession> getActiveSessions() {

        return agoraSessionRepository.findByStatus(
                AgoraSessionStatus.ACTIVE
        );
    }

    @Override
    @Transactional
    public void closeExpiredSessions() {

        LocalDateTime expiry =
                LocalDateTime.now().minusMinutes(60);

        List<AgoraSession> sessions =
                agoraSessionRepository
                        .findByStatusAndStartedAtBefore(
                                AgoraSessionStatus.ACTIVE,
                                expiry
                        );

        for (AgoraSession session : sessions) {

            endSession(
                    session.getRoom(),
                    AgoraSessionClosedBy.SCHEDULER
            );

            Room room = session.getRoom();

            room.setStatus(RoomStatus.COMPLETED);

            room.setEndedAt(LocalDateTime.now());

            roomRepository.save(room);

            log.info(
                    "Scheduler closed room {}",
                    room.getRoomCode()
            );
        }
    }

    @Override
    public void closeTodaySessions() {

        throw new UnsupportedOperationException(
                "Will implement in next step."
        );
    }
}