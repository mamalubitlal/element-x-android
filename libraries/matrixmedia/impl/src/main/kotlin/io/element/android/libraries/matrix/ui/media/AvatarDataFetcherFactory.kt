/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media

import android.net.Uri
import coil.ImageLoader
import coil.fetch.Fetcher
import coil.request.Options
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.matrix.api.media.MatrixMediaLoader

internal class AvatarDataFetcherFactory(
    private val matrixMediaLoader: MatrixMediaLoader
) : Fetcher.Factory<AvatarData> {
    override fun create(
        data: AvatarData,
        options: Options,
        imageLoader: ImageLoader
    ): Fetcher? {
        return when {
            data.url == null -> null
            data.url?.startsWith("mxc") == true -> CoilMediaFetcher(
                context = options.context,
                mediaLoader = matrixMediaLoader,
                mediaData = data.toMediaRequestData(),
            )
            else -> {
                // If the URL does not use the mxc scheme, it might be a local one using `content://`, try using a fallback fetcher
                val uri = Uri.parse(data.url!!)
                imageLoader.components.newFetcher(uri, options, imageLoader) as? Fetcher
            }
        }
    }
}
