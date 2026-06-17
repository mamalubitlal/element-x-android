/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.avatar.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import timber.log.Timber

@Composable
internal fun ImageAvatar(
    avatarData: AvatarData,
    avatarShape: Shape,
    forcedAvatarSize: Dp?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val size = forcedAvatarSize ?: avatarData.size.dp
    SubcomposeAsyncImage(
        model = avatarData,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(avatarShape)
    ) {
        when (val state = painter.state) {
            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            is AsyncImagePainter.State.Error -> {
                SideEffect {
                    Timber.e(
                        state.result.throwable,
                        "Error loading avatar $state\n${state.result}"
                    )
                }
                InitialLetterAvatar(
                    avatarData = avatarData,
                    avatarShape = avatarShape,
                    forcedAvatarSize = forcedAvatarSize,
                    contentDescription = contentDescription,
                )
            }
            else -> InitialLetterAvatar(
                avatarData = avatarData,
                avatarShape = avatarShape,
                forcedAvatarSize = forcedAvatarSize,
                contentDescription = contentDescription,
            )
        }
    }
}
