package com.chtor.app.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- DTOs for Matrix v3 Client-Server API ----

@Serializable
data class LoginRequest(
    val type: String = "m.login.password",
    val identifier: Identifier,
    val password: String,
    val initial_device_display_name: String = "Chator Web"
) {
    @Serializable
    data class Identifier(val type: String = "m.id.user", val user: String)
}

@Serializable
data class LoginResponse(
    val user_id: String,
    val access_token: String,
    val device_id: String,
    val home_server: String? = null
)

@Serializable
data class JoinedRoomsResponse(val joined_rooms: List<String> = emptyList())

@Serializable
data class MessagesResponse(
    val chunk: List<RoomEvent> = emptyList(),
    val start: String? = null,
    val end: String? = null
)

@Serializable
data class RoomEvent(
    val type: String? = null,
    val event_id: String? = null,
    val sender: String? = null,
    val origin_server_ts: Long? = null,
    val content: MessageContent? = null
)

@Serializable
data class MessageContent(
    val msgtype: String? = null,
    val body: String? = null
)

@Serializable
data class SendMessageRequest(
    @SerialName("msgtype") val msgtype: String = "m.text",
    @SerialName("body")    val body: String,
    @SerialName("m.relates_to") val relatesTo: RelationRef? = null
)

@Serializable
data class RelationRef(
    @SerialName("rel_type") val relType: String,
    @SerialName("event_id") val eventId: String
)

@Serializable
data class RelationsResponse(
    @SerialName("chunk")      val chunk: List<RoomEvent> = emptyList(),
    @SerialName("next_batch") val nextBatch: String? = null
)

@Serializable
data class RoomNameResponse(val name: String? = null)

@Serializable
data class UserDirectoryRequest(
    val search_term: String,
    val limit: Int = 10
)

@Serializable
data class UserDirectoryResponse(
    val results: List<UserDirectoryEntry> = emptyList(),
    val limited: Boolean = false
)

@Serializable
data class UserDirectoryEntry(
    val user_id: String,
    val display_name: String? = null,
    val avatar_url: String? = null
)

@Serializable
data class CreateRoomRequest(
    val preset: String = "trusted_private_chat",
    val invite: List<String> = emptyList(),
    val is_direct: Boolean = true,
    val name: String? = null
)

@Serializable
data class CreateRoomResponse(val room_id: String)

// ---- App-facing models (returned from API layer) ----

data class LoginResult(
    val userId: String,
    val accessToken: String,
    val deviceId: String,
    val homeServer: String?,
    val homeserverUrl: String
)

data class ChatRoom(
    val id: String,
    val name: String,
    val topic: String? = null,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastTimestamp: Long = 0L,
    val unread: Int = 0,
    val isDirect: Boolean = false
)

data class Message(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val isMine: Boolean,
    val status: MessageStatus = MessageStatus.Sent
)

enum class MessageStatus { Sending, Sent, Failed }

// ----- Search -----

data class SearchHit(
    val roomId: String,
    val roomName: String,
    val eventId: String,
    val sender: String,
    val body: String,
    val timestamp: Long
)

// ----- Spaces -----

data class SpaceNode(
    val id: String,
    val name: String,
    val kind: Kind,
    val memberCount: Int = 0,
    val isDirect: Boolean = false
) {
    enum class Kind { SPACE, ROOM }
}

// ----- Threads summary -----

@Serializable
data class ThreadSummary(
    val rootEventId: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val count: Int
)

@Serializable
data class CreateSpaceRequest(
    @SerialName("creation_content") val creationContent: Map<String, String> = mapOf("type" to "m.space"),
    @SerialName("name")              val name: String,
    @SerialName("topic")             val topic: String? = null,
    @SerialName("preset")            val preset: String = "private_chat",
    @SerialName("visibility")        val visibility: String = "private"
)

// ----- Spaces / hierarchy -----

@Serializable
data class HierarchyResponse(
    @SerialName("rooms")       val rooms: List<HierarchyRoom> = emptyList(),
    @SerialName("next_batch")  val next_batch: String? = null
)

@Serializable
data class HierarchyRoom(
    @SerialName("room_id")              val room_id: String,
    @SerialName("name")                 val name: String? = null,
    @SerialName("topic")                val topic: String? = null,
    @SerialName("room_type")            val room_type: String? = null,
    @SerialName("num_joined_members")   val num_joined_members: Int = 0
)

// ----- Spaces / hierarchy -----

// ----- Registration / UIA -----

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val auth: AuthRequest? = null,
    @SerialName("initial_device_display_name") val initialDeviceDisplayName: String = "Chator Web"
)

@Serializable
data class AuthRequest(
    val type: String,
    val session: String? = null
)

@Serializable
data class RegisterResponse(
    val user_id: String,
    val access_token: String,
    val device_id: String,
    val home_server: String? = null
)

@Serializable
data class UiaResponse(
    val session: String? = null,
    val flows: List<UiaFlow> = emptyList()
)

@Serializable
data class UiaFlow(
    val stages: List<String> = emptyList()
)

data class RegisterResult(
    val userId: String,
    val accessToken: String,
    val deviceId: String,
    val homeServer: String?,
    val homeserverUrl: String
)

@Serializable
data class PusherSetRequest(
    @SerialName("pushkey")              val pushkey: String,
    @SerialName("kind")                 val kind: String = "http",
    @SerialName("app_id")               val appId: String = "chator.web",
    @SerialName("app_display_name")     val appDisplayName: String = "Chator Web",
    @SerialName("device_display_name")  val deviceDisplayName: String = "Browser",
    @SerialName("lang")                 val lang: String = "ru",
    @SerialName("url")                  val url: String,
    @SerialName("format")               val format: String = "event_id_only"
)
