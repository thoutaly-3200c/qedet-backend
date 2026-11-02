package q.qedet.auth.dto;

/**
 * Verified Google identity claims taken from the OAuth2 user response.
 * Never populated from untrusted frontend input.
 */
public record GoogleIdentityClaims(
        String subject,
        String email,
        String name,
        String givenName,
        String familyName,
        String pictureUrl
) {
    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        String given = givenName == null ? "" : givenName.trim();
        String family = familyName == null ? "" : familyName.trim();
        String combined = (given + " " + family).trim();
        if (!combined.isEmpty()) {
            return combined;
        }
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email.trim();
    }
}
