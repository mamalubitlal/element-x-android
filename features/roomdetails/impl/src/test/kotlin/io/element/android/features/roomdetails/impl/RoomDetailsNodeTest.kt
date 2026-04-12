/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bumble.appyx.core.buildcontext.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.google.common.truth.Truth.assertThat
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.call.api.CallType
import io.element.android.features.call.api.ElementCallEntryPoint
import io.element.android.features.leaveroom.api.LeaveRoomRenderer
import io.element.android.libraries.androidutils.system.openUrlInExternalApp
import io.element.android.libraries.architecture.appyx.launchMolecule
import io.element.android.libraries.architecture.callback
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.matrix.api.room.BaseRoom
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.room.powerlevels.canCall
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.item.TimelineEvent
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.FakeUserId
import io.element.android.libraries.matrix.test.aRoomInfo
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.libraries.matrix.test.room.powerlevels.FakeRoomPermissions
import io.element.android.libraries.matrix.test.timeline.FakeTimelineProvider
import io.element.android.libraries.matrix.test.timeline.aTimeline
import io.element.android.services.analytics.api.AnalyticsService
import io.element.android.services.analytics.test.FakeAnalyticsService
import io.element.android.tests.testutils.EnsureCalledOnce
import io.element.android.tests.testutils.EnsureCalledOnceWithParam
import io.element.android.tests.testutils.EnsureNeverCalled
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.clickOn
import io.element.android.tests.testutils.pressBack
import io.element.android.tests.testutils.testWithLifecycleOwner
import io.element.android.tests.testutils.WarmUpRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi
class RoomDetailsNodeTest {

    @get:Rule val warmUpRule = WarmUpRule()
    @get:Rule val mainDispatcherRule = androidx.test.core.app.ApplicationProvider.getApplicationContext<MainDispatcherRule>()

    private lateinit var buildContext: BuildContext
    private lateinit var plugins: List<Plugin>
    private lateinit var presenter: RoomDetailsPresenter
    private lateinit var room: BaseRoom
    private lateinit var joinedRoom: JoinedRoom
    private lateinit var analyticsService: AnalyticsService
    private lateinit var leaveRoomRenderer: LeaveRoomRenderer
    private lateinit var elementCallEntryPoint: ElementCallEntryPoint
    private lateinit var roomDetailsNode: RoomDetailsNode

