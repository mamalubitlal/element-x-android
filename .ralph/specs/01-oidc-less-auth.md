# Spec: OIDC-Less Login & Registration (Element Web Style)

## Objective

Implement simple username/password authentication for чатор Android **without OIDC/SSO complexity**.

Reference: Element Web source code (`Login.ts`, `Login.tsx`, `Registration.tsx`)

---

## Requirements

### 1. Login Screen

**UI Components:**
- Username field (text input)
- Password field (secure input)
- "Login" button
- "Create Account" link → navigate to registration
- "Edit Server" link (advanced, hidden by default)

**Flow:**
```
1. User enters username + password
2. App calls GET /_matrix/client/v3/login
3. Check if m.login.password flow is available
4. POST /_matrix/client/v3/login with credentials
5. On success: store tokens, navigate to chat list
6. On failure: show error (wrong password, user not found)
```

**API Call:**
```kotlin
POST /_matrix/client/v3/login
Content-Type: application/json

{
  "type": "m.login.password",
  "identifier": {
    "type": "m.id.user",
    "user": "uggan"
  },
  "password": "secret123",
  "initial_device_display_name": "чатор Android"
}

Response:
{
  "access_token": "syT_c29tZXRoaW5n...",
  "user_id": "@uggan:chator.k.vu",
  "device_id": "ABCDEFGH",
  "home_server": "chator.k.vu"
}
```

**Reference:** Element Web `Login.ts` lines 126-168 (`loginViaPassword()`)

---

### 2. Registration Screen

**UI Components:**
- Username field (text input)
- Password field (secure input)
- Confirm password field
- Email field (optional, for account recovery)
- "Create Account" button
- "Back to Login" link

**Flow:**
```
1. User enters username + password + email
2. App calls POST /_matrix/client/v3/register
3. Server returns 401 with UIA flows (expected!)
4. Complete required auth stages:
   - m.login.terms (accept ToS)
   - m.login.email.identity (verify email)
5. On completion: server returns tokens (auto-login)
6. Navigate to chat list
```

**Initial Registration Call:**
```kotlin
POST /_matrix/client/v3/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "secret123",
  "initial_device_display_name": "чатор Android"
}

Response (401 Unauthorized - EXPECTED!):
{
  "flows": [
    {
      "stages": [
        "m.login.terms",
        "m.login.email.identity"
      ]
    }
  ],
  "session": "abc123session"
}
```

**Complete UIA Stage (Email):**
```kotlin
POST /_matrix/client/v3/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "secret123",
  "auth": {
    "type": "m.login.email.identity",
    "session": "abc123session",
    "threepid_creds": {
      "sid": "emailSidHere"
    }
  }
}
```

**Reference:** Element Web `Registration.tsx` lines 189-230 (initial request), lines 300-320 (UIA)

---

### 3. Server Discovery

**Check `.well-known` endpoint:**
```kotlin
GET https://chator.k.vu/.well-known/matrix/client

// If NO m.authentication field → OIDC-less mode available
{
  "m.homeserver": {
    "base_url": "https://chator.k.vu"
  }
}

// If m.authentication present → OIDC mode (skip password login)
{
  "m.authentication": {
    "issuer": "https://auth.example.com"
  }
}
```

**Reference:** Element Web `Login.ts` lines 97-118 (`getFlows()`)

---

## Acceptance Criteria

- [ ] Login screen shows username/password fields (no SSO buttons)
- [ ] Registration screen with username/password/email
- [ ] `GET /login` called to detect available flows
- [ ] `POST /login` with `m.login.password` type on submit
- [ ] Registration handles 401 UIA response correctly
- [ ] Email verification flow works (request token → submit)
- [ ] Tokens stored securely (EncryptedSharedPreferences)
- [ ] Auto-login on app restart (if tokens valid)
- [ ] Russian localization for all UI strings
- [ ] Error handling: network errors, wrong credentials, registration disabled

---

## Related Files

**Element Web Reference:**
- `apps/web/src/Login.ts` — Core login logic (lines 97-168)
- `apps/web/src/components/structures/auth/Login.tsx` — Login UI (lines 378-417)
- `apps/web/src/components/structures/auth/Registration.tsx` — Registration UI (lines 189-320)
- `apps/web/src/utils/oidc/isUserRegistrationSupported.ts` — OIDC registration check

**чатор Android Implementation:**
- `app/src/main/kotlin/io/element/android/features/login/` — Create this directory
- `app/src/main/kotlin/io/element/android/features/register/` — Create this directory
- `app/src/main/kotlin/io/element/android/features/auth/MatrixAuthService.kt` — New service
- `app/src/main/res/values-ru/strings.xml` — Russian strings

---

## Implementation Notes

### DO:
- ✅ Use Element Web code as REFERENCE for flow logic
- ✅ Implement direct Matrix API calls (`/login`, `/register`)
- ✅ Handle UIA stages for registration
- ✅ Store tokens securely (Android Keystore + EncryptedSharedPreferences)
- ✅ Russian localization for all strings
- ✅ Compose UI matching Element X style

### DON'T:
- ❌ Implement OIDC/SSO flow (too complex)
- ❌ Add external identity providers (Google, GitHub, etc.)
- ❌ Use Dex or other OIDC providers
- ❌ Add server picker (pre-configured to chator.k.vu)
- ❌ Show SSO buttons on login screen

---

## Testing

**Unit Tests:**
- MatrixAuthService tests (mock API responses)
- Token storage tests
- UIA flow state machine tests

**Integration Tests:**
- Login with valid credentials → success
- Login with invalid credentials → error
- Registration with email verification → success
- Registration disabled → show error

**Manual Testing:**
1. Fresh install → login screen shown
2. Login with existing account → chat list
3. Register new account → email verification → chat list
4. Force logout → back to login
5. Kill app → restart → auto-login

---

## Russian Strings

```xml
<!-- Login -->
<string name="login_title">Вход</string>
<string name="login_username">Имя пользователя</string>
<string name="login_password">Пароль</string>
<string name="login_button">Войти</string>
<string name="login_create_account">Создать аккаунт</string>
<string name="login_error_invalid_credentials">Неверное имя пользователя или пароль</string>
<string name="login_error_network">Ошибка сети</string>

<!-- Registration -->
<string name="register_title">Создание аккаунта</string>
<string name="register_username">Имя пользователя</string>
<string name="register_password">Пароль</string>
<string name="register_password_confirm">Подтвердите пароль</string>
<string name="register_email">Email (необязательно)</string>
<string name="register_button">Создать</string>
<string name="register_back_to_login">Вернуться ко входу</string>
<string name="register_error_username_taken">Имя пользователя занято</string>
<string name="register_error_password_too_short">Пароль слишком короткий</string>
<string name="register_email_verification">Проверьте email для завершения регистрации</string>

<!-- Server -->
<string name="server_edit">Изменить сервер</string>
<string name="server_url">URL сервера</string>
<string name="server_default">https://chator.k.vu</string>
```

---

## Success Metrics

- Login completes in < 3 seconds (good network)
- Registration completes in < 5 minutes (including email verification)
- Zero OIDC-related crashes
- 100% Russian UI coverage
- All tests pass (unit + integration)
