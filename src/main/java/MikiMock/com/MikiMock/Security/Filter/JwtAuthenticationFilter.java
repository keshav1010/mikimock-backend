package MikiMock.com.MikiMock.Security.Filter;

import MikiMock.com.MikiMock.Security.JWT.JwtService;
import MikiMock.com.MikiMock.Security.Service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.
        UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.
        SecurityContextHolder;
import org.springframework.security.core.userdetails.
        UserDetails;
import org.springframework.security.web.authentication.
        WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final CustomUserDetailsService
            userDetailsService;



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
//            log.info("Enters in security-----");


            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String userEmail;


//            log.info("before checking authHeader= {}",authHeader);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
//                log.info(
//                        "Reached after filterChain"
//                );

                return;
            }

            String path = request.getServletPath();
            if (path.equals("/auth/refresh")) {
                filterChain.doFilter(request, response);
                return;
            }

            jwt = authHeader.substring(7);

            try {

                userEmail =
                        jwtService.extractUsername(jwt);

            } catch (ExpiredJwtException e) {

                response.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                response.getWriter()
                        .write("JWT Token Expired");

                return;

            } catch (Exception e) {

                log.error(
                        "Invalid JWT: {}",
                        e.getMessage()
                );

                response.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                response.getWriter()
                        .write("Invalid JWT");

                return;
            }

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authToken);
                }
            }

        String correlationId =
                request.getHeader("X-Correlation-Id");

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        System.out.println("Correlation ID: " + correlationId);
        log.info("Log in filter chain");
        filterChain.doFilter(request, response);
    }
}
