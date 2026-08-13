package MikiMock.com.MikiMock.Interview.controller;


import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import MikiMock.com.MikiMock.Interview.dto.*;
import MikiMock.com.MikiMock.Interview.dto.JoinRoomResponse;
import MikiMock.com.MikiMock.Interview.services.*;
import MikiMock.com.MikiMock.Interview.Z_ProblemAssigner.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/practice")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;
    private final RoomService roomService;
    private final RoomContextService roomContextService;
    private final ProblemService problemService;
    private final ChangeProblemService changeProblemService;
    private final RoomServiceInt roomServiceInt;


    @PostMapping("/schedule-interview")
    private ResponseEntity<ApiResponse<ScheduleRequest>> scheduleInterview(@Valid @RequestBody ScheduleRequest request){

        ScheduleRequest response = interviewService.scheduleInterview(request);
        return ResponseUtil.success("Interview scheduled",response);
    }
    @GetMapping("/scheduled-future-interviews")
    private ResponseEntity<ApiResponse<List<FutureScheduledInterviews>>> getFutureScheduledInterviews(){

        List<FutureScheduledInterviews> respnse = interviewService.getFutureScheduledInterviews();

        return ResponseUtil.success("Future Interview Fetched",respnse);
    }

    @PostMapping("/join")
    private ResponseEntity<ApiResponse<JoinInterviewResponse>> joinInterview(){

        log.info("Request for Join interview");
        JoinInterviewResponse response = interviewService.joinInterview();

        return ResponseUtil.success("Matched",response);
    }

    @GetMapping("/{roomCode}/token")
    public ResponseEntity<?> generateRoomToken(@PathVariable String roomCode) {

        log.info("Request for generate Room");
        RoomJoinResponse response = roomService.joinRoom(roomCode);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{roomCode}/context")
    public ResponseEntity<ApiResponse<RoomContextResponse>> getRoomContext(@PathVariable String roomCode) {
        log.info("Request for room context");

        RoomContextResponse roomContextResponse = roomContextService.getRoomContext(roomCode);

        return ResponseUtil.success("Room context fetched",roomContextResponse);
    }

    @GetMapping("/{roomCode}/problem")
    public ResponseEntity<ApiResponse<AssignedProblemResponse>> getAssignedProblem(@PathVariable String roomCode, @RequestParam(defaultValue = "DSA") String language
    ) {

        AssignedProblemResponse response = problemService.getAssignedProblem(roomCode, language);

        return ResponseUtil.success("Problem fetched", response);
    }

    @PatchMapping("/{roomCode}/rolechange")
    public ResponseEntity<ApiResponse<RoleChangeResponse>> getRoleChanged(@PathVariable String roomCode, @RequestParam(defaultValue = "JAVA" +
            "")String language) {

        RoleChangeResponse response = roomService.changeRole(roomCode, language);

        return ResponseUtil.success(
                "Role changed successfully",
                response
        );
    }

    @PatchMapping("/{roomCode}/problem/change")
    public ResponseEntity<ApiResponse<AssignedProblemResponse>> changeRoomProblem(@PathVariable String roomCode, @RequestParam(defaultValue = "JAVA")String language) {
        log.info("Room problem change request");

        AssignedProblemResponse response = changeProblemService.changeRoomProblem(roomCode, language);

        return ResponseUtil.success(
                "Room Problem changed successfully",
                response
        );
    }

    @PatchMapping("/problem/change")
    public ResponseEntity<ApiResponse<FutureScheduledInterviews>> changeUserProblem(@RequestParam UUID scheduledInterviewId) {
        log.info("User assigned problem change request : {}",scheduledInterviewId);


        FutureScheduledInterviews response = changeProblemService.changeUserProblem("Java" , scheduledInterviewId);

        return ResponseUtil.success("User assigned Problem changed successfully",response);
    }

    @PatchMapping("/scheduled-interview-cancel")
    public ResponseEntity<ApiResponse<CancelScheduledInterview>> cancelScheduledInterview(@RequestParam UUID scheduledInterviewId) {
        log.info("Cancel scheduled interview request");

        CancelScheduledInterview response = interviewService.cancelScheduledInterview(scheduledInterviewId);

        return ResponseUtil.success(
                "Cancel scheduled interview request successfully",response
        );
    }

    @GetMapping("/{roomCode}/join-room")
    public ResponseEntity<ApiResponse<JoinRoomResponse>> joinRoom(
            @PathVariable String roomCode,
            @RequestParam(defaultValue = "java") String language
    ) {

        JoinRoomResponse response = roomServiceInt.joinExistingRoom(
                        roomCode,
                        language
                );

        return ResponseUtil.success(
                "Room joined successfully",
                response
        );
    }

    @PatchMapping("/{roomCode}/language")
    public ResponseEntity<ApiResponse<Void>> changeLanguage(
            @PathVariable String roomCode,
            @RequestBody ChangeRoomLanguageRequest request
    ) {

        roomServiceInt.changeLanguage(
                roomCode,
                request.getLanguage()
        );

        return ResponseUtil.success(
                        "Language changed successfully",
                        null
        );
    }
}
