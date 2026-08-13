package MikiMock.com.MikiMock.Security.JWT;

import javax.crypto.SecretKey;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long jwtExpiration;

    private final UserRepository userRepository;

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secretKey.getBytes()
        );
    }

    public String generateToken(UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        Map<String, Object> claims = new HashMap<>();

        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());
        claims.put("publicId", user.getPublicId());
        claims.put("subscriptionType", user.getSubscriptionType().name());
        claims.put("fullName", user.getFullName());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

//    public String generateToken(
//            UserDetails userDetails
//    ) {
//
//        return Jwts.builder()
//
//                .setSubject(
//                        userDetails.getUsername()
//                )
//
//                .setIssuedAt(new Date())
//
//                .setExpiration(
//                        new Date(
//                                System.currentTimeMillis()
//                                        + jwtExpiration
//                        )
//                )
//
//                .signWith(
//                        getSigningKey(),
//                        SignatureAlgorithm.HS256
//                )
//
//                .compact();
//    }

    // Extract Username (Email)
    public String extractUsername(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    // Validate Token
    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {

        final String username =
                extractUsername(token);

        return username.equals(
                userDetails.getUsername()
        ) && !isTokenExpired(token);
    }

    // Check Expiration
    private boolean isTokenExpired(
            String token
    ) {

        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    // Extract Claims
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(
                        getSigningKey()
                )
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}