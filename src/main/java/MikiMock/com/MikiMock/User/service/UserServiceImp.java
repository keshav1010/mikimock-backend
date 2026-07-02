package MikiMock.com.MikiMock.User.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.Repository.InterviewRepository;
import MikiMock.com.MikiMock.Interview.entity.InterviewSchedule;
import MikiMock.com.MikiMock.User.dto.*;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.entity.UserProfile;
import MikiMock.com.MikiMock.User.entity.UserSkill;
import MikiMock.com.MikiMock.User.repository.UserProfileRepository;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import MikiMock.com.MikiMock.User.repository.UserSkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserServiceImp implements UserService{
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSkillRepository userSkillRepository;
    private final InterviewRepository interviewRepository;




    @Override
    public List<User> getAllUsers() {
//        List<User> response = userRepository.findAll();
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.get();
    }







//    ----------------------------------------------------------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    @Override
    public UserAuthMeResponse getCurrentUser() {

        String correlationId = MDC.get("X-Correlation-Id");
        log.info("Fetching current user profile | correlationId={}", correlationId);

        User user = getAuthenticatedUser();


        return mapToResponse(user);

    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        String correlationId = MDC.get("X-Correlation-Id");

        log.info("before authenticate");

        User user = getAuthenticatedUser();

        UserProfile response = userProfileRepository.findByUser(user).orElseGet(() ->{ UserProfile profile = new UserProfile();
                                                        profile.setUser(user);
                                                        profile.setFullName(request.getFullName());
                                                        profile.setScore(0);
                                                        userProfileRepository.save(profile);
                                                        return profile;
                                                    });

        UserSkill response2 = userSkillRepository.findByUser(user).orElseGet(() ->{ UserSkill profile = new UserSkill();
            profile.setUser(user);
            profile.setSkill_name(request.getSkills());
            userSkillRepository.save(profile);
            return profile;
        });

        response.setFullName(request.getFullName());
        response.setHeadline(request.getHeadline());
        response.setBio(request.getBio());
        response.setCurrentOrganization(request.getCurrentOrganization());
        response.setExperienceYears(request.getExperienceYears());
        response.setCurrentLocation(request.getCurrentLocation());
        response.setCountry(request.getCountry());
        response.setTimezone(request.getTimezone());
        response.setUser(user);



        log.info("before save");


        userProfileRepository.save(response);

        response2.setSkill_name(request.getSkills());
        userSkillRepository.save(response2);

        return mapToProfileResponse(response , response2);

    }

    @Override
    public UserProfileResponse getMyProfile() {
        User user = getAuthenticatedUser();

        UserProfile response = userProfileRepository.findByUser(user).orElseGet(() ->{ UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setFullName("");
            profile.setScore(0);
            userProfileRepository.save(profile);
            return profile;
        });

        UserSkill response2 = userSkillRepository.findByUser(user).orElseGet(() ->{ UserSkill profile = new UserSkill();
            profile.setUser(user);
            profile.setSkill_name("");
            userSkillRepository.save(profile);
            return profile;
        });

        return mapToProfileResponse(response , response2);

    }

    @Override
    public List<InterviewDateResponse> getActivity() {
        User user = getAuthenticatedUser();
        List<InterviewDateResponse> response = interviewRepository.getInterviewDates(user.getId());

        return response;

    }

    @Override
    public Statsdto getMyStats() {
        User user = getAuthenticatedUser();

        List<Object[]> topicObjects = interviewRepository.getTopicStats(user.getId());

        List<Object[]> levelObjects = interviewRepository.getLevelStats(user.getId());

        Map<String, Long> topicStats = new HashMap<>();
        Map<String, Long> levelStats = new HashMap<>();

        for (Object[] row : topicObjects){
            topicStats.put(row[0].toString(),(Long) row[1]);
        }

        for (Object[] row : levelObjects){
            levelStats.put(row[0].toString(),(Long) row[1]);
        }

        return new Statsdto(topicStats , levelStats);

    }

    @Override
    public Page<InterviewListResponse> getMyInterviewList(int page, int size) {
        User user = getAuthenticatedUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledTime").descending());

        Page<InterviewSchedule> interview = interviewRepository.getInterviewList(user.getId(), pageable);

        Page<InterviewListResponse> response = interview.map(interviews -> InterviewListResponse.builder()
                .interviewId(interviews.getUser().getId())
                .topic(interviews.getTopic())
                .level(interviews.getLevel())
                .pertnerEmail(null)
                .problem(null)
                .scheduledTime(interviews.getScheduledTime())
                .status(interviews.getStatus().toString())
                .build()
        );

        return response;
    }


    private UserProfileResponse mapToProfileResponse(UserProfile response, UserSkill response2) {
        Integer rank = userProfileRepository.getRank(response.getScore());



        UserProfileResponse userProfileResponse = UserProfileResponse.builder()
                .fullName(response.getFullName())
                .headline(response.getHeadline())
                .bio(response.getBio())
                .skills(response2.getSkill_name())
                .currentOrganization(response.getCurrentOrganization())
                .experienceYears(response.getExperienceYears())
                .currentLocation(response.getCurrentLocation())
                .score(response.getScore())
                .rank(rank + 1)
                .build();

        return userProfileResponse;
    }


    private UserAuthMeResponse mapToResponse(
            User user
    ) {

        return UserAuthMeResponse.builder()
                .id(user.getId())

                .publicId(user.getPublicId())

                .email(user.getEmail())

                .fullName(user.getFullName())

                .role(user.getRole().name())

                .subscriptionType(
                        user.getSubscriptionType().name()
                )

                .freeInterviewUsed(
                        user.getFreeInterviewUsed()
                )

                .isEmailVerified(
                        user.getIsEmailVerified()
                )

                .createdAt(user.getCreatedAt())

                .build();
    }

    private User getAuthenticatedUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken){
            log.warn("Unauthorized access attempt | correlationId={}",MDC.get("X-Correlation-Id"));
            throw new BusinessException("User not authenticated");
        }
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    throw new BusinessException("User not found");
                });
        return user;
    }

}
