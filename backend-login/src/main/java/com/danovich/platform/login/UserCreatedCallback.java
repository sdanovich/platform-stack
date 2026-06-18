package com.danovich.platform.login;

import java.util.UUID;

/**
 * Hook invoked once, right after a brand-new user account is created (via email
 * registration or first social sign-in). Lets a consuming app run app-specific
 * setup — e.g. seeding default data — without the login module knowing about it.
 *
 * <p>The default platform bean is a no-op; an app overrides it by declaring its
 * own {@code UserCreatedCallback} bean.
 */
@FunctionalInterface
public interface UserCreatedCallback {

    /**
     * @param userId   the new user's id
     * @param email    the (normalized) email
     * @param provider "LOCAL", "GOOGLE", or "GITHUB"
     */
    void onUserCreated(UUID userId, String email, String provider);
}
