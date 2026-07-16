package com.busantrip.auth;

import com.busantrip.auth.AuthDtos.EmailAvailabilityResponse;
import com.busantrip.auth.AuthDtos.LoginRequest;
import com.busantrip.auth.AuthDtos.SignupRequest;
import com.busantrip.security.AuthenticatedUser;
import com.busantrip.user.AppUser;
import com.busantrip.user.AppUserRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<AuthenticatedUser> signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        String displayName = request.displayName().trim();

        return repository.existsByEmail(email)
                .flatMap(exists -> exists
                        ? Mono.error(duplicateEmail())
                        : repository.save(new AppUser(
                                null,
                                email,
                                passwordEncoder.encode(request.password()),
                                displayName,
                                Instant.now())))
                .onErrorMap(DataIntegrityViolationException.class, error -> duplicateEmail())
                .map(this::toPrincipal);
    }

    public Mono<AuthenticatedUser> login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        return repository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(request.password(), user.passwordHash()))
                .switchIfEmpty(Mono.error(new AuthException(
                        "INVALID_CREDENTIALS",
                        "이메일 또는 비밀번호가 올바르지 않습니다.",
                        HttpStatus.UNAUTHORIZED)))
                .map(this::toPrincipal);
    }

    public Mono<EmailAvailabilityResponse> checkEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        return repository.existsByEmail(email)
                .map(exists -> new EmailAvailabilityResponse(email, !exists));
    }

    private AuthenticatedUser toPrincipal(AppUser user) {
        return new AuthenticatedUser(user.id(), user.email(), user.displayName());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AuthException("INVALID_EMAIL", "이메일을 확인해 주세요.", HttpStatus.BAD_REQUEST);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthException duplicateEmail() {
        return new AuthException(
                "EMAIL_ALREADY_EXISTS",
                "이미 사용 중인 이메일입니다.",
                HttpStatus.CONFLICT);
    }
}
