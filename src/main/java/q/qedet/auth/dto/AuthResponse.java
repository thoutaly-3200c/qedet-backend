package q.qedet.auth.dto;

import java.util.Set;
import java.util.UUID;
import q.qedet.common.model.RegistrationStatus;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        UUID employeeId,
        UUID organizationId,
        String role,
        Set<String> permissions,
        RegistrationStatus registrationStatus,
        String status,
        String nextStep,
        String message,
        String firstName,
        String lastName,
        String email
) {
    public AuthResponse(String accessToken, String refreshToken, UUID userId, UUID employeeId,
                        UUID organizationId, String role, Set<String> permissions) {
        this(accessToken, refreshToken, userId, employeeId, organizationId, role, permissions,
                null, null, null, null, null, null, null);
    }

    public AuthResponse(String accessToken, String refreshToken, UUID userId, UUID employeeId,
                        UUID organizationId, String role, Set<String> permissions,
                        RegistrationStatus registrationStatus) {
        this(accessToken, refreshToken, userId, employeeId, organizationId, role, permissions,
                registrationStatus, null, null, null, null, null, null);
    }

    public AuthResponse(String accessToken, String refreshToken, UUID userId, UUID employeeId,
                        UUID organizationId, String role, Set<String> permissions,
                        RegistrationStatus registrationStatus, String status, String nextStep,
                        String message) {
        this(accessToken, refreshToken, userId, employeeId, organizationId, role, permissions,
                registrationStatus, status, nextStep, message, null, null, null);
    }
}
