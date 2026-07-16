package com.busantrip.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.busantrip.auth.AuthDtos.CsrfResponse;
import com.busantrip.auth.AuthDtos.UserResponse;
import com.busantrip.user.AppUser;
import com.busantrip.user.AppUserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    private static final String SESSION_COOKIE = "BUSANTRIP_SESSION";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private AppUserRepository repository;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void signsUpAndNeverReturnsPassword() {
        SessionCsrf session = csrf();

        post("/api/auth/signup", signupBody(), session)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.email").isEqualTo("traveler@example.com")
                .jsonPath("$.displayName").isEqualTo("부산여행자")
                .jsonPath("$.password").doesNotExist()
                .jsonPath("$.passwordHash").doesNotExist();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        SessionCsrf session = csrf();
        post("/api/auth/signup", signupBody(), session).expectStatus().isCreated();

        post("/api/auth/signup", Map.of(
                "email", "TRAVELER@example.com",
                "password", "Password123!",
                "displayName", "다른사용자"), session)
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void storesBcryptHashInsteadOfPlainPassword() {
        SessionCsrf session = csrf();
        post("/api/auth/signup", signupBody(), session).expectStatus().isCreated();

        AppUser saved = repository.findByEmail("traveler@example.com").block();

        assertThat(saved).isNotNull();
        assertThat(saved.passwordHash()).startsWith("$2");
        assertThat(saved.passwordHash()).doesNotContain("Password123!");
    }

    @Test
    void logsInWithCorrectPasswordAndIssuesHttpOnlySession() {
        SessionCsrf session = signup();

        EntityExchangeResult<UserResponse> result = login(session, "Password123!")
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .returnResult();

        ResponseCookie cookie = result.getResponseCookies().getFirst(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(result.getResponseBody().email()).isEqualTo("traveler@example.com");
    }

    @Test
    void rejectsIncorrectPassword() {
        SessionCsrf session = signup();

        login(session, "WrongPassword!")
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void rejectsProtectedApiWithoutAuthentication() {
        client.get()
                .uri("/api/auth/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    void includesCorsHeadersOnAuthenticationErrors() {
        client.get()
                .uri("/api/auth/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173")
                .expectHeader().valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        "true");
    }

    @Test
    void returnsCurrentUserAfterLogin() {
        SessionCsrf session = signup();
        String authenticatedSession = authenticatedSession(session);

        client.get()
                .uri("/api/auth/me")
                .cookie(SESSION_COOKIE, authenticatedSession)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("traveler@example.com")
                .jsonPath("$.displayName").isEqualTo("부산여행자")
                .jsonPath("$.passwordHash").doesNotExist();
    }

    @Test
    void invalidatesSessionOnLogout() {
        SessionCsrf session = signup();
        String authenticatedSession = authenticatedSession(session);
        SessionCsrf authenticated = new SessionCsrf(
                authenticatedSession,
                session.headerName(),
                session.token(),
                session.csrfCookie());

        post("/api/auth/logout", Map.of(), authenticated)
                .expectStatus().isNoContent();

        client.get()
                .uri("/api/auth/me")
                .cookie(SESSION_COOKIE, authenticatedSession)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void reportsEmailAvailability() {
        SessionCsrf session = signup();

        client.get()
                .uri(uriBuilder -> uriBuilder.path("/api/auth/check-email")
                        .queryParam("email", "traveler@example.com")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.available").isEqualTo(false);
    }

    private SessionCsrf signup() {
        SessionCsrf session = csrf();
        post("/api/auth/signup", signupBody(), session).expectStatus().isCreated();
        return session;
    }

    private String authenticatedSession(SessionCsrf session) {
        EntityExchangeResult<UserResponse> result = login(session, "Password123!")
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .returnResult();
        ResponseCookie changed = result.getResponseCookies().getFirst(SESSION_COOKIE);
        return changed == null ? session.sessionId() : changed.getValue();
    }

    private WebTestClient.ResponseSpec login(SessionCsrf session, String password) {
        return post("/api/auth/login", Map.of(
                "email", "traveler@example.com",
                "password", password), session);
    }

    private WebTestClient.ResponseSpec post(String path, Object body, SessionCsrf session) {
        WebTestClient.RequestBodySpec request = client.post()
                .uri(path)
                .cookie("XSRF-TOKEN", session.csrfCookie())
                .header(session.headerName(), session.token());
        if (session.sessionId() != null) {
            request.cookie(SESSION_COOKIE, session.sessionId());
        }
        return request.bodyValue(body).exchange();
    }

    private SessionCsrf csrf() {
        EntityExchangeResult<CsrfResponse> result = client.get()
                .uri("/api/auth/csrf")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CsrfResponse.class)
                .returnResult();
        ResponseCookie csrfCookie = result.getResponseCookies().getFirst("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        CsrfResponse token = result.getResponseBody();
        assertThat(token).isNotNull();
        return new SessionCsrf(null, token.headerName(), token.token(), csrfCookie.getValue());
    }

    private Map<String, String> signupBody() {
        return Map.of(
                "email", "Traveler@example.com",
                "password", "Password123!",
                "displayName", "부산여행자");
    }

    private record SessionCsrf(String sessionId, String headerName, String token, String csrfCookie) {
    }
}
