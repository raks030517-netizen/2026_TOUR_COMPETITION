package com.busantrip.auth;

import com.busantrip.auth.AuthDtos.CsrfResponse;
import com.busantrip.auth.AuthDtos.EmailAvailabilityResponse;
import com.busantrip.auth.AuthDtos.LoginRequest;
import com.busantrip.auth.AuthDtos.SignupRequest;
import com.busantrip.auth.AuthDtos.UserResponse;
import com.busantrip.security.AuthenticatedUser;
import com.busantrip.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final ServerSecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            CurrentUserService currentUserService,
            ServerSecurityContextRepository securityContextRepository
    ) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request).map(UserResponse::from);
    }

    @PostMapping("/login")
    public Mono<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            ServerWebExchange exchange
    ) {
        return authService.login(request)
                .flatMap(user -> establishSession(exchange, user)
                        .thenReturn(UserResponse.from(user)));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(ServerWebExchange exchange) {
        return exchange.getSession().flatMap(session -> {
            session.getAttributes().clear();
            return session.invalidate();
        });
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(currentUserService.require(authentication));
    }

    @GetMapping("/check-email")
    public Mono<EmailAvailabilityResponse> checkEmail(
            @RequestParam @Email String email
    ) {
        return authService.checkEmail(email);
    }

    @GetMapping("/csrf")
    public Mono<CsrfResponse> csrf(
            @RequestAttribute(name = "org.springframework.security.web.server.csrf.CsrfToken")
            Mono<CsrfToken> csrfToken
    ) {
        return csrfToken.map(token -> new CsrfResponse(token.getHeaderName(), token.getToken()));
    }

    private Mono<Void> establishSession(ServerWebExchange exchange, AuthenticatedUser user) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextImpl context = new SecurityContextImpl(authentication);
        return exchange.getSession()
                .flatMap(session -> session.changeSessionId()
                        .then(securityContextRepository.save(exchange, context)));
    }
}
