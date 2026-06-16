/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaviewer.impl.local.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.mediaviewer.api.local.LocalMedia
import io.element.android.libraries.mediaviewer.impl.local.LocalMediaViewState
import io.element.android.libraries.mediaviewer.impl.local.rememberLocalMediaViewState
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun MediaImageView(
    localMediaViewState: LocalMediaViewState,
    localMedia: LocalMedia?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Image(
            painter = painterResource(id = CommonDrawables.sample_background),
            modifier = modifier,
            contentDescription = null,
        )
    } else {
        AsyncImage(
            model = localMedia?.uri,
            contentDescription = stringResource(id = CommonStrings.common_image),
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun MediaImageViewPreview() = ElementPreview {
    MediaImageView(
        modifier = Modifier.fillMaxSize(),
        localMediaViewState = rememberLocalMediaViewState(),
        localMedia = null,
        onClick = {},
    )
}
