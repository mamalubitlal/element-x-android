package com.chtor.app.matrix

import com.chtor.app.currentTimeMillis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * XHR-backed Matrix client. All JS interop lives behind [ChatorJs] (declared in
 * `JsBridge.kt`); no `js()` calls in this file (Kotlin/Wasm restricts those to
 * constant string literals).
 *
 * Uses synchronous XHR — allowed in the wasmJs main thread — wrapped in a
 * trivial coroutine so it composes with the rest of the app's suspend code.
 */
class MatrixClient(
    private val storage: TokenStorage = JsTokenStorage
) : MatrixApi {

    private var homeserverUrl: String? = null
    private var accessToken: String? = null
    private var userId: String? = null
    private var deviceId: String? = null

    init {
        accessToken    = storage.get(KEY_TOKEN)
        userId         = storage.get(KEY_USER)
        homeserverUrl  = storage.get(KEY_HOMESERVER)
        deviceId       = storage.get(KEY_DEVICE)
    }

    override fun isLoggedIn(): Boolean    = !accessToken.isNullOrEmpty()
    override fun currentUserId(): String? = userId
    override fun accessToken(): String?   = accessToken
    override fun homeserver(): String?    = homeserverUrl

    override suspend fun login(homeserver: String, username: String, password: String): Result<LoginResult> = runCatching {
        val url = homeserver.trimEnd('/')
        val req = LoginRequest(
            identifier = LoginRequest.Identifier(user = username),
            password = password
        )
        val raw = ChatorJs.xhr("POST", "$url/_matrix/client/v3/login", json.encodeToString(req), auth = null)
        val res = json.decodeFromString<LoginResponse>(raw)

        accessToken    = res.access_token
        userId         = res.user_id
        deviceId       = res.device_id
        homeserverUrl  = url

        storage.set(KEY_TOKEN, res.access_token)
        storage.set(KEY_USER, res.user_id)
        storage.set(KEY_DEVICE, res.device_id)
        storage.set(KEY_HOMESERVER, url)

        LoginResult(
            userId = res.user_id,
            accessToken = res.access_token,
            deviceId = res.device_id,
            homeServer = res.home_server,
            homeserverUrl = url
        )
    }

    override suspend fun register(homeserver: String, username: String, password: String): Result<RegisterResult> = runCatching {
        val hs = homeserver.trimEnd('/')
        val url = "$hs/_matrix/client/v3/register?kind=user"

        fun tryRegister(auth: AuthRequest?): String {
            val req = RegisterRequest(username = username, password = password, auth = auth)
            val raw = ChatorJs.xhr("POST", url, json.encodeToString(req), auth = null)
            if (raw.startsWith("__ERR__:")) throw Exception(raw.removePrefix("__ERR__:"))
            return raw
        }

        val raw = runCatching { tryRegister(null) }.recoverCatching { e ->
            val msg = e.message ?: throw e
            if (!msg.startsWith("http 401:")) throw e
            val uiaBody = msg.removePrefix("http 401:").trimStart(' ')
            val uia = json.decodeFromString<UiaResponse>(uiaBody)
            val session = uia.session ?: throw Exception("Ошибка регистрации")
            val hasDummy = uia.flows.any { f -> f.stages.any { s -> s == "m.login.dummy" } }
            if (!hasDummy) throw Exception("Регистрация не поддерживается на этом сервере")
            tryRegister(AuthRequest(type = "m.login.dummy", session = session))
        }.getOrThrow()

        val res = json.decodeFromString<RegisterResponse>(raw)
        accessToken = res.access_token
        userId = res.user_id
        deviceId = res.device_id
        homeserverUrl = hs
        storage.set(KEY_TOKEN, res.access_token)
        storage.set(KEY_USER, res.user_id)
        storage.set(KEY_DEVICE, res.device_id)
        storage.set(KEY_HOMESERVER, hs)
        RegisterResult(
            userId = res.user_id,
            accessToken = res.access_token,
            deviceId = res.device_id,
            homeServer = res.home_server,
            homeserverUrl = hs
        )
    }

    override suspend fun logout() {
        runCatching { ChatorJs.xhr("POST", "${homeserverUrl}/_matrix/client/v3/logout", "", auth = accessToken) }
        accessToken = null; userId = null; deviceId = null; homeserverUrl = null
        storage.clear()
    }

    override suspend fun joinedRooms(): Result<List<ChatRoom>> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val raw = ChatorJs.xhr("GET", "$hs/_matrix/client/v3/joined_rooms", "", auth = accessToken)
        val res = json.decodeFromString<JoinedRoomsResponse>(raw)
        res.joined_rooms.map { id ->
            val name = runCatching { roomName(hs, id) }.getOrDefault(id)
            ChatRoom(id = id, name = name)
        }
    }

    override suspend fun roomMessages(roomId: String, limit: Int): Result<List<Message>> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val encId = ChatorJs.encodeURI(roomId)
        val raw = ChatorJs.xhr(
            "GET",
            "$hs/_matrix/client/v3/rooms/$encId/messages?dir=b&limit=$limit",
            "",
            auth = accessToken
        )
        val res = json.decodeFromString<MessagesResponse>(raw)
        val mine = userId
        res.chunk.mapNotNull { ev ->
            val type = ev.type ?: return@mapNotNull null
            if (type != "m.room.message") return@mapNotNull null
            val body = ev.content?.body ?: return@mapNotNull null
            val sender = ev.sender ?: return@mapNotNull null
            val id = ev.event_id ?: return@mapNotNull null
            val ts = ev.origin_server_ts ?: 0L
            Message(id = id, sender = sender, body = body, timestamp = ts, isMine = sender == mine)
        }.sortedBy { it.timestamp }
    }

    override suspend fun sendMessage(roomId: String, body: String): Result<Unit> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val encId = ChatorJs.encodeURI(roomId)
        val txn = "tx-${currentTimeMillis()}-${ChatorJs.now().toRawBits().toString(16).take(8)}"
        val url = "$hs/_matrix/client/v3/rooms/$encId/send/m.room.message/$txn"
        ChatorJs.xhr("PUT", url, json.encodeToString(SendMessageRequest(body = body)), auth = accessToken)
    }

    override suspend fun threadMessages(roomId: String, rootEventId: String, limit: Int): Result<List<Message>> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val encRoom  = ChatorJs.encodeURI(roomId)
        val encEvent = ChatorJs.encodeURI(rootEventId)
        val raw = ChatorJs.xhr(
            "GET",
            "$hs/_matrix/client/v1/rooms/$encRoom/relations/$encEvent/m.thread?dir=b&limit=$limit",
            "",
            auth = accessToken
        )
        val res = json.decodeFromString<RelationsResponse>(raw)
        val mine = userId
        res.chunk.mapNotNull { ev ->
            if (ev.type != "m.room.message") return@mapNotNull null
            val body = ev.content?.body ?: return@mapNotNull null
            val sender = ev.sender ?: return@mapNotNull null
            val id = ev.event_id ?: return@mapNotNull null
            val ts = ev.origin_server_ts ?: 0L
            Message(id = id, sender = sender, body = body, timestamp = ts, isMine = sender == mine)
        }.sortedBy { it.timestamp }
    }

    override suspend fun sendThreadMessage(roomId: String, rootEventId: String, body: String): Result<Unit> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val encId = ChatorJs.encodeURI(roomId)
        val txn = "tx-${currentTimeMillis()}-${ChatorJs.now().toRawBits().toString(16).take(8)}"
        val url = "$hs/_matrix/client/v3/rooms/$encId/send/m.room.message/$txn"
        val payload = SendMessageRequest(
            body = body,
            relatesTo = RelationRef(relType = "m.thread", eventId = rootEventId)
        )
        ChatorJs.xhr("PUT", url, json.encodeToString(payload), auth = accessToken)
    }

    override suspend fun roomThreads(roomId: String): Result<List<ThreadSummary>> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val encId = ChatorJs.encodeURI(roomId)
        val raw = ChatorJs.xhr("GET", "$hs/_matrix/client/v1/rooms/$encId/threads", "", auth = accessToken)
        val obj: kotlinx.serialization.json.JsonObject = json.parseToJsonElement(raw).jsonObject
        val chunkArr: kotlinx.serialization.json.JsonArray? = obj["chunk"]?.jsonArray
        if (chunkArr == null) {
            emptyList<ThreadSummary>()
        } else {
            val out = ArrayList<ThreadSummary>(chunkArr.size)
            for (entry in chunkArr) {
                val o = entry.jsonObject
                val event = o["event"]?.jsonObject ?: continue
                val eventId = event["event_id"]?.jsonPrimitive?.contentOrNull ?: continue
                val sender = event["sender"]?.jsonPrimitive?.contentOrNull ?: ""
                val body = event["content"]?.jsonObject?.get("body")?.jsonPrimitive?.contentOrNull ?: ""
                val ts = event["origin_server_ts"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
                val count = o["count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                out.add(ThreadSummary(eventId, sender, body, ts, count))
            }
            out
        }
    }

    override suspend fun userDirectory(query: String, limit: Int): Result<List<MatrixUser>> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val q = query.trim()
        if (q.isEmpty()) return@runCatching emptyList()
        val raw = ChatorJs.xhr(
            "POST",
            "$hs/_matrix/client/v3/user_directory/search",
            json.encodeToString(UserDirectoryRequest(search_term = q, limit = limit)),
            auth = accessToken
        )
        val res = json.decodeFromString<UserDirectoryResponse>(raw)
        res.results.map { MatrixUser(userId = it.user_id, displayName = it.display_name, avatarUrl = it.avatar_url) }
    }

    override suspend fun createDirectChat(userId: String): Result<ChatRoom> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val raw = ChatorJs.xhr(
            "POST",
            "$hs/_matrix/client/v3/createRoom",
            json.encodeToString(CreateRoomRequest(invite = listOf(userId))),
            auth = accessToken
        )
        val res = json.decodeFromString<CreateRoomResponse>(raw)
        val name = runCatching { roomName(hs, res.room_id) }.getOrDefault(userId)
        ChatRoom(id = res.room_id, name = name, isDirect = true)
    }

    override suspend fun registerPush(): Result<Unit> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        // Kick off async registration; poll the global result.
        ChatorJs.registerPush()
        val deadline = currentTimeMillis() + 8_000
        var raw: String? = null
        while (currentTimeMillis() < deadline) {
            raw = ChatorJs.pollPush()
            if (raw != null) break
            kotlinx.coroutines.delay(150)
        }
        val payload = raw ?: error("push registration timeout")

        // Parse {ok: true, endpoint, keys} | {ok: false, err|reason}
        val jsonEl = json.parseToJsonElement(payload).jsonObject
        if (jsonEl["ok"]?.jsonPrimitive?.booleanOrNull != true) {
            error(jsonEl["err"]?.jsonPrimitive?.contentOrNull
                  ?: jsonEl["reason"]?.jsonPrimitive?.contentOrNull
                  ?: "push subscription failed")
        }
        val endpoint = jsonEl["endpoint"]?.jsonPrimitive?.content ?: error("no endpoint")
        val keys = jsonEl["keys"]?.jsonObject
        val p256dh = keys?.get("p256dh")?.jsonPrimitive?.content
        val auth   = keys?.get("auth")?.jsonPrimitive?.content

        // Register on the homeserver.
        val pushBody = PusherSetRequest(
            pushkey = endpoint,
            url = "https://matrix.org/_matrix/push/v1/notify",
            deviceDisplayName = p256dh?.take(12) ?: "Browser",
            lang = "ru"
        )
        ChatorJs.xhr(
            "POST",
            "$hs/_matrix/client/v3/pushers/set",
            json.encodeToString(pushBody),
            auth = accessToken
        )
    }

    override suspend fun searchAllMessages(query: String): Result<List<SearchHit>> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val q = query.trim()
        if (q.length < 2) return@runCatching emptyList()

        val raw = ChatorJs.xhr("GET", "$hs/_matrix/client/v3/joined_rooms", "", auth = accessToken)
        val rooms = json.decodeFromString<JoinedRoomsResponse>(raw).joined_rooms

        val hits = mutableListOf<SearchHit>()
        for (id in rooms) {
            val encId = ChatorJs.encodeURI(id)
            val mraw = ChatorJs.xhr(
                "GET",
                "$hs/_matrix/client/v3/rooms/$encId/messages?dir=b&limit=100",
                "",
                auth = accessToken
            )
            val mres = json.decodeFromString<MessagesResponse>(mraw)
            for (ev in mres.chunk) {
                val body = ev.content?.body ?: continue
                if (!body.contains(q, ignoreCase = true)) continue
                if (ev.type != "m.room.message") continue
                val name = runCatching { roomName(hs, id) }.getOrDefault(id)
                hits.add(
                    SearchHit(
                        roomId = id,
                        roomName = name,
                        eventId = ev.event_id ?: continue,
                        sender = ev.sender ?: "",
                        body = body,
                        timestamp = ev.origin_server_ts ?: 0L
                    )
                )
            }
        }
        hits.sortedByDescending { it.timestamp }
    }

    private fun roomName(hs: String, roomId: String): String {
        val encId = ChatorJs.encodeURI(roomId)
        val raw = ChatorJs.xhr(
            "GET",
            "$hs/_matrix/client/v3/rooms/$encId/state/m.room.name",
            "",
            auth = accessToken
        )
        val res = json.decodeFromString<RoomNameResponse>(raw)
        return res.name ?: roomId
    }

    private fun roomKind(hs: String, roomId: String): SpaceNode.Kind? {
        val enc = ChatorJs.encodeURI(roomId)
        val raw = ChatorJs.xhr(
            "GET",
            "$hs/_matrix/client/v3/rooms/$enc/state/m.room.type",
            "",
            auth = accessToken
        )
        val type = json.parseToJsonElement(raw).jsonObject["type"]?.jsonPrimitive?.contentOrNull
        return if (type == "m.space") SpaceNode.Kind.SPACE else SpaceNode.Kind.ROOM
    }

    override suspend fun rootSpaces(): Result<List<SpaceNode>> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val raw = ChatorJs.xhr("GET", "$hs/_matrix/client/v3/joined_rooms", "", auth = accessToken)
        val ids = json.decodeFromString<JoinedRoomsResponse>(raw).joined_rooms
        ids.mapNotNull { id ->
            val k = runCatching { roomKind(hs, id) }.getOrNull() ?: return@mapNotNull null
            val name = runCatching { roomName(hs, id) }.getOrDefault(id)
            SpaceNode(id = id, name = name, kind = k)
        }.filter { it.kind == SpaceNode.Kind.SPACE }
    }

    override suspend fun spaceChildren(spaceId: String): Result<List<SpaceNode>> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val enc = ChatorJs.encodeURI(spaceId)
        val raw = ChatorJs.xhr(
            "GET",
            "$hs/_matrix/client/v1/rooms/$enc/hierarchy?max_depth=1&limit=100",
            "",
            auth = accessToken
        )
        val res = json.decodeFromString<HierarchyResponse>(raw)
        res.rooms.mapNotNull { hr ->
            val isSpace = hr.room_type == "m.space"
            SpaceNode(
                id = hr.room_id,
                name = hr.name ?: hr.room_id,
                kind = if (isSpace) SpaceNode.Kind.SPACE else SpaceNode.Kind.ROOM,
                memberCount = hr.num_joined_members
            )
        }
    }

    override suspend fun createSpace(name: String, isPublic: Boolean): Result<SpaceNode> = runCatching {
        val hs = homeserverUrl ?: error("Not logged in")
        val req = CreateSpaceRequest(
            name = name,
            preset = if (isPublic) "public_chat" else "private_chat",
            visibility = if (isPublic) "public" else "private"
        )
        val raw = ChatorJs.xhr(
            "POST",
            "$hs/_matrix/client/v3/createRoom",
            json.encodeToString(req),
            auth = accessToken
        )
        val id = json.parseToJsonElement(raw).jsonObject["room_id"]?.jsonPrimitive?.content
            ?: error("no room_id in response")
        SpaceNode(id = id, name = name, kind = SpaceNode.Kind.SPACE)
    }
}

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

// ----------------- Storage abstraction -----------------

interface TokenStorage {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun clear()
}

private object JsTokenStorage : TokenStorage {
    override fun get(key: String): String? = ChatorJs.storage.getString(key)
    override fun set(key: String, value: String) { ChatorJs.storage.setString(key, value) }
    override fun clear() {
        listOf(KEY_TOKEN, KEY_USER, KEY_DEVICE, KEY_HOMESERVER).forEach { ChatorJs.storage.removeString(it) }
    }
}

private const val KEY_TOKEN      = "chator.access_token"
private const val KEY_USER       = "chator.user_id"
private const val KEY_DEVICE     = "chator.device_id"
private const val KEY_HOMESERVER = "chator.homeserver"
