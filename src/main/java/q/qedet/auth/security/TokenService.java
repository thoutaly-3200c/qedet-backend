package q.qedet.auth.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import q.qedet.auth.entity.AuthSession;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.AuthSessionRepository;

import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.OrganizationStatus;
import q.qedet.common.model.UserStatus;
import q.qedet.employee.entity.Employee;
import q.qedet.common.exception.UnauthorizedException;


import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {

    public record TokenPair(String accessToken, String refreshToken) {
    }

    private final AuthSessionRepository authSessionRepository;

    public TokenService(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    @Transactional
    public TokenPair createSession(UserAccount user, Employee employee, boolean rememberMe) {
        String accessToken = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        Duration accessDuration = rememberMe ? Duration.ofDays(7) : Duration.ofHours(12);
        Duration refreshDuration = rememberMe ? Duration.ofDays(30) : Duration.ofDays(7);
        AuthSession session = new AuthSession(
                user,
                employee,
                TokenHasher.sha256(accessToken),
                TokenHasher.sha256(refreshToken),
                now.plus(accessDuration),
                now.plus(refreshDuration)
        );
        authSessionRepository.saveAndFlush(session);
        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional
    public void revoke(AuthSession session) {
        session.setRevokedAt(Instant.now());
        authSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public AuthSession authenticate(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new UnauthorizedException("Missing access token");
        }
        AuthSession session = authSessionRepository.findByAccessTokenHash(TokenHasher.sha256(accessToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid access token"));
        if (session.getRevokedAt() != null || session.getAccessTokenExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Expired or revoked access token");
        }
        if (session.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is inactive");
        }
        if (!session.getUser().isEmailVerified()) {
            throw new UnauthorizedException("Email verification required");
        }
        Employee employee = session.getEmployee();
        if (employee != null) {
            if (employee.getStatus() == EmployeeStatus.INACTIVE
                    || employee.getStatus() == EmployeeStatus.SUSPENDED
                    || employee.getStatus() == EmployeeStatus.TERMINATED) {
                throw new UnauthorizedException("Employee is inactive");
            }
            if (employee.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
                throw new UnauthorizedException("Organization is not active");
            }
        }
        return session;
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }
        AuthSession session = authSessionRepository.findByRefreshTokenHash(TokenHasher.sha256(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (session.getRevokedAt() != null || session.getRefreshTokenExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Expired or revoked refresh token");
        }
        session.setRevokedAt(Instant.now());
        authSessionRepository.save(session);
        return createSession(session.getUser(), session.getEmployee(), false);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        boolean revoked = false;
        if (accessToken != null && !accessToken.isBlank()) {
            revoked = authSessionRepository.findByAccessTokenHash(TokenHasher.sha256(accessToken)).map(session -> {
                session.setRevokedAt(Instant.now());
                authSessionRepository.save(session);
                return true;
            }).orElse(false);
        }
        if (!revoked && refreshToken != null && !refreshToken.isBlank()) {
            authSessionRepository.findByRefreshTokenHash(TokenHasher.sha256(refreshToken)).ifPresent(session -> {
                session.setRevokedAt(Instant.now());
                authSessionRepository.save(session);
            });
        }
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        authSessionRepository.revokeAllActiveByUserId(userId, Instant.now());
    }
}
