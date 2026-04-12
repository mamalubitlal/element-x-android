/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl

import com.bumble.appyx.core.buildcontext.BuildContext
import com.bumble.appyx.core.plugin.Plugin
import com.google.common.truth.Truth.assertThat
import io.element.android.features.call.api.ElementCallEntryPoint
import io.element.android.features.leaveroom.api.LeaveRoomRenderer
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.matrix.api.room.BaseRoom
import io.element.android.libraries.matrix.test.FakeUserId
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.libraries.matrix.test.timeline.FakeTimelineProvider
import io.element.android.services.analytics.api.AnalyticsService
import io.element.android.services.analytics.test.FakeAnalyticsService
import io.element.android.tests.testutils.EventsRecorder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi
class RoomDetailsNodeTest {

    @get:Rule val warmUpRule = io.element.android.tests.testutils.WarmUpRule()
    @get:Rule val mainDispatcherRule = androidx.test.core.app.ApplicationProvider.getApplicationContext<MainDispatcherRule>()

    private lateinit var buildContext: BuildContext
    private lateinit var plugins: List<Plugin>
    private lateinit var presenter: RoomDetailsPresenter
    private lateinit var room: BaseRoom
    private lateinit var analyticsService: AnalyticsService
    private lateinit var leaveRoomRenderer: LeaveRoomRenderer
    private lateinit var elementCallEntryPoint: ElementCallEntryPoint
    private lateinit var roomDetailsNode: RoomDetailsNode

    @Before
    fun setUp() {
        buildContext = BuildContext.root(savedStateMap = null)
        plugins = emptyList()
        presenter = FakeRoomDetailsPresenter()
        room = FakeBaseRoom(updateMembersResult = {}, initialRoomInfo = io.element.android.libraries.matrix.test.aRoomInfo())
        analyticsService = FakeAnalyticsService()
        leaveRoomRenderer = FakeLeaveRoomRenderer()
        elementCallEntryPoint = FakeElementCallEntryPoint()

        roomDetailsNode = RoomDetailsNode(
            buildContext = buildContext,
            plugins = plugins,
            presenter = presenter,
            room = room,
            analyticsService = analyticsService,
            leaveRoomRenderer = leaveRoomRenderer,
            elementCallEntryPoint = elementCallEntryPoint
        )
    }

    @Test
    fun `given room call state allows join when handleRoomCall then navigates to element call`() = runTest {
        // GIVEN: Room call state allows joining (Element Call available)
        val fakeRoomCallState = FakeRoomCallState(canJoin = true)
        val presenterState = FakeRoomDetailsState(roomCallState = fakeRoomCallState)
        presenter.state = presenterState

        // WHEN
        roomDetailsNode.handleRoomCall(CallIntent.AUDIO)

        // THEN: Should navigate to Element Call, not use Jitsi
        assertThat(elementCallEntryPoint.navigateToRoomCallCalled).isTrue()
        assertThat(elementCallEntryPoint.lastCallIntent).isEqualTo(CallIntent.AUDIO)
    }

    @Test
    fun `given room call state does not allow join when handleRoomCall then uses jitsi via browser`() = runTest {
        // GIVEN: Room call state does NOT allow joining (Element Call unavailable)
        val fakeRoomCallState = FakeRoomCallState(canJoin = false)
        val presenterState = FakeRoomDetailsState(roomCallState = fakeRoomCallState)
        presenter.state = presenterState

        // WHEN
        roomDetailsNode.handleRoomCall(CallIntent.VIDEO)

        // THEN: Should NOT navigate to Element Call (Jitsi fallback will be used)
        assertThat(elementCallEntryPoint.navigateToRoomCallCalled).isFalse()
    }

