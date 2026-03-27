# чатор Ralph Loop — Android Integration

Autonomous AI agent loop for integrating OIDC-less auth + ByeDPI into чатор Android (Element X fork).

---

## 📁 File Structure

```
chator-ralph/
├── PROMPT.md              # Agent instructions (fed every iteration)
├── AGENT.md               # Project conventions & learned lessons
├── fix_plan.md            # Dynamic task tracker (updated each iteration)
├── progress.txt           # Iteration log
├── ralph.sh               # Main loop script (run this!)
├── README.md              # This file
└── specs/
    ├── 01-oidc-less-auth.md       # Login/register spec (Element Web reference)
    ├── 02-dpi-bypass.md           # ByeDPI integration spec (TODO)
    └── 03-strategy-picker.md      # Strategy picker UI spec (TODO)
```

---

## 🚀 Quick Start

```bash
cd /mnt/data/openclaw/workspace/.openclaw/workspace/chator-ralph
./ralph.sh
```

The loop will:
1. Read `fix_plan.md` and pick the next TODO item
2. Run OpenCode with `PROMPT.md` as instructions
3. Implement the item fully (no placeholders!)
4. Run backpressure (tests, lint, build)
5. Update `fix_plan.md` with results
6. Commit changes
7. Repeat until all items complete

---

## 🎯 Current Goals

### Phase 1: OIDC-Less Auth (Element Web Style) ✅ SPEC READY
- Simple username/password login (NO OIDC/SSO)
- Direct Matrix API calls (`/login`, `/register`)
- Reference: Element Web `Login.ts`, `Login.tsx`, `Registration.tsx`
- Russian localization
- **Spec:** `specs/01-oidc-less-auth.md`

### Phase 2: ByeDPI Integration ✅ SPEC READY
- 71 strategies auto-test
- First-boot + network change detection
- Per-network strategy storage
- Background worker (no UI blocking)
- **Spec:** `specs/02-dpi-bypass.md`
- **Reference:** `../chator-dpi-tester/` (working implementation)

### Phase 3: Strategy Picker UI ✅ SPEC READY
- Compose UI for strategy selection
- Manual test trigger
- Settings + Bug Report integration
- **Spec:** `specs/03-strategy-picker.md`

---

## 📖 Key References

### OIDC-Less Auth
- **Element Web Source:** https://github.com/element-hq/element-web
- **Login.ts:** https://github.com/element-hq/element-web/blob/develop/apps/web/src/Login.ts
- **Login.tsx:** https://github.com/element-hq/element-web/blob/develop/apps/web/src/components/structures/auth/Login.tsx
- **Registration.tsx:** https://github.com/element-hq/element-web/blob/develop/apps/web/src/components/structures/auth/Registration.tsx
- **Spec:** `specs/01-oidc-less-auth.md`

### ByeDPI
- **Reference Implementation:** `../chator-dpi-tester/`
- **Feature Summary:** `../chator-dpi-tester/FEATURE_SUMMARY.md`
- **Integration Guide:** `../chator-dpi-tester/INTEGRATION_GUIDE.md`

### чатор Project
- **Element X Fork:** https://github.com/mamalubitlal/element-x-android
- **Server:** https://chator-server.onrender.com (currently down)
- **Domain:** chator.k.vu

---

## 📊 Status

| Component | Status | Notes |
|-----------|--------|-------|
| Server | ❌ DOWN | Since ~04:00 UTC (needs manual wake) |
| Build #68 | ⚠️ Partial | Debug APK ✅, Release APK ❌ (signing issue) |
| Ralph Loop | ✅ Ready | All files created |
| OIDC-Less Spec | ✅ Complete | `specs/01-oidc-less-auth.md` |
| DPI Bypass Spec | ✅ Complete | `specs/02-dpi-bypass.md` |
| Strategy Picker Spec | ✅ Complete | `specs/03-strategy-picker.md` |

---

## 🛠️ Manual Tasks (Human Required)

1. **Wake Render Server**
   - Visit https://dashboard.render.com
   - Find `chator-matrix` service
   - Click to wake (or curl the endpoint)

2. **Fix Release APK Signing**
   - Build #68 failed at "Build GPlay Release APK (unsigned)"
   - Need signing config or skip for now (debug APK works)

3. **Review Iterations**
   - Check `progress.txt` after each run
   - Review git commits: `git log --oneline`
   - Intervene if agent goes off-track

---

## 💡 Tips

- **One iteration = one task** — don't rush, let agent focus
- **Check fix_plan.md** — see what's done vs. remaining
- **Server down?** — agent can still work on UI/code without server
- **Build fails?** — debug APK from Actions tab works for testing

---

**Good luck! 🥞🚀**
