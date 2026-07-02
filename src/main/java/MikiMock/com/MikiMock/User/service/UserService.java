package MikiMock.com.MikiMock.User.service;

import MikiMock.com.MikiMock.User.dto.*;
import MikiMock.com.MikiMock.User.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface UserService {
    UserAuthMeResponse getCurrentUser();

    UserProfileResponse updateProfile(@Valid UpdateProfileRequest request);

    UserProfileResponse getMyProfile();

    List<InterviewDateResponse> getActivity();

    Statsdto getMyStats();

    Page<InterviewListResponse> getMyInterviewList(int page, int size);

    List<User> getAllUsers();

    User getUserById(Long id);
}
