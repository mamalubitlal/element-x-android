/* ============================================
   Chator — SPA Router + All Screens
   ============================================ */

// --- State ---
const State = {
  screen: 'onboarding',
  rooms: [],
  currentRoom: null,
  messages: [],
  filter: 0,
};

const $ = sel => document.querySelector(sel);
const app = () => $('#app');

// --- SVG Icons ---
const Icons = {
  back: '←',
  search: '🔍',
  filter: '≡',
  add: '+',
  send: '→',
  chat: '💬',
  people: '👥',
  settings: '⚙',
  person: '<svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#999" stroke-width="1.5"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6"/></svg>',
  lock: '<svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#999" stroke-width="1.5"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 118 0v4"/></svg>',
  eyeOff: '👁‍🗨',
  eyeOn: '👁',
  home: '🏠',
};

const avatarColors = ['#E91E63','#9C27B0','#3F51B5','#009688','#795548','#607D8B','#FF5722','#00BCD4'];

function avatarColor(name) {
  let h = 0;
  for (let i = 0; i < name.length; i++) h = ((h << 5) - h + name.charCodeAt(i)) | 0;
  return avatarColors[Math.abs(h) % avatarColors.length];
}

function timeAgo(ts) {
  if (!ts) return '';
  const diff = Date.now() - ts;
  const min = Math.floor(diff / 60000);
  const hr = Math.floor(min / 60);
  const d = Math.floor(hr / 24);
  if (min < 1) return 'сейчас';
  if (min < 60) return min + 'м';
  if (hr < 24) return hr + 'ч';
  if (d < 7) return d + 'д';
  return Math.floor(d/7) + 'н';
}

// --- Router ---
function navigate(screen, data) {
  State.screen = screen;
  if (data) Object.assign(State, data);
  render();
  history.pushState({ screen, data }, '', '#' + screen);
}

window.addEventListener('popstate', e => {
  if (e.state?.screen) {
    State.screen = e.state.screen;
    if (e.state.data) Object.assign(State, e.state.data);
    render();
  }
});

// --- Render ---
function render() {
  const s = State.screen;
  if (s === 'onboarding') renderOnboarding();
  else if (s === 'login') renderLogin();
  else if (s === 'verify') renderVerify();
  else if (s === 'home') renderHome();
  else if (s === 'chat') renderChat();
  else if (s === 'settings') renderSettings();
  else if (s === 'search') renderSearch();
  else renderOnboarding();
}

// ============================================
// SCREEN: Onboarding (Day)
// ============================================
function renderOnboarding() {
  app().innerHTML = `
    <div class="auth-screen">
      <div class="auth-screen__gradient"></div>
      <div class="auth-screen__content">
        <div class="auth-logo">
          <div class="auth-logo__box">
            <img src="img/onboarding_logo.png" alt="Chator">
          </div>
        </div>
        <div class="auth-title">Общайся свободно.</div>
        <div class="auth-subtitle">Добро пожаловать в быстрый и простой Element.</div>
        <div class="auth-bottom">
          <button class="btn btn--primary" onclick="navigate('login')">Продолжить</button>
          <div class="auth-version">Версия 1.0.0</div>
        </div>
      </div>
    </div>`;
}

