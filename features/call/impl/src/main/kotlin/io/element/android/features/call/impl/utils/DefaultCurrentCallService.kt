/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.utils

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.features.call.api.CurrentCall
import io.element.android.features.call.api.CurrentCallService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultCurrentCallService : CurrentCallService {
    init {
        CallServiceHolder.instance = this
    }
    override val currentCall = MutableStateFlow<CurrentCall>(CurrentCall.None)
    
    private val _callEnded = MutableSharedFlow<Unit>()
    val callEnded: SharedFlow<Unit> = _callEnded.asSharedFlow()

    fun onCallStarted(call: CurrentCall) {
        currentCall.value = call
    }

    fun onCallEnded() {
        currentCall.value = CurrentCall.None
        _callEnded.tryEmit(Unit)
    }
}
