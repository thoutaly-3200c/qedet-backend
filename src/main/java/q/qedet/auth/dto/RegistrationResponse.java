package q.qedet.auth.dto;

import java.util.UUID;

public record RegistrationResponse(
        boolean success,
        String message,
        Data data
) {
    public record Data(
            UUID userId,
            String email,
            String nextStep
    ) {
    }
}