// ============================================
// SCREEN: Login
// ============================================
function renderLogin() {
  let showPwd = false;
  let login = '';
  let password = '';
  let loading = false;
  let error = '';

  function html() {
    return `
    <div class="white-auth">
      <div class="topbar" style="background:var(--bg)">
        <button class="topbar__back" onclick="navigate('onboarding')">${Icons.back}</button>
      </div>
      <div class="white-auth__content">
        <div class="white-auth__icon">
          <div class="white-auth__icon-box">${Icons.person}</div>
        </div>
        <div class="white-auth__title">Вы собираетесь войти в ${MatrixAPI.homeserver().replace('https://','')}</div>
        <div class="white-auth__subtitle">Matrix — это открытая сеть для безопасной децентрализованной связи.</div>
        <div class="white-auth__label">Введите свои данные</div>
        <div class="white-auth__fields">
          <div class="field">
            <input class="field__input" type="text" placeholder="Имя пользователя" id="login-user" autocomplete="username">
          </div>
          <div class="field">
            <input class="field__input" type="${showPwd ? 'text' : 'password'}" placeholder="Пароль" id="login-pwd" autocomplete="current-password">
            <button class="field__toggle" id="toggle-pwd" type="button">${showPwd ? Icons.eyeOn : Icons.eyeOff}</button>
          </div>
        </div>
        ${error ? `<div class="error-text">${error}</div>` : ''}
        <div class="white-auth__actions">
          <button class="btn btn--secondary" id="login-submit" disabled>Продолжить</button>
        </div>
      </div>
    </div>`;
  }

  app().innerHTML = html();

  // Bind events
  const userInput = $('#login-user');
  const pwdInput = $('#login-pwd');
  const submitBtn = $('#login-submit');
  const toggleBtn = $('#toggle-pwd');

  userInput.addEventListener('input', () => {
    login = userInput.value.trim();
    submitBtn.disabled = !login || !password || loading;
  });
  pwdInput.addEventListener('input', () => {
    password = pwdInput.value;
    submitBtn.disabled = !login || !password || loading;
  });
  toggleBtn.addEventListener('click', () => {
    showPwd = !showPwd;
    pwdInput.type = showPwd ? 'text' : 'password';
    toggleBtn.innerHTML = showPwd ? Icons.eyeOn : Icons.eyeOff;
  });
  submitBtn.addEventListener('click', async () => {
    if (!login || !password || loading) return;
    loading = true;
    error = '';
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<div class="spinner" style="margin:auto"></div>';
    try {
      await MatrixAPI.login(MatrixAPI.homeserver(), login, password);
      navigate('home');
    } catch (e) {
      error = e.message || 'Ошибка входа. Проверьте данные.';
      loading = false;
      // Re-render with error
      app().innerHTML = html();
      // Re-bind after re-render
      const u2 = $('#login-user'), p2 = $('#login-pwd'), s2 = $('#login-submit'), t2 = $('#toggle-pwd');
      u2.addEventListener('input', () => { login = u2.value.trim(); s2.disabled = !login || !password; });
      p2.addEventListener('input', () => { password = p2.value; s2.disabled = !login || !password; });
      t2.addEventListener('click', () => { showPwd = !showPwd; p2.type = showPwd ? 'text' : 'password'; t2.innerHTML = showPwd ? Icons.eyeOn : Icons.eyeOff; });
      s2.addEventListener('click', arguments.callee);
    }
  });
}

// ============================================
// SCREEN: Session Verification
// ============================================
function renderVerify() {
  app().innerHTML = `
    <div class="white-auth">
      <div class="verify-topbar">
        <button class="verify-topbar__link" onclick="navigate('onboarding')">Удалить это устройство</button>
      </div>
      <div class="white-auth__content">
        <div class="verify-icon">
          <div class="verify-icon__box">${Icons.lock}</div>
        </div>
        <div class="verify-title">Подтвердите личность</div>
        <div class="verify-subtitle">Выберите способ подтверждения для настройки защищенного обмена сообщениями.</div>
        <button class="verify-link">Подробнее</button>
        <div class="verify-actions">
          <button class="btn btn--primary" onclick="navigate('home')">Использовать другое устройство</button>
          <button class="btn btn--primary" onclick="navigate('home')">Использовать ключ восстановления</button>
          <button class="btn btn--outlined" onclick="navigate('onboarding')">Не можете подтвердить?</button>
        </div>
      </div>
    </div>`;
}

// ============================================
// SCREEN: Home
// ============================================
async function renderHome() {
  const userId = MatrixAPI.currentUserId() || '';
  const initial = userId.replace('@','').charAt(0).toUpperCase() || '?';
  const filters = ['Новые', 'Пользователи', 'Комнаты'];

  app().innerHTML = `
    <div class="screen">
      <div class="home-topbar">
        <div class="home-topbar__avatar" style="background:#8B0000">${initial}</div>
        <div class="home-topbar__title">Чаты</div>
        <button class="home-topbar__action" onclick="navigate('search')">${Icons.search}</button>
        <button class="home-topbar__action">${Icons.filter}</button>
      </div>
      <div class="chips-row" id="chips">
        ${filters.map((f, i) => `<button class="chip ${i === State.filter ? 'chip--selected' : ''}" data-idx="${i}">${f}</button>`).join('')}
      </div>
      <div class="room-list" id="room-list">
        <div class="loading-center"><div class="spinner"></div></div>
      </div>
      <div class="bottomnav">
        <div class="bottomnav__pill">
          <button class="bottomnav__btn bottomnav__btn--active">${Icons.chat}</button>
          <button class="bottomnav__btn">${Icons.people}</button>
        </div>
        <button class="bottomnav__fab">${Icons.add}</button>
      </div>
    </div>`;

  // Bind chips
  document.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => {
      State.filter = parseInt(chip.dataset.idx);
      document.querySelectorAll('.chip').forEach((c, i) => {
        c.className = 'chip' + (i === State.filter ? ' chip--selected' : '');
      });
    });
  });

  // Load rooms
  try {
    const rooms = await MatrixAPI.joinedRooms();
    State.rooms = rooms;
    renderRoomList(rooms);
  } catch (e) {
    $('#room-list').innerHTML = `<div class="empty-center"><div class="empty-center__title">Ошибка загрузки</div><div class="empty-center__subtitle">${e.message}</div></div>`;
  }
}

