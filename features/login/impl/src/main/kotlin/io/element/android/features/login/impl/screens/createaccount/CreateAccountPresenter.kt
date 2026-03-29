/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.createaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AssistedInject
class CreateAccountPresenter(
    @Assisted private val homeserverUrl: String,
    private val authenticationService: MatrixAuthenticationService,
) : Presenter<CreateAccountState> {
    @AssistedFactory
    interface Factory {
        fun create(homeserverUrl: String): CreateAccountPresenter
    }

    @Composable
    override fun present(): CreateAccountState {
        val coroutineScope = rememberCoroutineScope()
        val createAction: MutableState<AsyncAction<SessionId>> = remember {
            mutableStateOf(AsyncAction.Uninitialized)
        }

        val formState = rememberSaveable {
            mutableStateOf(CreateAccountFormState())
        }

        fun handleEvent(event: CreateAccountEvents) {
            when (event) {
                is CreateAccountEvents.SetUsername -> updateFormState(formState) {
                    copy(username = event.username)
                }
                is CreateAccountEvents.SetPassword -> updateFormState(formState) {
                    copy(password = event.password)
                }
                is CreateAccountEvents.SetConfirmPassword -> updateFormState(formState) {
                    copy(confirmPassword = event.confirmPassword)
                }
                CreateAccountEvents.Submit -> {
                    coroutineScope.submit(formState.value, createAction)
                }
                CreateAccountEvents.ClearError -> createAction.value = AsyncAction.Uninitialized
            }
        }

        return CreateAccountState(
            formState = formState.value,
            homeserverUrl = homeserverUrl,
            createAction = createAction.value,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.submit(formState: CreateAccountFormState, createAction: MutableState<AsyncAction<SessionId>>) = launch {
        createAction.value = AsyncAction.Loading
        // Set the homeserver first, then register
        authenticationService.setHomeserver(homeserverUrl)
            .onSuccess {
                runCatchingExceptions {
                    authenticationService.register(formState.username.trim(), formState.password)
                }.onSuccess { sessionId ->
                    createAction.value = AsyncAction.Success(sessionId)
                }.onFailure { failure ->
                    createAction.value = AsyncAction.Failure(failure)
                }
            }
            .onFailure { failure ->
                createAction.value = AsyncAction.Failure(failure)
            }
    }

    private fun updateFormState(formState: MutableState<CreateAccountFormState>, updateLambda: CreateAccountFormState.() -> CreateAccountFormState) {
        formState.value = updateLambda(formState.value)
    }
}
