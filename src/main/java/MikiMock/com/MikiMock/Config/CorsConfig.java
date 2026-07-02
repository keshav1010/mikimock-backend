package MikiMock.com.MikiMock.Config;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsConfig implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletResponse res =
                (HttpServletResponse) response;

        HttpServletRequest req =
                (HttpServletRequest) request;

        String origin =
                req.getHeader("Origin");

        if (origin != null) {

            res.setHeader(
                    "Access-Control-Allow-Origin",
                    origin
            );

            res.setHeader(
                    "Access-Control-Allow-Credentials",
                    "true"
            );

            res.setHeader(
                    "Access-Control-Allow-Methods",
                    "GET, POST, PUT, DELETE, OPTIONS, PATCH"
            );

            res.setHeader(
                    "Access-Control-Allow-Headers",
                    "*"
            );
        }

        if (
                "OPTIONS".equalsIgnoreCase(
                        req.getMethod()
                )
        ) {

            res.setStatus(
                    HttpServletResponse.SC_OK
            );

            return;
        }

        chain.doFilter(
                request,
                response
        );
    }
}






//import org.springframework.context.annotation.Bean;
//
//import org.springframework.context.annotation.Configuration;
//
//import org.springframework.web.cors.CorsConfiguration;
//
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import org.springframework.web.filter.CorsFilter;
//
//import java.util.List;
//
//@Configuration
//public class CorsConfig {
//
//    @Bean
//    public CorsFilter corsFilter() {
//
//        CorsConfiguration config = new CorsConfiguration();
//
//        config.setAllowCredentials(true);
//
//        config.setAllowedOrigins(
//                List.of(
//                        "http://localhost:5173",
//                        "https://starlit-faun-19b827.netlify.app",
//                        "https://*.ngrok-free.dev"
//                )
//        );
//
//        config.setAllowedHeaders(
//                List.of(
////                        "Authorization",
////                        "Content-Type",
////                        "Accept",
////                        "ngrok-skip-browser-warning"
//                        "*"
//                )
//        );
//        config.setAllowCredentials(true);
//        config.setAllowedMethods(
//                List.of(
//                        "GET",
//                        "POST",
//                        "PUT",
//                        "DELETE",
//                        "PATCH",
//                        "OPTIONS"
//                )
//        );
//
//
//        config.setExposedHeaders(
//                List.of(
//                        "Authorization"
////                        "*"
//                )
//        );
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//
//        source.registerCorsConfiguration(
//                "/**",
//                config
//        );
//
//        return new CorsFilter(source);
//    }
//}