function renderRoomList(rooms) {
  const list = $('#room-list');
  if (!list) return;
  if (rooms.length === 0) {
    list.innerHTML = `<div class="empty-center"><div class="empty-center__title">Нет комнат</div><div class="empty-center__subtitle">Начните новый чат</div></div>`;
    return;
  }
  list.innerHTML = rooms.map((room, i) => {
    const color = avatarColor(room.name);
    const initial = (room.name || '#').charAt(0).toUpperCase();
    return `
      <div class="room-item" data-idx="${i}">
        <div class="room-item__avatar" style="background:${color}20;color:${color}">${initial}</div>
        <div class="room-item__content">
          <div class="room-item__header">
            <div class="room-item__name">${esc(room.name)}</div>
            <div class="room-item__time">${timeAgo(room.lastTimestamp)}</div>
          </div>
          ${room.lastMessage ? `<div class="room-item__preview">${esc(room.lastMessage)}</div>` : ''}
        </div>
      </div>`;
  }).join('');

  // Bind click
  list.querySelectorAll('.room-item').forEach(item => {
    item.addEventListener('click', () => {
      const idx = parseInt(item.dataset.idx);
      State.currentRoom = rooms[idx];
      navigate('chat');
    });
  });
}

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

// ============================================
// SCREEN: Chat
// ============================================
async function renderChat() {
  const room = State.currentRoom;
  if (!room) { navigate('home'); return; }

  app().innerHTML = `
    <div class="screen">
      <div class="topbar">
        <button class="topbar__back" onclick="navigate('home')">${Icons.back}</button>
        <div class="topbar__title" style="font-size:18px;font-weight:600">${esc(room.name)}</div>
      </div>
      <div class="chat-messages" id="chat-msgs">
        <div class="loading-center"><div class="spinner"></div></div>
      </div>
      <div class="composer">
        <button class="composer__attach">${Icons.add}</button>
        <textarea class="composer__input" id="composer-input" placeholder="Сообщение…" rows="1"></textarea>
        <button class="composer__send" id="composer-send" disabled>${Icons.send}</button>
      </div>
    </div>`;

  // Auto-resize textarea
  const textarea = $('#composer-input');
  const sendBtn = $('#composer-send');
  textarea.addEventListener('input', () => {
    sendBtn.disabled = !textarea.value.trim();
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
  });

  // Send
  sendBtn.addEventListener('click', async () => {
    const body = textarea.value.trim();
    if (!body) return;
    textarea.value = '';
    sendBtn.disabled = true;
    textarea.style.height = 'auto';

    // Optimistic add
    const msgEl = document.createElement('div');
    msgEl.className = 'msg msg--mine';
    msgEl.innerHTML = `${esc(body)}<div class="msg__time">…</div>`;
    $('#chat-msgs').appendChild(msgEl);
    $('#chat-msgs').scrollTop = $('#chat-msgs').scrollHeight;

    try {
      await MatrixAPI.sendMessage(room.id, body);
      msgEl.querySelector('.msg__time').textContent = formatTime(Date.now());
    } catch (e) {
      msgEl.querySelector('.msg__time').textContent = 'ошибка';
      msgEl.style.opacity = '0.5';
    }
  });

  // Ctrl+Enter to send
  textarea.addEventListener('keydown', e => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!sendBtn.disabled) sendBtn.click();
    }
  });

  // Load messages
  try {
    const msgs = await MatrixAPI.roomMessages(room.id);
    State.messages = msgs;
    renderMessages(msgs);
  } catch (e) {
    $('#chat-msgs').innerHTML = `<div class="empty-center"><div class="empty-center__title">Ошибка загрузки</div><div class="empty-center__subtitle">${esc(e.message)}</div></div>`;
  }
}

