package com.woori.library.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 로컬 개발 기본값은 Vite dev server. 배포 환경에서는 APP_FRONTEND_BASE_URL로 실제 프론트 도메인(https)을 지정한다.
    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    // 세션 쿠키와 동일한 값을 써야 한다 — CSRF 쿠키(XSRF-TOKEN)만 SameSite=None이 아니면
    // 크로스 도메인 배포(Vercel ↔ my-library.org)에서 프론트가 이 쿠키를 되돌려보내지 못해
    // POST/PUT/DELETE 요청이 전부 403(Invalid CSRF token)으로 거부된다.
    @Value("${server.servlet.session.cookie.same-site:lax}")
    private String cookieSameSite;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean cookieSecure;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // SPA가 세션 쿠키로 인증하므로 CSRF는 끄지 않고, JS가 읽을 수 있는 쿠키에 토큰을 담아
            // 요청 헤더(X-XSRF-TOKEN)로 되돌려받는 표준 방식을 쓴다.
            // CsrfToken은 기본적으로 지연 로딩이라 아무도 .getToken()을 호출하지 않으면(순수 REST API라
            // 서버 렌더링 폼이 없음) 쿠키 자체가 응답에 안 실린다 — 아래 필터로 매 요청마다 강제로 로딩시켜
            // 쿠키가 실제로 발급되게 한다(Spring Security 공식 SPA 연동 가이드 패턴).
            // 기본 핸들러(XorCsrfTokenRequestAttributeHandler)는 BREACH 방지를 위해 토큰을 XOR로
            // 마스킹하는데, 이러면 JS가 쿠키 값을 그대로 읽어 헤더로 되돌려보내는 방식과 안 맞아
            // "Invalid CSRF token"으로 매번 거부된다 — 마스킹 없는 단순 핸들러로 명시적으로 바꾼다.
            .csrf(csrf -> csrf
                .csrfTokenRepository(cookieCsrfTokenRepository())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
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

    private CookieCsrfTokenRepository cookieCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite(cookieSameSite).secure(cookieSecure));
        return repository;
    }

    /** CsrfToken의 지연 로딩을 강제로 해제해 XSRF-TOKEN 쿠키가 실제로 응답에 실리게 한다. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
