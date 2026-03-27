# Ralph Loop Instructions (OpenCode)

## Your Task

Read @fix_plan.md and choose the **MOST IMPORTANT** incomplete item.
Implement it **fully**. Do NOT create placeholder implementations.

## Project Context: чатор (chator)

**What:** Matrix messenger for Russian users (alternative to VK/Telegram)
**Stack:** 
- Android: Element X fork (Kotlin, Gradle)
- Server: Matrix Synapse + Supabase PostgreSQL
- DPI Bypass: ByeDPI integration (71 strategies, auto-test)

**GitHub:** https://github.com/mamalubitlal/element-x-android (develop branch)
**Server:** https://chator-server.onrender.com (currently down)
**Domain:** chator.k.vu (DNS pending)

## 🔐 CRITICAL: OIDC-LESS AUTH (Element Web Reference)

**DO NOT implement OIDC/SSO complexity.** Use Element Web's OIDC-LESS flow as reference:

### Login Flow (from Element Web `Login.ts`)
1. Check `/.well-known/matrix/client` — if NO `m.authentication` field → OIDC-LESS mode
2. Call `GET /_matrix/client/v3/login` → get available flows
3. If `m.login.password` in flows → show username/password form
4. On submit: `POST /_matrix/client/v3/login` with body:
```json
{
  "type": "m.login.password",
  "identifier": {"type": "m.id.user", "user": "username"},
  "password": "secret123",
  "initial_device_display_name": "чатор Android"
}
```
5. Store returned: `access_token`, `user_id`, `device_id`, `home_server`

### Registration Flow (from Element Web `Registration.tsx`)
1. Call `POST /_matrix/client/v3/register` with username/password
2. Expect 401 response with UIA flows (User-Interactive Authentication)
3. Complete required stages (email verification if needed)
4. On success: auto-login with returned tokens

### Key Files to Study
- Element Web `Login.ts` — flow detection, password auth logic
- Element Web `Login.tsx` — password form rendering
- Element Web `Registration.tsx` — UIA handling
- Element Web `isUserRegistrationSupported.ts` — OIDC registration check

**Server Config (Synapse):**
```yaml
# NO oidc_providers section = OIDC-less mode
oidc_providers: []

password_config:
  enabled: true
  localdb_enabled: true
```

## Rules

1. Read relevant specs from @specs/ for context
2. Implement the selected item **completely** — no TODOs, no placeholders
3. Run tests and validation (backpressure) after each change
4. Update @fix_plan.md with results (mark complete, add bugs found)
5. If stuck, break the problem down and reason step by step
6. Use shell commands for: searching codebase, running tests, updating docs
7. Make changes directly — no subagent abstraction needed

## Constraints

- **ONE item per iteration** — focus on completing it fully
- **NO placeholder code** — full implementations only
- **Element Web style login** — simple username/password, NO OIDC/SSO complexity
- **DPI bypass built-in** — automatic strategy testing, no separate apps
- **Russian-first** — all UI in Russian by default
- **Simplicity principle** — all-in-one solution, no modular components

## Completion Criteria (MUST PASS BEFORE STOPPING)

- ✅ All existing tests pass (./gradlew test)
- ✅ No lint errors (./gradlew lint)
- ✅ No type errors (Kotlin compiler clean)
- ✅ fix_plan.md updated with current state
- ✅ No temporary/debug files left behind
- ✅ Russian localization added for new UI strings
- ✅ Build succeeds (./gradlew assembleDebug)

## OpenCode-Specific

- You are running in OpenCode, an open-source AI coding assistant
- You have access to shell commands, file read/write, and search tools
- Each session is fresh — rely on files for persistence, not conversation history
- Use `git diff` to understand what changed in previous iterations
- Working directory: /mnt/data/openclaw/workspace/.openclaw/workspace/chator

## Key Files

- **fix_plan.md** — Dynamic task tracker (read this first!)
- **specs/*.md** — Feature specifications
- **AGENT.md** — Project conventions and learned lessons
- **progress.txt** — Iteration log
- **chator/** — Main Element X fork source code
- **chator-dpi-tester/** — ByeDPI integration reference

## Start Each Iteration

```bash
1. git status (see what changed)
2. cat fix_plan.md (pick next TODO item)
3. Read relevant spec from specs/
4. Implement fully
5. Run backpressure (tests, lint, build)
6. Update fix_plan.md
7. Commit
```
