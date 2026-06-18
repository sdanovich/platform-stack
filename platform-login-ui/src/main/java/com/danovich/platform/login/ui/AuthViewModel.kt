package com.danovich.platform.login.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val registerMode: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * Drives the platform login screen: email/password + Google + GitHub. It owns no
 * persistence — on success it hands the [AuthResponse] to [onSession] (the app stores
 * it) and emits the signed-in email via [authedEmail] for navigation. The [api] and
 * [onSession] are injected by the host app through [LoginConfig].
 */
class AuthViewModel(
    private val api: AuthApi,
    private val onSession: (AuthResponse) -> Unit
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    private val _authedEmail = MutableStateFlow<String?>(null)
    val authedEmail: StateFlow<String?> = _authedEmail
    fun consumeAuthed() { _authedEmail.value = null }

    fun setEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun setPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun toggleMode() = _state.update { it.copy(registerMode = !it.registerMode, error = null) }
    fun setError(msg: String) = _state.update { it.copy(loading = false, error = msg) }

    fun submitEmail() {
        val s = _state.value
        val email = s.email.trim()
        if (email.isBlank() || !email.contains("@") || s.password.length < 6) {
            setError("Enter a valid email and a password of at least 6 characters.")
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val req = AuthRequest(email, s.password)
                onSuccess(if (s.registerMode) api.register(req) else api.login(req))
            } catch (e: Exception) {
                setError(friendly(e, s.registerMode))
            }
        }
    }

    fun googleSignIn(idToken: String) = socialSignIn { api.google(GoogleAuthRequest(idToken)) }

    fun githubSignIn(code: String) = socialSignIn { api.github(GitHubAuthRequest(code)) }

    private fun socialSignIn(call: suspend () -> AuthResponse) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                onSuccess(call())
            } catch (e: Exception) {
                val msg = e.message ?: ""
                setError(if (msg.contains("503")) "That sign-in method isn't set up yet." else "Sign-in failed — try again.")
            }
        }
    }

    private fun onSuccess(resp: AuthResponse) {
        onSession(resp)
        _state.update { it.copy(loading = false, error = null) }
        _authedEmail.value = resp.email
    }

    private fun friendly(e: Exception, register: Boolean): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("409") -> "That email is already registered — log in instead."
            msg.contains("401") -> "Invalid email or password."
            msg.contains("400") -> "Enter a valid email and a password of at least 6 characters."
            else -> if (register) "Couldn't create the account — check your connection."
            else "Couldn't log in — check your connection."
        }
    }

    companion object {
        /** Compose factory: builds an AuthViewModel bound to the app's api + session sink. */
        fun factory(api: AuthApi, onSession: (AuthResponse) -> Unit): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(api, onSession) as T
            }
    }
}
