/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.createaccount

import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.matrix.api.core.SessionId

data class CreateAccountState(
    val formState: CreateAccountFormState,
    val homeserverUrl: String,
    val createAction: AsyncAction<SessionId>,
    val eventSink: (CreateAccountEvents) -> Unit
)

data class CreateAccountFormState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
) {
    val submitEnabled: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && password == confirmPassword

    val isPasswordMismatch: Boolean
        get() = confirmPassword.isNotBlank() && password != confirmPassword
}
