package com.danovich.platform.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller-parameter marker. On a {@code UUID} parameter it resolves to the id
 * of the authenticated user (the JWT subject). See {@link CurrentUserArgumentResolver}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
