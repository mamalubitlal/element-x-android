package com.chtor.app.matrix

/**
 * Pure Matrix client contract. Lives in commonMain, has zero JS / Wasm / platform references.
 * wasmJsMain provides the actual XHR-backed implementation.
 */
interface MatrixApi {
    suspend fun login(homeserver: String, username: String, password: String): Result<LoginResult>
    suspend fun register(homeserver: String, username: String, password: String): Result<RegisterResult>
    suspend fun logout()
    suspend fun joinedRooms(): Result<List<ChatRoom>>
    suspend fun roomMessages(roomId: String, limit: Int = 50): Result<List<Message>>
    suspend fun sendMessage(roomId: String, body: String): Result<Unit>
    suspend fun userDirectory(query: String, limit: Int = 10): Result<List<MatrixUser>>
    suspend fun createDirectChat(userId: String): Result<ChatRoom>
    suspend fun registerPush(): Result<Unit>
    suspend fun searchAllMessages(query: String): Result<List<SearchHit>>
    suspend fun threadMessages(roomId: String, rootEventId: String, limit: Int = 50): Result<List<Message>>
    suspend fun sendThreadMessage(roomId: String, rootEventId: String, body: String): Result<Unit>
    suspend fun roomThreads(roomId: String): Result<List<ThreadSummary>>
    suspend fun rootSpaces(): Result<List<SpaceNode>>
    suspend fun spaceChildren(spaceId: String): Result<List<SpaceNode>>
    suspend fun createSpace(name: String, isPublic: Boolean): Result<SpaceNode>
    fun isLoggedIn(): Boolean
    fun currentUserId(): String?
    fun accessToken(): String?
    fun homeserver(): String?
}

data class MatrixUser(
    val userId: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
)
