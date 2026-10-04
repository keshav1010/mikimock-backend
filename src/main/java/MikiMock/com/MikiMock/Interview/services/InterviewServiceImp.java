package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.Repository.InterviewRepository;
import MikiMock.com.MikiMock.Interview.Repository.ProblemBankRepository;
import MikiMock.com.MikiMock.Interview.Z_ProblemAssigner.ProblemAssignmentService;
import MikiMock.com.MikiMock.Interview.dto.CancelScheduledInterview;
import MikiMock.com.MikiMock.Interview.dto.FutureScheduledInterviews;
import MikiMock.com.MikiMock.Interview.dto.JoinInterviewResponse;
import MikiMock.com.MikiMock.Interview.dto.ScheduleRequest;
import MikiMock.com.MikiMock.Interview.entity.*;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.entity.SubscriptionType;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import MikiMock.com.MikiMock.WebSocket.MatchNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewServiceImp implements InterviewService{

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final MatchQueueService matchQueueService;
    private final ProblemBankRepository problemBankRepository;
    private final RoomRepository roomRepository;
    private final MatchNotificationService matchNotificationService;
    private final ProblemAssignmentService problemAssignmentService;

    private static final int JOIN_GRACE_MINUTES = 5;


    @Override
    @Transactional
    public ScheduleRequest scheduleInterview(ScheduleRequest request) {
        User user = getAuthenticatedUser();

        if(request.getTime().isBefore(
                LocalDateTime.now().plusMinutes(15)
        )) {

            throw new BusinessException(
                    "Schedule at least 15 mins before"
            );
        }

        boolean alreadyInterviewScheduled = interviewRepository.existsByUserAndScheduledTimeAndStatusNot(user, request.getTime(), ScheduleStatus.CANCELLED);

        if(alreadyInterviewScheduled){
            log.info("Interview already scheduled at that time slot");
            throw new BusinessException("Slot is already booked for your Interview");
        }


        if (user.getSubscriptionType() == SubscriptionType.FREE) {

            // FOR 1ST USER EXPERIENCE
            long usedFreeInterviews = interviewRepository.countByUserAndStatusNot(
                            user,
                            ScheduleStatus.CANCELLED
                    );



            // --------------------------------------------------
            // 3. Topic + time-slot capacity
            // --------------------------------------------------

            if(usedFreeInterviews >= 2) {
                long scheduledCount = interviewRepository.countByTopicAndScheduledTimeAndStatusNot(
                                        request.getTopic(),
                                        request.getTime(),
                                        ScheduleStatus.CANCELLED
                                );


                if (scheduledCount >= 20) {
                    log.info(
                            "Free-user slot capacity reached. Topic: {}, Time: {}, Count: {}",
                            request.getTopic(),
                            request.getTime(),
                            scheduledCount
                    );

                    throw new BusinessException(
                            "100 free spots are filled. Cannot schedule interview for this slot."
                    );
                }


                // --------------------------------------------------
                // 4. FREE user monthly quota
                //    Maximum 5 completed interviews in last 30 days
                // --------------------------------------------------

                LocalDateTime thirtyDaysAgo =
                        LocalDateTime.now().minusDays(30);

                long completedInterviews =
                        interviewRepository
                                .countByUserAndScheduledTimeBetweenAndStatus(
                                        user,
                                        thirtyDaysAgo,
                                        LocalDateTime.now(),
                                        ScheduleStatus.COMPLETED
                                );

                if (completedInterviews >= 5) {

                    log.info(
                            "Free interview quota completed for user {}. Interviews in last 30 days: {}",
                            user.getId(),
                            completedInterviews
                    );

                    throw new BusinessException(
                            "Free interview quota is completed. You have already completed 5 interviews in the last 30 days."
                    );
                }
            }
        }



        InterviewQuestion interviewQuestion = problemAssignmentService.assignProblemToUser(user, request);


        InterviewSchedule interviewSchedule = InterviewSchedule.builder()
                .user(user)
                .topic(request.getTopic())
                .scheduledTime(request.getTime())
                .endTime(request.getTime().plusMinutes(65))
                .level(request.getLevel())
                .status(ScheduleStatus.SCHEDULED)
                .assignedProblem(interviewQuestion)
                .createdAt(LocalDateTime.now())
                .build();

        interviewRepository.save(interviewSchedule);
        return request;
    }


    @Override
    public List<FutureScheduledInterviews> getFutureScheduledInterviews() {
        User user = getAuthenticatedUser();

        List<InterviewSchedule> interviewSchedule = interviewRepository.findFutureSchedule(user.getId());
        List<FutureScheduledInterviews> response = new ArrayList<>();
        for (InterviewSchedule i : interviewSchedule){
            FutureScheduledInterviews futureScheduledInterviews = FutureScheduledInterviews.builder()
                    .id(i.getId())
                    .problem(i.getAssignedProblem().getTitle())
                    .topic(i.getTopic())
                    .level(i.getLevel())
                    .scheduledTime(i.getScheduledTime())
                    .endTime(i.getEndTime())
                    .build();

            response.add(futureScheduledInterviews);
        }

        return response;

    }

    @Override
    public CancelScheduledInterview cancelScheduledInterview(UUID scheduledInterviewId) {
        User user = getAuthenticatedUser();

        InterviewSchedule interviewSchedule = interviewRepository.findById(scheduledInterviewId).orElseThrow(
                () -> new BusinessException("Scheduled Interview not found")
        );
        LocalDateTime now = LocalDateTime.now();

        if(now.isAfter(interviewSchedule.getScheduledTime().minusMinutes(15))) return null;

        interviewSchedule.setStatus(ScheduleStatus.CANCELLED);

        interviewRepository.save(interviewSchedule);
        CancelScheduledInterview cancelScheduledInterview = CancelScheduledInterview.builder()
                .id(scheduledInterviewId)
                .status(ScheduleStatus.CANCELLED)
                .build();
        return cancelScheduledInterview;
    }

    @Transactional
    public void cancelInterviewForMatchmakingTimeout(Long userId) {

        InterviewSchedule schedule = interviewRepository.findActiveJoinedInterviewForUser(userId , ScheduleStatus.JOINED).orElse(null);

        if (schedule == null) {

            log.debug(
                    "No JOINED interview found for matchmaking timeout userId={}",
                    userId
            );

            return;
        }

        /*
         * Race-condition protection.
         *
         * If another process already changed the lifecycle,
         * don't overwrite it.
         */
        if (schedule.getStatus()
                != ScheduleStatus.JOINED) {

            return;
        }

        schedule.setStatus(
                ScheduleStatus.CANCELLED
        );

        interviewRepository.save(schedule);

        log.info(
                "Interview schedule cancelled because matchmaking timed out userId={} scheduleId={}",
                userId,
                schedule.getId()
        );
    }

    @Override
    public boolean hasJoinedInterview(Long userId) {
        return interviewRepository.existsByUserIdAndStatus(
                userId,
                ScheduleStatus.JOINED
        );
    }


    @Override
    @Transactional
    public JoinInterviewResponse joinInterview() {
        User user = getAuthenticatedUser();

        InterviewSchedule schedule = interviewRepository.findActiveSchedule(user.getId()).orElseThrow(() -> new BusinessException("No Active Schedule"));
        Optional<Room> activeRoom = roomRepository.findActiveRoomByUserId(user.getId());

        if (activeRoom.isPresent()) {
            log.info("Active room present");
            return JoinInterviewResponse.builder()
                    .status("ROOM_EXISTS")
                    .roomCode(
                            activeRoom.get().getRoomCode()
                    )
                    .roomMode(activeRoom.get().getRoomMode())
                    .build();
        }


        if (LocalDateTime.now().isAfter(schedule.getScheduledTime().plusMinutes(JOIN_GRACE_MINUTES))) {
            log.warn(
                    "Interview join window expired. interviewId={}, scheduledTime={}, now={}",
                    schedule.getId(),
                    schedule.getScheduledTime(),
                    LocalDateTime.now()
            );
            throw new BusinessException(
                    "The interview join window has expired. Please make sure you join within the 5 minutes of scheduled time."
            );
        }


        matchQueueService.addUserToQueue(
            schedule.getTopic(),
            schedule.getLevel(),
            user.getId()
        );

        schedule.setStatus(ScheduleStatus.JOINED);
        interviewRepository.save(schedule);

        return JoinInterviewResponse.builder()
                .status("Matching")
                .build();
    }



    private User getAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||authentication instanceof AnonymousAuthenticationToken){
            throw new BusinessException("User not authorized");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new BusinessException("User not found"));
    }
}
