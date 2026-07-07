/* ============================================
   Chator — Matrix API Client (vanilla JS)
   ============================================ */
const MatrixAPI = (() => {
  const HS = 'https://chator.space';
  let _hs = localStorage.getItem('chator_hs') || HS;
  let _userId = localStorage.getItem('chator_uid') || null;
  let _accessToken = localStorage.getItem('chator_token') || null;
  let _deviceId = localStorage.getItem('chator_device') || null;

  function _url(path) { return _hs.replace(/\/+$/, '') + path; }

  function _req(method, path, body, auth) {
    const xhr = new XMLHttpRequest();
    xhr.open(method, _url(path), false);
    xhr.setRequestHeader('Content-Type', 'application/json');
    if (auth && _accessToken) xhr.setRequestHeader('Authorization', 'Bearer ' + _accessToken);
    try { xhr.send(body ? JSON.stringify(body) : undefined); }
    catch (e) { throw new Error('network: ' + e); }
    if (xhr.status >= 200 && xhr.status < 300) {
      return xhr.responseText ? JSON.parse(xhr.responseText) : {};
    }
    let msg = 'HTTP ' + xhr.status;
    try { const r = JSON.parse(xhr.responseText); msg += ': ' + (r.errcode || r.error || xhr.responseText); } catch(e) {}
    throw new Error(msg);
  }

  // Simple async wrapper using fetch
  async function _fetch(method, path, body, auth) {
    const headers = { 'Content-Type': 'application/json' };
    if (auth && _accessToken) headers['Authorization'] = 'Bearer ' + _accessToken;
    const resp = await fetch(_url(path), {
      method, headers,
      body: body ? JSON.stringify(body) : undefined
    });
    const text = await resp.text();
    if (!resp.ok) {
      let msg = 'HTTP ' + resp.status;
      try { const r = JSON.parse(text); msg += ': ' + (r.errcode || r.error || text); } catch(e) {}
      throw new Error(msg);
    }
    return text ? JSON.parse(text) : {};
  }

  return {
    isLoggedIn() { return !!_accessToken && !!_userId; },
    currentUserId() { return _userId; },
    homeserver() { return _hs; },
    accessToken() { return _accessToken; },

    async login(homeserver, username, password) {
      _hs = homeserver;
      const data = await _fetch('POST', '/_matrix/client/v3/login', {
        type: 'm.login.password',
        identifier: { type: 'm.id.user', user: username },
        password,
        initial_device_display_name: 'Chator Web'
      });
      _userId = data.user_id;
      _accessToken = data.access_token;
      _deviceId = data.device_id;
      localStorage.setItem('chator_hs', _hs);
      localStorage.setItem('chator_uid', _userId);
      localStorage.setItem('chator_token', _accessToken);
      localStorage.setItem('chator_device', _deviceId);
      return data;
    },

    logout() {
      if (_accessToken) {
        try { _req('POST', '/_matrix/client/v3/logout', {}, true); } catch(e) {}
      }
      _accessToken = null; _userId = null; _deviceId = null;
      localStorage.removeItem('chator_uid');
      localStorage.removeItem('chator_token');
      localStorage.removeItem('chator_device');
    },

    async joinedRooms() {
      const data = await _fetch('GET', '/_matrix/client/v3/joined_rooms', null, true);
      const rooms = [];
      for (const roomId of data.joined_rooms) {
        try {
          const [nameData, stateData] = await Promise.all([
            _fetch('GET', `/_matrix/client/v3/rooms/${encodeURIComponent(roomId)}/name`, null, true).catch(() => ({})),
            _fetch('GET', `/_matrix/client/v3/rooms/${encodeURIComponent(roomId)}/state`, null, true).catch(() => [])
          ]);
          let name = nameData.name || '';
          let lastMessage = '', lastTs = 0;
          // Try to get last message from room state
          const createEvent = stateData.find(e => e.type === 'm.room.create');
          if (!name && createEvent?.content?.creator) {
            name = createEvent.content.creator;
          }
          if (!name) name = roomId;
          rooms.push({
            id: roomId, name, topic: '',
            lastMessage, lastTimestamp: lastTs,
            unread: 0, isDirect: false
          });
        } catch(e) { /* skip broken rooms */ }
      }
      // Sort by name
      rooms.sort((a, b) => a.name.localeCompare(b.name));
      return rooms;
    },

    async roomMessages(roomId, limit = 50) {
      const data = await _fetch('GET',
        `/_matrix/client/v3/rooms/${encodeURIComponent(roomId)}/messages?limit=${limit}&dir=b`, null, true);
      const msgs = [];
      const chunk = data.chunk || [];
      const myId = _userId || '';
      for (const ev of chunk.reverse()) {
        if (ev.type !== 'm.room.message') continue;
        if (!ev.content?.body) continue;
        msgs.push({
          id: ev.event_id || '',
          sender: ev.sender || '',
          body: ev.content.body,
          timestamp: ev.origin_server_ts || 0,
          isMine: ev.sender === myId,
          status: 'sent'
        });
      }
      return msgs;
    },

    async sendMessage(roomId, body) {
      const txnId = Date.now().toString(36) + Math.random().toString(36).slice(2);
      return _fetch('PUT',
        `/_matrix/client/v3/rooms/${encodeURIComponent(roomId)}/send/m.room.message/${txnId}`,
        { msgtype: 'm.text', body }, true);
    },

    async searchAllMessages(query) {
      // Use /user_directory for user search; for message search we'd need server support
      // Fallback: return empty
      return [];
    },

    async threadMessages(roomId, rootEventId, limit = 50) {
      return [];
    },

    async sendThreadMessage(roomId, rootEventId, body) {
      const txnId = Date.now().toString(36) + Math.random().toString(36).slice(2);
      return _fetch('PUT',
        `/_matrix/client/v3/rooms/${encodeURIComponent(roomId)}/send/m.room.message/${txnId}`,
        { msgtype: 'm.text', body, 'm.relates_to': { rel_type: 'm.thread', event_id: rootEventId } }, true);
    },

    async roomThreads(roomId) { return []; },
    async rootSpaces() { return []; },
    async spaceChildren(spaceId) { return []; },

    async registerPush() { return { ok: false, reason: 'not implemented' }; },

    async userDirectory(query, limit = 10) {
      const data = await _fetch('POST', '/_matrix/client/v3/user_directory/search',
        { search_term: query, limit }, true);
      return (data.results || []).map(r => ({
        userId: r.user_id, displayName: r.display_name, avatarUrl: r.avatar_url
      }));
    },

    async createDirectChat(userId) {
      const data = await _fetch('POST', '/_matrix/client/v3/createRoom', {
        preset: 'trusted_private_chat',
        invite: [userId],
        is_direct: true
      }, true);
      return { id: data.room_id, name: userId };
    },

    async register(homeserver, username, password) {
      _hs = homeserver;
      // Step 1: start registration
      const data = await _fetch('POST', '/_matrix/client/v3/register', {
        username, password,
        auth: { type: 'm.login.dummy' },
        initial_device_display_name: 'Chator Web'
      });
      if (data.access_token) {
        _userId = data.user_id;
        _accessToken = data.access_token;
        _deviceId = data.device_id;
        localStorage.setItem('chator_hs', _hs);
        localStorage.setItem('chator_uid', _userId);
        localStorage.setItem('chator_token', _accessToken);
        localStorage.setItem('chator_device', _deviceId);
      }
      return data;
    },

    // Helper: send arbitrary state
    async sendState(roomId, eventType, content, stateKey) {
      const path = `/_matrix/client/v3/rooms/${encodeURIComponent(roomId)}/state/${eventType}` +
        (stateKey ? `/${encodeURIComponent(stateKey)}` : '');
      return _fetch('PUT', path, content, true);
    }
  };
})();
