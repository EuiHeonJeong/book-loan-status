package com.woori.library.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 로컬 개발 기본값은 Vite dev server. 배포 환경에서는 APP_FRONTEND_BASE_URL로 실제 프론트 도메인(https)을 지정한다.
    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 쿠키 기반 더블서밋 CSRF 보호는 프론트(Vercel)와 백엔드(my-library.org)가 서로 다른
            // 도메인이라 구조적으로 성립하지 않는다 — 백엔드가 발급하는 XSRF-TOKEN 쿠키를 다른
            // 도메인의 JS는 애초에 읽을 수 없어(document.cookie가 도메인별로 격리됨) 헤더로 되돌려
            // 보낼 방법이 없다. 대신 CORS를 우리 프론트 origin 하나로만 엄격히 제한하고(아래
            // corsConfigurationSource) 자격 증명(쿠키) 필수로 묶어서, 다른 사이트發 요청은 브라우저의
            // CORS 정책 자체가 막아준다 — 이 조합으로 CSRF를 대체한다.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // CORS preflight(OPTIONS)는 자격 증명(쿠키) 없이 오므로, 인증을 요구하면 브라우저가
                // "preflight에 리다이렉트는 허용되지 않는다"며 실제 요청 자체를 막아버린다.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .defaultSuccessUrl(frontendBaseUrl + "/dashboard", true))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendBaseUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
