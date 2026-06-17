// Chator Web — service worker.
// Receives push events from the matrix.org push gateway and shows a
// browser notification. Tapping the notification focuses the app window
// (or opens a new tab if the app isn't already open).

self.addEventListener("install", (e) => self.skipWaiting());
self.addEventListener("activate", (e) => e.waitUntil(self.clients.claim()));

self.addEventListener("push", (e) => {
  // Matrix push gateway payload: { "event_id": "...", "room_id": "...", "sender": "...", "type": "m.room.message" }
  // for "format": "event_id_only" we get a notification. For full event we get the full JSON.
  let payload = {};
  try { payload = e.data ? e.data.json() : {}; } catch (_) {}

  const title = payload.sender_displayname || payload.sender || "Chator";
  const body  = payload.content?.body || "Новое сообщение";
  const tag   = payload.room_id || "chator";

  e.waitUntil(
    self.registration.showNotification(title, {
      body,
      tag,
      badge: "icons/chator-192.png",
      icon: "icons/chator-192.png",
      data: { roomId: payload.room_id, eventId: payload.event_id },
    })
  );
});

self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  const roomId = e.notification.data?.roomId;
  e.waitUntil((async () => {
    const all = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
    if (all.length > 0) {
      const c = all[0];
      await c.focus();
      if (roomId) c.postMessage({ type: "open-room", roomId });
    } else {
      self.clients.openWindow(roomId ? `/?room=${roomId}` : "/");
    }
  })());
});