function renderMessages(msgs) {
  const container = $('#chat-msgs');
  if (!container) return;
  if (msgs.length === 0) {
    container.innerHTML = `<div class="empty-center"><div class="empty-center__title">Нет сообщений</div><div class="empty-center__subtitle">Напишите первое!</div></div>`;
    return;
  }
  container.innerHTML = msgs.map(msg => {
    const cls = msg.isMine ? 'msg--mine' : 'msg--other';
    const sender = msg.isMine ? '' : `<div class="msg__sender">${esc(msg.sender.split(':')[0].replace('@',''))}</div>`;
    return `
      <div class="msg ${cls}">
        ${sender}
        ${esc(msg.body)}
        <div class="msg__time">${formatTime(msg.timestamp)}</div>
      </div>`;
  }).join('');
  container.scrollTop = container.scrollHeight;
}

function formatTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return d.getHours().toString().padStart(2,'0') + ':' + d.getMinutes().toString().padStart(2,'0');
}

// ============================================
// SCREEN: Search
// ============================================
function renderSearch() {
  app().innerHTML = `
    <div class="screen">
      <div class="topbar">
        <button class="topbar__back" onclick="navigate('home')">${Icons.back}</button>
        <div class="topbar__title">Поиск</div>
      </div>
      <div style="padding:12px">
        <input class="field__input" type="text" placeholder="Сообщение или чат…" id="search-input" style="padding-left:16px">
      </div>
      <div class="loading-center" style="color:var(--text-tertiary)">Введите минимум 2 символа</div>
    </div>`;

  // Would debounce and call MatrixAPI.searchAllMessages() here
}

// ============================================
// SCREEN: Settings
// ============================================
function renderSettings() {
  const userId = MatrixAPI.currentUserId() || '';
  const initial = userId.replace('@','').charAt(0).toUpperCase() || '?';
  const homeserver = MatrixAPI.homeserver() || '';

  app().innerHTML = `
    <div class="screen">
      <div class="topbar">
        <button class="topbar__back" onclick="navigate('home')">${Icons.back}</button>
        <div class="topbar__title">Настройки</div>
      </div>
      <div class="settings-list">
        <div class="settings-profile">
          <div class="settings-profile__avatar">${initial}</div>
          <div class="settings-profile__info">
            <div class="settings-profile__name">${esc(userId)}</div>
            <div class="settings-profile__id">${esc(homeserver)}</div>
          </div>
        </div>

        <div class="settings-section">Настройки</div>
        <div class="settings-item">
          <div class="settings-item__icon">🔔</div>
          <div class="settings-item__text">Уведомления</div>
          <div class="settings-item__arrow">›</div>
        </div>
        <div class="settings-item">
          <div class="settings-item__icon">🔒</div>
          <div class="settings-item__text">Конфиденциальность</div>
          <div class="settings-item__arrow">›</div>
        </div>
        <div class="settings-item">
          <div class="settings-item__icon">🎨</div>
          <div class="settings-item__text">Оформление</div>
          <div class="settings-item__arrow">›</div>
        </div>

        <div class="settings-section">Информация</div>
        <div class="settings-item">
          <div class="settings-item__icon">ℹ️</div>
          <div class="settings-item__text">О приложении</div>
          <div class="settings-item__arrow">›</div>
        </div>
        <div class="settings-item">
          <div class="settings-item__icon">📋</div>
          <div class="settings-item__text">Сообщить о баге</div>
          <div class="settings-item__arrow">›</div>
        </div>

        <div class="settings-section"></div>
        <div class="settings-item" id="logout-btn">
          <div class="settings-item__icon" style="background:#FFEBEE;color:#FF6B6B">🚪</div>
          <div class="settings-item__text" style="color:#FF6B6B">Выйти</div>
        </div>
        <div style="height:40px"></div>
      </div>
    </div>`;

  $('#logout-btn').addEventListener('click', () => {
    MatrixAPI.logout();
    navigate('onboarding');
  });
}

// ============================================
// Init
// ============================================
function init() {
  // Register service worker
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }

  // Check login state
  if (MatrixAPI.isLoggedIn()) {
    navigate('home');
  } else {
    // Check hash
    const hash = location.hash.replace('#','');
    if (hash && ['login','home','chat','settings','search','verify'].includes(hash)) {
      navigate(hash);
    } else {
      navigate('onboarding');
    }
  }
}

document.addEventListener('DOMContentLoaded', init);
