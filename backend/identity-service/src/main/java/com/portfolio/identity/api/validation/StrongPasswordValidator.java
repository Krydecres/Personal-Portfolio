package com.portfolio.identity.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        List<String> failed = PasswordPolicy.violations(value);
        if (failed.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        // Never echo the password itself back; only the rules it broke.
        context.buildConstraintViolationWithTemplate("Mật khẩu cần: " + String.join("; ", failed))
                .addConstraintViolation();
        return false;
    }
}
