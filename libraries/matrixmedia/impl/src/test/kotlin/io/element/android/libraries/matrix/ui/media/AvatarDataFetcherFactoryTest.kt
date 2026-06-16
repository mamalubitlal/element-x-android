/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media

import android.graphics.Bitmap
import coil.ComponentRegistry
import coil.ImageLoader
import coil.asImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.Options
import coil.request.SuccessResult
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.components.avatar.anAvatarData
import io.element.android.libraries.matrix.test.media.FakeMatrixMediaLoader
import io.mockk.mockk
import org.junit.Test

class AvatarDataFetcherFactoryTest {
    @Test
    fun `create - with mxc returns CoilMediaFetcher`() {
        val factory = AvatarDataFetcherFactory(matrixMediaLoader = FakeMatrixMediaLoader())

        val fetcher = factory.create(anAvatarData(url = "mxc://test"), Options(mockk()), imageLoader = FakeImageLoader())
        assertThat(fetcher).isInstanceOf(CoilMediaFetcher::class.java)
    }

    @Test
    fun `create - with http or https returns null, which means fallback default fetcher will be used`() {
        val factory = AvatarDataFetcherFactory(matrixMediaLoader = FakeMatrixMediaLoader())

        val fetcherHttp = factory.create(anAvatarData(url = "http://test"), Options(mockk()), imageLoader = FakeImageLoader())
        assertThat(fetcherHttp).isNull()

        val fetcherHttps = factory.create(anAvatarData(url = "https://test"), Options(mockk()), imageLoader = FakeImageLoader())
        assertThat(fetcherHttps).isNull()
    }

    @Test
    fun `create - with content scheme returns null, which means fallback default fetcher will be used`() {
        val factory = AvatarDataFetcherFactory(matrixMediaLoader = FakeMatrixMediaLoader())

        val fetcher = factory.create(anAvatarData(url = "content://test"), Options(mockk()), imageLoader = FakeImageLoader())
        assertThat(fetcher).isNull()
    }
}

private class FakeImageLoader : ImageLoader {
    override val defaults: ImageRequest.Defaults = ImageRequest.Defaults.DEFAULT
    override val components: ComponentRegistry = ComponentRegistry.Builder().build()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null

    override fun enqueue(request: ImageRequest): Disposable {
        return mockk()
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        return SuccessResult(
            image = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8).asImage(),
            request = request,
        )
    }

    override fun shutdown() {}

    override fun newBuilder(): ImageLoader.Builder {
        return ImageLoader.Builder(mockk())
    }
}