    @Before
    fun setUp() {
        buildContext = BuildContext.root(savedStateMap = null)
        plugins = emptyList()
        presenter = FakeRoomDetailsPresenter()
        room = FakeBaseRoom(updateMembersResult = {}, initialRoomInfo = aRoomInfo())
        joinedRoom = FakeJoinedRoom(baseRoom = room, timelineProvider = FakeTimelineProvider())
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

    @After
    fun tearDown() {
        Timber.clearListeners()
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
        assertThat(JitsiMeet.joinCalled).isFalse()
    }

    @Test
    fun `given room call state does not allow join when handleRoomCall then uses jitsi fallback`() = runTest {
        // GIVEN: Room call state does NOT allow joining (Element Call unavailable)
        val fakeRoomCallState = FakeRoomCallState(canJoin = false)
        val presenterState = FakeRoomDetailsState(roomCallState = fakeRoomCallState)
        presenter.state = presenterState

        // Make room a JoinedRoom so notification gets sent
        roomDetailsNode.room = joinedRoom

        // WHEN
        roomDetailsNode.handleRoomCall(CallIntent.VIDEO)

        // THEN: Should send notification and use Jitsi
        assertThat(joinedRoom.sentMessageBody).startsWith("Jitsi видеозвонок: https://meet.jit.si/")
        assertThat(joinedRoom.sentMessageHtmlBody).contains("Jitsi видеозвонок: <a href=\"https://meet.jit.si/")
        assertThat(JitsiMeet.joinCalled).isTrue()
        assertThat(JitsiMeet.lastRoomName).isNotEmpty()
        assertThat(JitsiMeet.lastOptions.serverUrl).isEqualTo("https://meet.jit.si")
    }

    @Test
    fun `given jitsi sdk fails when handleRoomCall then falls back to external browser`() = runTest {
        // GIVEN: Room call state does NOT allow joining (Element Call unavailable)
        val fakeRoomCallState = FakeRoomCallState(canJoin = false)
        val presenterState = FakeRoomDetailsState(roomCallState = fakeRoomCallState)
        presenter.state = presenterState

        // Make room a JoinedRoom so notification gets sent
        roomDetailsNode.room = joinedRoom

        // AND: JitsiMeet.join throws an exception
        JitsiMeet.joinThrowsException = true

        // WHEN
        roomDetailsNode.handleRoomCall(CallIntent.AUDIO)

        // THEN: Should send notification and fall back to external browser
        assertThat(joinedRoom.sentMessageBody).startsWith("Jitsi видеозвонок: https://meet.jit.si/")
        assertThat(joinedRoom.sentMessageHtmlBody).contains("Jitsi видеозвонок: <a href=\"https://meet.jit.si/")
        assertThat(ApplicationProvider.getApplicationContext<Activity>().openedUrl).startsWith("https://meet.jit.si/")
    }

    @Test
    fun `given not in joined room when handleRoomCall then does not send notification`() = runTest {
        // GIVEN: Room call state does NOT allow joining (Element Call unavailable)
        val fakeRoomCallState = FakeRoomCallState(canJoin = false)
        val presenterState = FakeRoomDetailsState(roomCallState = fakeRoomCallState)
        presenter.state = presenterState

        // Keep room as BaseRoom (not JoinedRoom) so notification is NOT sent
        roomDetailsNode.room = room // This is a FakeBaseRoom, not FakeJoinedRoom

        // WHEN
        roomDetailsNode.handleRoomCall(CallIntent.VIDEO)

        // THEN: Should NOT send notification but still try Jitsi
        assertThat(joinedRoom.sentMessageBody).isEmpty() // joinedRoom is not used, so no message sent
        assertThat(JitsiMeet.joinCalled).isTrue()
    }

    // Test doubles
    private class FakeRoomDetailsPresenter : RoomDetailsPresenter(
        client = FakeMatrixClient(),
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

    private class FakeRoomCallState(var canJoin: Boolean) {
        fun hasPermissionToJoin(): Boolean = canJoin
    }

    private class FakeLeaveRoomState : LeaveRoomState(
        roomId = "!test:localhost",
        needsConfirmation = false
    ) {
        override fun eventSink(event: LeaveRoomEvent) = Unit
    }

    private class FakeElementCallEntryPoint : ElementCallEntryPoint(
        assistedInjectConstructor = AssistedInject::class.java
    ) {
        var navigateToRoomCallCalled = false
        var lastCallIntent: CallIntent? = null
        override fun createNode(
            parentNode: Node,
            buildContext: BuildContext,
            params: ElementCallEntryPoint.Params,
            callback: ElementCallEntryPoint.Callback
        ): Node {
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

    // Extensions for test verification
    private val Activity.openUrlInExternalApp: (url: String) -> Unit get() = {
        ApplicationProvider.getApplicationContext<Activity>().openedUrl = url
    }

    private var Activity.openedUrl: String by androidx.delegate.notnull<String>()

    private val Timeline.sendMessage: (body: String, htmlBody: String, intentionalMentions: List<UserId>) -> Unit get() = {
        (this as FakeJoinedRoom).liveTimeline as FakeTimelineProvider
    }.sendMessage

    private val FakeJoinedRoom.liveTimeline get() = timelineProvider as FakeTimelineProvider
    private val FakeTimelineProvider.sentMessageBody: String by mutableStateOf("")
    private val FakeTimelineProvider.sentMessageHtmlBody: String by mutableStateOf("")
    private fun FakeTimelineProvider.sendMessage(
        body: String,
        htmlBody: String,
        intentionalMentions: List<UserId>
    ) {
        this.sentMessageBody = body
        this.sentMessageHtmlBody = htmlBody
    }

    // JitsiMeet test double
    object JitsiMeet {
        var joinCalled = false
        var lastRoomName: String? = null
        var lastOptions: JitsiMeetConferenceOptions? = null
        var joinThrowsException = false

        fun join(activity: Activity, options: JitsiMeetConferenceOptions) {
            joinCalled = true
            // Extract room name from options (this is simplified)
            lastOptions = options
            if (joinThrowsException) {
                throw Exception("Jitsi SDK failed")
            }
        }
    }
}