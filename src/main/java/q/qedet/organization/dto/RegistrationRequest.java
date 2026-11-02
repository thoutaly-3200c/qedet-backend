package q.qedet.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        String phone,
        String companyName,
        String country,
        String timezone,
        String previousEmail
) {
    public RegistrationRequest(String name, String email, String password, String phone,
                               String companyName, String country, String timezone) {
        this(name, email, password, phone, companyName, country, timezone, null);
    }
}
