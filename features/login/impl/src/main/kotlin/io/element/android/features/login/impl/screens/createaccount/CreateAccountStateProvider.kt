/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.createaccount

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.core.SessionId

open class CreateAccountStateProvider : PreviewParameterProvider<CreateAccountState> {
    override val values: Sequence<CreateAccountState>
        get() = sequenceOf(
            aCreateAccountState(),
            aCreateAccountState(createAction = AsyncData.Loading()),
            aCreateAccountState(createAction = AsyncData.Failure(RuntimeException("Failed to create account"))),
            aCreateAccountState(
                formState = CreateAccountFormState(
                    username = "testuser",
                    password = "testpassword",
                    confirmPassword = "testpassword"
                )
            ),
        )
}

private fun aCreateAccountState(
    formState: CreateAccountFormState = CreateAccountFormState(),
    homeserverUrl: String = "https://matrix.org",
    createAction: AsyncData<SessionId> = AsyncData.Uninitialized,
) = CreateAccountState(
    formState = formState,
    homeserverUrl = homeserverUrl,
    createAction = createAction,
    eventSink = {}
)
