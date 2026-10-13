package com.university.sigac.core.security;

/** Tells the JWT filter whether an account is still active. Implemented by the users module. */
@FunctionalInterface
public interface ActiveUserChecker {

    boolean isActive(Long userId);
}
