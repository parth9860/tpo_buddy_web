package com.tpobuddy.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                // Disable CSRF for API routes called via fetch/JSON and file upload
                .ignoringRequestMatchers("/api/**", "/logout", "/resume", "/profile")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .cacheControl(cache -> cache.disable()) // Custom cache-control filter handles protected routes
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(2)
            )
            .authorizeHttpRequests(auth -> auth
                // Static resources & public assets
                .requestMatchers("/assets/**", "/react-dist/**", "/favicon.ico").permitAll()
                // Public views and landing
                .requestMatchers("/", "/index.jsp", "/login.jsp", "/register.jsp", "/forgot-password.jsp", "/react-app").permitAll()
                // Authentication API
                .requestMatchers("/api/auth/**", "/logout").permitAll()
                // Actuator health check
                .requestMatchers("/actuator/**").permitAll()
                // Protected application routes (authenticated session required)
                .requestMatchers("/dashboard", "/student-dashboard.jsp").permitAll() // Controller handles session check and role redirect
                .requestMatchers("/resume", "/resume-analysis.jsp", "/api/resume/**").permitAll()
                .requestMatchers("/ai-buddy", "/ai-buddy.jsp", "/api/ai/**").permitAll()
                .requestMatchers("/interview", "/interview.jsp", "/api/interview/**").permitAll()
                .requestMatchers("/react-portal", "/react-portal.jsp", "/portal", "/react-app").permitAll()
                .requestMatchers("/tests", "/tests.jsp", "/api/tests/**").permitAll()
                .requestMatchers("/jobs", "/jobs.jsp", "/api/jobs/**").permitAll()
                .requestMatchers("/progress", "/progress.jsp").permitAll()
                .requestMatchers("/profile", "/profile.jsp").permitAll()
                .requestMatchers("/faculty", "/faculty-dashboard.jsp", "/api/faculty/**").permitAll()
                .requestMatchers("/admin", "/admin-dashboard.jsp").permitAll()
                // Any other request
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
