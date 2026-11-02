package q.qedet.auth.service;

import org.springframework.stereotype.Component;
import q.qedet.common.exception.BadRequestException;

@Component
public class PasswordPolicy {

    public void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new BadRequestException("Password must include upper, lower, digit, and special characters");
        }
    }
}