    // Test doubles
    private class FakeRoomDetailsPresenter : RoomDetailsPresenter(
        client = io.element.android.libraries.matrix.test.FakeMatrixClient(),
        room = FakeJoinedRoom(FakeBaseRoom()),
        featureFlagService = FakeFeatureFlagService(emptyMap()),
        notificationSettingsService = FakeNotificationSettingsService(),
        roomMembersDetailsPresenterFactory = { _ -> FakeRoomMemberDetailsPresenter() },
        leaveRoomPresenter = { FakeLeaveRoomState() },
        roomCallStatePresenter = { FakeRoomCallState() },
        dispatchers = FakeCoroutineDispatchers(),
        analyticsService = FakeAnalyticsService(),
        clipboardHelper = FakeClipboardHelper(),
        appPreferencesStore = InMemoryAppPreferencesStore()
    ) {
        var state: FakeRoomDetailsState = FakeRoomDetailsState()
        override fun present() = state
    }

    private class FakeRoomDetailsState(
        var roomCallState: FakeRoomCallState = FakeRoomCallState()
    ) : RoomDetailsState(
        roomId = "!test:localhost",
        roomName = "Test Room",
        roomAvatarUrl = null,
        roomTopic = RoomTopicState.NoTopic,
        memberCount = 0,
        pinnedMessagesCount = 0,
        canShowSecurityAndPrivacy = false,
        showDebugInfo = false,
        canInvite = false,
        canEdit = false,
        roomType = RoomDetailsType.NoRoomType,
        roomNotificationSettings = null,
        leaveRoomState = FakeLeaveRoomState(),
        eventSink = EventsRecorder(expectEvents = false)
    ) {
        override val roomCallState: FakeRoomCallState get() = roomCallState
    }

    private class FakeRoomCallState(var canJoin: Boolean = false) {
        fun hasPermissionToJoin(): Boolean = canJoin
    }

    private class FakeLeaveRoomState : LeaveRoomState(
        roomId = "!test:localhost",
        needsConfirmation = false
    ) {
        override fun eventSink(event: LeaveRoomEvent) = Unit
    }

    private class FakeElementCallEntryPoint : ElementCallEntryPoint(
        assistedInjectConstructor = dev.zacsweers.metro.AssistedInject::class.java
    ) {
        var navigateToRoomCallCalled = false
        var lastCallIntent: CallIntent? = null
        override fun createNode(
            parentNode: com.bumble.appyx.core.node.Node,
            buildContext: BuildContext,
            params: ElementCallEntryPoint.Params,
            callback: ElementCallEntryPoint.Callback
        ): com.bumble.appyx.core.node.Node {
            navigateToRoomCallCalled = true
            lastCallIntent = params.callIntent
            return super.createNode(parentNode, buildContext, params, callback)
        }
    }

    private class FakeLeaveRoomRenderer : LeaveRoomRenderer() {
        override fun Render(
            state: LeaveRoomState,
            onSelectNewOwners: (() -> Unit)?,
            modifier: androidx.compose.ui.Modifier
        ) = androidx.compose.runtime.Composable {}
    }

    private class FakeRoomMemberDetailsPresenter : RoomMemberDetailsPresenter(
        roomMemberId = FakeUserId(),
        room = FakeJoinedRoom(FakeBaseRoom()),
        userProfilePresenterFactory = { FakeRoomMemberDetailsPresenter() },
        encryptionService = FakeEncryptionService(),
        clipboardHelper = FakeClipboardHelper()
    ) {
        override fun present() = FakeRoomMemberDetailsState()
        private class FakeRoomMemberDetailsState : RoomMemberDetailsState(
            avatarUrl = null,
            displayName = null,
            membership = null,
            powerLevel = 0,
            typingsNotice = null,
            roomMemberId = FakeUserId()
        )
    }

    private class FakeCoroutineDispatchers : CoroutineDispatchers() {
        override val main: kotlinx.coroutines.CoroutineDispatcher get() = kotlinx.coroutines.Dispatchers.Unconfined
        override val io: kotlinx.coroutines.CoroutineDispatcher get() = kotlinx.coroutines.Dispatchers.Unconfined
    }
}
