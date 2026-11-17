package com.portfolio.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Accepts only absolute http(s) URLs (or null/blank). Blocks "javascript:", "data:" and similar
 * schemes that would turn a stored link into an XSS vector when rendered as href/src.
 */
@Documented
@Constraint(validatedBy = HttpUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {
    String message() default "Đường dẫn phải bắt đầu bằng http:// hoặc https://";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
