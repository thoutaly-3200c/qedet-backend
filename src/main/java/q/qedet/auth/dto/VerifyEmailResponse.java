package q.qedet.auth.dto;

public record VerifyEmailResponse(
        String status,
        String message,
        String nextStep
) {
}
