package MikiMock.com.MikiMock.Security.Service;

import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "User not found"
                            ));

            return org.springframework.security.core.userdetails.User
                    .builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .build();
        }
}

// Authentication Flow

//Login Request
//    ↓
//AuthenticationManager
//    ↓
//CustomUserDetailsService
//    ↓
//Password Match
//    ↓
//Generate JWT
//    ↓
//Return Token




//Protected API Flow
//Request with JWT
//    ↓
//JWT Filter
//    ↓
//Validate Token
//    ↓
//Set Authentication
//    ↓
//Controller Access