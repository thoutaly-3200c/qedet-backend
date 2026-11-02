package q.qedet.employee.dto;

public record InviteRequest(
        String email,
        String message,
        String role
) {
}
