package com.busantrip.config;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerSecurityContextRepository securityContextRepository,
            JsonMapper jsonMapper
    ) {
        CookieServerCsrfTokenRepository csrfRepository = CookieServerCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieName("XSRF-TOKEN");
        return http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/check-email", "/api/auth/csrf")
                        .permitAll()
                        .pathMatchers("/api/system/health")
                        .permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, error) -> writeError(
                                exchange, jsonMapper, HttpStatus.UNAUTHORIZED,
                                "AUTHENTICATION_REQUIRED", "로그인이 필요합니다."))
                        .accessDeniedHandler((exchange, error) -> writeError(
                                exchange, jsonMapper, HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED", "요청을 수행할 권한이 없습니다.")))
                .build();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, JsonMapper mapper, HttpStatus status,
                                  String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = mapper.writeValueAsString(Map.of("code", code, "message", message))
                    .getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(body)));
        } catch (Exception exception) {
            return exchange.getResponse().setComplete();
        }
    }
}
