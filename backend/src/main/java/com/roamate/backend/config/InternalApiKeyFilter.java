package com.roamate.backend.config;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * BE-1·BE-2가 호출하는 서비스간 엔드포인트(ai-status 갱신, 장소 실시간 상황 조회)는
 * 사용자 JWT가 아니라 이 내부 키로만 보호한다. Spring Security의 authorizeHttpRequests에서는
 * permitAll이지만, 이 필터가 그보다 먼저 걸러낸다.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-Api-Key";
    private static final List<String> PROTECTED_PATTERNS = List.of(
            "/api/schedules/*/ai-status",
            "/api/places/*/condition"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String expectedKey;
    private final ObjectMapper objectMapper;

    public InternalApiKeyFilter(String expectedKey, ObjectMapper objectMapper) {
        this.expectedKey = expectedKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isProtected = PROTECTED_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isProtected && !expectedKey.equals(request.getHeader(HEADER_NAME))) {
            response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error(ErrorCode.UNAUTHORIZED, "내부 서비스 인증이 필요합니다.")));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
