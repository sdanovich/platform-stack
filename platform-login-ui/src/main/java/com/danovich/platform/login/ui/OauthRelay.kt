package com.danovich.platform.login.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Hands a GitHub OAuth redirect code from the host Activity's intent (the
 * {@code <scheme>://oauth?code=…} deep link) to the login screen. The app forwards
 * the code via [deliver] from onCreate/onNewIntent; the login screen collects it.
 */
object OauthRelay {
    private val _githubCode = MutableStateFlow<String?>(null)
    val githubCode: StateFlow<String?> = _githubCode

    fun deliver(code: String) {
        _githubCode.value = code
    }

    fun consume() {
        _githubCode.value = null
    }
}
