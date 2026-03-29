/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.createaccount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.components.form.textFieldState
import io.element.android.libraries.designsystem.modifiers.onTabOrEnterKeyFocusNext
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountView(
    state: CreateAccountState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val autofillManager = LocalAutofillManager.current

    BackHandler {
        autofillManager?.cancel()
        onBackClick()
    }

    val isLoading by remember(state.createAction) {
        derivedStateOf {
            state.createAction is AsyncData.Loading
        }
    }
    val focusManager = LocalFocusManager.current

    fun submit() {
        focusManager.clearFocus(force = true)
        autofillManager?.commit()
        state.eventSink(CreateAccountEvents.Submit)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    BackButton(onClick = {
                        autofillManager?.cancel()
                        onBackClick()
                    })
                },
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(state = scrollState)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            // Title
            IconTitleSubtitleMolecule(
                modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp),
                iconStyle = BigIcon.Style.Default(CompoundIcons.UserProfileAdd()),
                title = stringResource(R.string.screen_create_account_title),
                subTitle = stringResource(R.string.screen_create_account_subtitle, state.homeserverUrl)
            )
            Spacer(Modifier.height(40.dp))
            RegistrationForm(
                state = state,
                isLoading = isLoading,
                onSubmit = ::submit
            )
            // Min spacing
            Spacer(Modifier.height(24.dp))
            // Flexible spacing to keep the submit button at the bottom
            Spacer(modifier = Modifier.weight(1f))
            // Submit
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                ButtonColumnMolecule {
                    Button(
                        text = stringResource(R.string.screen_create_account_submit),
                        showProgress = isLoading,
                        onClick = ::submit,
                        enabled = state.formState.submitEnabled || isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.loginContinue)
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            if (state.createAction is AsyncData.Failure) {
                ErrorDialog(
                    title = stringResource(CommonStrings.dialog_title_error),
                    content = (state.createAction as AsyncData.Failure).error.message ?: "Registration failed",
                    onSubmit = { state.eventSink(CreateAccountEvents.ClearError) }
                )
            }
        }
    }
}

@Composable
private fun RegistrationForm(
    state: CreateAccountState,
    isLoading: Boolean,
    onSubmit: () -> Unit,
) {
    var usernameFieldState by textFieldState(stateValue = state.formState.username)
    var passwordFieldState by textFieldState(stateValue = state.formState.password)
    var confirmPasswordFieldState by textFieldState(stateValue = state.formState.confirmPassword)

    val focusManager = LocalFocusManager.current
    val eventSink = state.eventSink

    Column {
        // Username field
        TextField(
            label = stringResource(R.string.screen_create_account_username_label),
            value = usernameFieldState,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .onTabOrEnterKeyFocusNext(focusManager)
                .testTag(TestTags.loginEmailUsername)
                .semantics {
                    contentType = ContentType.Username
                },
            placeholder = stringResource(R.string.screen_create_account_username_placeholder),
            onValueChange = {
                val sanitized = it.sanitize()
                usernameFieldState = sanitized
                eventSink(CreateAccountEvents.SetUsername(sanitized))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }),
            singleLine = true,
            trailingIcon = if (usernameFieldState.isNotEmpty()) {
                {
                    Box(Modifier.clickable {
                        usernameFieldState = ""
                        eventSink(CreateAccountEvents.SetUsername(""))
                    }) {
                        Icon(
                            imageVector = CompoundIcons.Close(),
                            contentDescription = stringResource(CommonStrings.action_clear),
                            tint = ElementTheme.colors.iconSecondary
                        )
                    }
                }
            } else {
                null
            },
        )
        Spacer(Modifier.height(20.dp))

        // Password field
        var passwordVisible by remember { mutableStateOf(false) }
        if (state.createAction is AsyncData.Loading) {
            passwordVisible = false
        }
        TextField(
            value = passwordFieldState,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .onTabOrEnterKeyFocusNext(focusManager)
                .testTag(TestTags.loginPassword)
                .semantics {
                    contentType = ContentType.Password
                },
            onValueChange = {
                val sanitized = it.sanitize()
                passwordFieldState = sanitized
                eventSink(CreateAccountEvents.SetPassword(sanitized))
            },
            placeholder = stringResource(CommonStrings.common_password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image =
                    if (passwordVisible) CompoundIcons.VisibilityOn() else CompoundIcons.VisibilityOff()
                val description =
                    if (passwordVisible) stringResource(CommonStrings.a11y_hide_password) else stringResource(CommonStrings.a11y_show_password)
                Box(Modifier.clickable { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = description,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
        )
        Spacer(Modifier.height(20.dp))

        // Confirm Password field
        TextField(
            value = confirmPasswordFieldState,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .onTabOrEnterKeyFocusNext(focusManager)
                .semantics {
                    contentType = ContentType.Password
                },
            onValueChange = {
                val sanitized = it.sanitize()
                confirmPasswordFieldState = sanitized
                eventSink(CreateAccountEvents.SetConfirmPassword(sanitized))
            },
            placeholder = stringResource(R.string.screen_create_account_confirm_password_placeholder),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = state.formState.isPasswordMismatch,
            supportingText = if (state.formState.isPasswordMismatch) {
                { Text(stringResource(R.string.screen_create_account_password_mismatch)) }
            } else null,
            trailingIcon = {
                if (confirmPasswordFieldState.isNotEmpty() && !state.formState.isPasswordMismatch) {
                    Icon(
                        imageVector = CompoundIcons.Checkmark(),
                        contentDescription = null,
                        tint = ElementTheme.colors.iconSuccess
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            singleLine = true,
        )
    }
}

private fun String.sanitize(): String {
    return replace("\n", "")
}

@PreviewsDayNight
@Composable
internal fun CreateAccountViewPreview(@PreviewParameter(CreateAccountStateProvider::class) state: CreateAccountState) = ElementPreview {
    CreateAccountView(
        state = state,
        onBackClick = {},
    )
}
