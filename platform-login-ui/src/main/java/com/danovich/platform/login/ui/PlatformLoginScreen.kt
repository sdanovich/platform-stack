package com.danovich.platform.login.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * Reusable email + Google + GitHub sign-in screen. Branding, provider client ids,
 * icons, the auth API, and the session sink all come from [config]. On success it
 * persists via the config's session sink and emits the email through [onAuthenticated].
 */
@Composable
fun PlatformLoginScreen(config: LoginConfig, onAuthenticated: (String) -> Unit) {
    val vm: AuthViewModel = viewModel(factory = AuthViewModel.factory(config.api, config.onSession))
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPassword by remember { mutableStateOf(false) }

    val authed by vm.authedEmail.collectAsState()
    LaunchedEffect(authed) { authed?.let { onAuthenticated(it); vm.consumeAuthed() } }

    val ghCode by OauthRelay.githubCode.collectAsState()
    LaunchedEffect(ghCode) { ghCode?.let { vm.githubSignIn(it); OauthRelay.consume() } }

    val doGoogle = {
        if (config.googleServerClientId.isBlank()) {
            vm.setError("Google sign-in isn't set up yet.")
        } else {
            scope.launch {
                try {
                    val cm = CredentialManager.create(context)
                    // The explicit "Sign in with Google" button flow (always shows the
                    // account chooser), using the Web client id as the server client id.
                    val opt = GetSignInWithGoogleOption
                        .Builder(config.googleServerClientId)
                        .build()
                    val req = GetCredentialRequest.Builder().addCredentialOption(opt).build()
                    val cred = cm.getCredential(context, req).credential
                    if (cred is CustomCredential &&
                        cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        vm.googleSignIn(GoogleIdTokenCredential.createFrom(cred.data).idToken)
                    } else {
                        vm.setError("Unexpected Google credential type.")
                    }
                } catch (e: GetCredentialCancellationException) {
                    vm.setError("Google sign-in canceled.")
                } catch (e: NoCredentialException) {
                    vm.setError("No Google credential. Add an Android OAuth client (package + SHA-1) in Google Cloud, and a test user.")
                } catch (e: Exception) {
                    android.util.Log.e("PlatformLogin", "google sign-in failed", e)
                    vm.setError("Google failed: ${e.javaClass.simpleName} — ${e.message}")
                }
            }
            Unit
        }
    }

    val doGithub = {
        if (config.githubClientId.isBlank()) {
            vm.setError("GitHub sign-in isn't set up yet.")
        } else {
            val url = "https://github.com/login/oauth/authorize?client_id=" +
                config.githubClientId + "&scope=user:email&redirect_uri=" +
                Uri.encode(config.githubRedirectUri)
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                config.appName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                if (state.registerMode) config.createSubtitle else config.signInSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            SocialButton(config.googleIconRes, "Continue with Google", !state.loading) { doGoogle() }
            Spacer(Modifier.height(12.dp))
            SocialButton(config.githubIconRes, "Continue with GitHub", !state.loading) { doGithub() }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    "OR",
                    Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = vm::setEmail,
                label = { Text("Enter your email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = vm::setPassword,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(
                            if (showPassword) "Hide" else "Show",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.submitEmail() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    if (state.loading) "Please wait…"
                    else if (state.registerMode) "Create account" else "Continue with email",
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = { vm.toggleMode() }) {
                Text(
                    if (state.registerMode) "Have an account? Log in"
                    else "New here? Create an account"
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SocialButton(iconRes: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}
