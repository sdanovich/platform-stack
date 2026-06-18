package com.danovich.platform.login.ui

import androidx.annotation.DrawableRes

/**
 * Everything the host app injects into [PlatformLoginScreen]: branding, the social
 * provider client ids (a blank id disables that button), the icon resources, the
 * auth API (the app's own OkHttp-backed Retrofit service), and a session sink the
 * screen invokes with the [AuthResponse] on success so the app can persist it.
 */
data class LoginConfig(
    val appName: String,
    val signInSubtitle: String = "Sign in to continue",
    val createSubtitle: String = "Create your account",
    @DrawableRes val googleIconRes: Int,
    @DrawableRes val githubIconRes: Int,
    /** Google Web OAuth client id used as the Credential Manager serverClientId; blank disables. */
    val googleServerClientId: String,
    /** GitHub OAuth App client id; blank disables. */
    val githubClientId: String,
    /** GitHub OAuth redirect (custom scheme, e.g. "appscheme://oauth"). */
    val githubRedirectUri: String,
    val api: AuthApi,
    val onSession: (AuthResponse) -> Unit,
)
