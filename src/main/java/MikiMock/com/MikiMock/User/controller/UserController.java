package MikiMock.com.MikiMock.User.controller;


import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import MikiMock.com.MikiMock.User.dto.*;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserAuthMeResponse>> getCurrentUser(){
        log.info("Fetching current user profile | correlationId={}", MDC.get("X-Correlation-Id"));

        UserAuthMeResponse response = userService.getCurrentUser();

        log.info("User profile fetched successfully | correlationId={} | email={}", MDC.get("X-Correlation-Id"),response.getEmail());
        log.info("User Data = {}",response);
        return ResponseUtil.success("User profile fetched Succesfully",response);
    }

    @PostMapping("/update-profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request){
        String correlationId = MDC.get("X-Correlation-Id");
        log.info("User profile update request | correlationId={}",correlationId);

        UserProfileResponse response = userService.updateProfile(request);

        log.info("User profile updated Successfully | correlationId={} ",correlationId);

        return ResponseUtil.success("Profile updated successfully",response);
    }

    @GetMapping("/my-profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(){
        String correlationId = MDC.get("X-Correlation-Id");
        log.info("User profile request | correlationId={}",correlationId);

        UserProfileResponse response = userService.getMyProfile();

        return ResponseUtil.success("Profile fetched",response);

    }

    @GetMapping("/my-activity")
    public ResponseEntity<ApiResponse<List<InterviewDateResponse>>> getActivity(){
        String correlationId = MDC.get("X-Correlation-Id");
        log.info("User activity request | correlationId={}",correlationId);

        List<InterviewDateResponse> response = userService.getActivity();

        return ResponseUtil.success("Activity fetched",response);
    }

    @GetMapping("/my-stats")
    public ResponseEntity<ApiResponse<Statsdto>> getMyStats(){
        String correlationId = MDC.get("X-Correlation-Id");
        log.info("User activity request | correlationId={}",correlationId);

        Statsdto reponse = userService.getMyStats();

        return ResponseUtil.success("Stats fecthed",reponse);
    }

    @GetMapping("my-interviews")
    public ResponseEntity<ApiResponse<Page<InterviewListResponse>>> getMyInterviewList(@RequestParam(defaultValue = "0")  int page , @RequestParam(defaultValue = "10") int size){

        Page<InterviewListResponse> response = userService.getMyInterviewList(page , size);

        return ResponseUtil.success("Interview List fetched succesfully" , response);


    }

    @GetMapping("/getallusers")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers(){
        List<User> response = userService.getAllUsers();

        return ResponseUtil.success("Users fetched" ,response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long Id){

        User user = userService.getUserById(Id);
        return ResponseUtil.success("User fetched" , user);
    }

}
