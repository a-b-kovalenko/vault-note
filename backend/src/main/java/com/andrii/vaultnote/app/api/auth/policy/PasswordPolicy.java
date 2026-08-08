package com.andrii.vaultnote.app.api.auth.policy;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordPolicy {

  String message() default "Password must contain at least two digits and one alphabetic character";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
