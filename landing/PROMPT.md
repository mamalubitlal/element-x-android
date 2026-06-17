# BUILD PROMPT: Чатор Landing Page

You are building a marketing landing page for **Чатор** — a Matrix messenger for Android (fork of Element X). Target audience: Russian-speaking Android users.

> **You have 3D capabilities (Three.js / R3F). USE THEM HEAVILY.** This is the main feature of this build.

---

## BRAND KIT (from the actual app source code)

### Colors
```
--blue-primary:  #389CFF
--blue-dark:     #1E6FD9
--blue-light:    #6BB3FF
--blue-deeper:   #1558A8
--bg-canvas:     #FFFFFF
--bg-surface:    #F0F2F5
--bg-me:         #E1E6EC
--text-primary:  #1A1C20
--text-secondary:#6F7885
--border:        #E1E6EC
--onboarding-grad: linear-gradient(90deg, #0DBDA8, #0D5CBD)
--send-gradient:  linear-gradient(180deg, #1558A8, #1E6FD9, #389CFF, #6BB3FF)
```

### Typography
- **Headings:** Unbounded (Google Fonts) — geometric, distinctive
- **Body:** Inter (Google Fonts) — clean, technical

### Logo
- The Cyrillic letter **«Ч»** in blue (#389CFF), Unbounded font, is the logo mark
- Full logo: **«Ч»атор**

### App Design Language (replicate these)
- Background: pure white (#FFFFFF)
- Buttons: pill-shaped (border-radius: 999px), filled blue (#389CFF), 48px min-height
- Onboarding gradient at bottom: teal->blue (#0DBDA8 → #0D5CBD), subtle opacity
- Message bubbles: radius 12px, light gray (#F0F2F5 for received, #E1E6EC for sent)
- Cards: radius 16px, white bg, 1px solid #E1E6EC border
- No heavy shadows — clean and minimal

### Key Links
- APK download: https://github.com/chator-im/element-x-android/releases
- Source code: https://github.com/chator-im/element-x-android

---

## LAYOUT

```
[Nav: Ч Logo | Features · About · Download | RU/EN]

[Hero: 
  Badge: "For Android 8.0+"
  Headline: "«Чатор» — приватный мессенджер, которому можно доверять"
  Subhead: "Чатор — форк Element X на Matrix..."
  [CTA: "Скачать APK"] [Ghost: "Исходный код"]
  3D Phone Mockup with chat animation
]

[Features: 4x cards in grid — E2E encryption · Russian UI · Voice/video calls · Rich messaging]

[About: 2 columns — "What is Matrix" + "Tech Stack" with tags]

[Download: 2 cards — APK download card · Build from source card]

[Team: Solo developer card with Ч avatar]

[Footer: Ч · "Built with purpose." · GitHub]
```

---

## 3D & ANIMATION REQUIREMENTS (MANDATORY)

### 1. HERO — 3D Phone Mockup
Create a 3D phone model (Three.js or React Three Fiber). Not CSS — actual 3D.
- Subtle tilt (3-5°) following mouse position
- Floating animation (slowly bobbing up and down)
- Inside the phone screen: chat messages appearing sequentially with animation
- Background: animated mesh/grid network (reference to Matrix protocol), blue, pulsing softly
- Elements fly in from different directions on page load with fade

### 2. 3D «Ч» Logo
- The Cyrillic letter `Ч` as a 3D object with blue gradient material
- Slowly rotates in 3D space
- On scroll: changes Z-axis angle
- On hover: small particle burst from the letter
- Used in Nav (small) and as a massive semi-transparent background element

### 3. Matrix Grid Background
- Animated grid pattern visible behind the hero and floating between sections
- Lines glow in blue (#389CFF → transparent) 
- Scroll-driven parallax (moves at 0.3x scroll speed)
- Fades in/out between sections

### 4. Feature Cards — 3D Tilt
- On hover: card lifts and tilts in 3D (tilt effect)
- Feature icons are small 3D objects (lock for encryption, flag for Russian UI, phone for calls, message for rich messaging) — OR use emoji as textures on 3D planes
- Cards fly in from alternating sides on scroll (staggered, fan-like entrance)

### 5. Scroll Parallax
- Multiple depth layers moving at different speeds
- Section titles move faster than content
- Background elements drift slowly

### 6. Download Button — Particle Burst
- On hover over "Download APK" button: blue particles burst outward
- On click: ripple wave across the screen

### 7. Micro-animations
- Nav links: blue underline animated from left
- Buttons: scale(1.02) + glow on hover
- Language toggle: smooth flip transition
- Custom smooth scroll with progress indicator on edge
- Section entrances: staggered, with slight rotation + fade

### 8. Team Section
- Dev avatar: 3D geometry (sphere or cube with blue gradient)
- On hover: rotates / unfolds

### 9. Loading Screen
- Pulsing animated «Ч» with progress indication
- Smooth fade to main content

### 10. Footer
- Tiny floating particles (stars) drifting slowly

---

## TECHNICAL REQUIREMENTS

### Stack
- **3D:** Three.js or React Three Fiber (@react-three/fiber + @react-three/drei)
- **Animations:** Framer Motion or GSAP
- **Build:** Vite + React 19
- **Styling:** CSS Modules or vanilla CSS (NOT Tailwind)

### Performance
- MUST run at 60fps on mobile devices
- Implement LOD: simplify/reduce 3D on mobile
- GPU-accelerated animations (transform, opacity)
- Lazy-load 3D elements below the fold
- Use `will-change` on animated elements

### Responsiveness
- Mobile (<768px): disable complex 3D, replace with 2D animations or static content
- Phone 3D → flat image on mobile
- Tablet (768-1024px): reduce particle count, simplify 3D

### Accessibility
- Respect `prefers-reduced-motion`: disable all animations
- Respect `prefers-color-scheme`: optional dark mode
- Semantic HTML: `section`, `nav`, `article`, `h1-h3`
- All interactive elements keyboard-accessible

### i18n
- Two languages: Russian (default) + English
- Switch without page reload
- Persist choice in localStorage
- All 3D elements must NOT contain text (they're language-agnostic)

---

## CONTENT

### Russian (RU)

| Section | Russian |
|---|---|
| Nav | Возможности · О проекте · Скачать |
| Hero badge | Для Android 8.0+ |
| Hero title | «Чатор» — приватный мессенджер, которому можно доверять |
| Hero sub | Чатор — форк Element X на Matrix. Сквозное шифрование, русский интерфейс. Без компромиссов. |
| Features title | Что умеет Чатор |
| Feature 1 | 🔐 Сквозное шифрование — Сообщения видите только вы и собеседник. Ни сервер, ни провайдер не могут их прочесть |
| Feature 2 | 🇷🇺 Русский интерфейс — Полная локализация: меню, уведомления, подсказки — всё на родном языке с первого запуска |
| Feature 3 | 📞 Аудио- и видеозвонки — Защищённые вызовы через Matrix VoIP с end-to-end шифрованием |
| Feature 4 | 💬 Богатые сообщения — Реакции, опросы, голосовые сообщения, редактирование, пересылка и спейсы |
| About title | О проекте |
| Matrix subtitle | Matrix — открытый протокол |
| Matrix text | Matrix — это децентрализованный протокол для коммуникации. Вы выбираете сервер, контролируете данные и общаетесь с кем угодно, независимо от приложения. Чатор использует Matrix Rust SDK — ту же надёжную основу, что и Element. |
| Tech subtitle | Стек технологий |
| Tech text | Rust SDK (шифрование), Jetpack Compose (UI), Kotlin (логика). Минимум зависимостей, максимум контроля. |
| Download title | Скачать |
| APK title | APK (GitHub Releases) |
| APK desc | Последняя стабильная версия. Установите вручную. |
| Build title | Собрать из исходников |
| Build desc | Клонируйте репозиторий и соберите приложение: |
| Build cmd | `./gradlew :app:assembleGplayDebug` |
| Team title | Разработчик |
| Team role | Создатель проекта |
| Team bio | Сделано одним разработчиком, которому не хватило русскоязычного, приватного и свободного мессенджера на Android. Форкнул Element X, перевёл интерфейс — получился Чатор. |
| Footer caption | Сделано с целью. |
| Footer links | GitHub |

### English (EN)

| Section | English |
|---|---|
| Nav | Features · About · Download |
| Hero badge | For Android 8.0+ |
| Hero title | Chator — a private messenger you can trust |
| Hero sub | Chator is a fork of Element X on Matrix. End-to-end encryption, Russian interface. No compromises. |
| Features title | What Chator can do |
| Feature 1 | 🔐 E2E Encryption — Only you and the recipient can read your messages. Not even the server can decrypt them |
| Feature 2 | 🇷🇺 Russian UI — Fully localized interface from the first launch |
| Feature 3 | 📞 Voice & Video Calls — Secure calls via Matrix VoIP with end-to-end encryption |
| Feature 4 | 💬 Rich Messaging — Reactions, polls, voice messages, edits, forwarding, and spaces |
| About title | About |
| Matrix subtitle | Matrix — open protocol |
| Matrix text | Matrix is a decentralized protocol for communication. You choose your server, control your data, and talk to anyone. Chator uses the Matrix Rust SDK. |
| Tech subtitle | Tech stack |
| Tech text | Rust SDK (encryption), Jetpack Compose (UI), Kotlin (logic). Minimal dependencies, maximum control. |
| Download title | Download |
| APK title | APK (GitHub Releases) |
| APK desc | Latest stable release. Install manually. |
| Build title | Build from source |
| Build desc | Clone the repository and build: |
| Build cmd | `./gradlew :app:assembleGplayDebug` |
| Team title | Developer |
| Team role | Project creator |
| Team bio | Built by one developer who needed a Russian-language, private messenger on Android. Forked Element X, translated the UI — Chator was born. |
| Footer caption | Built with purpose. |
| Footer links | GitHub |

---

## FILE STRUCTURE

```
/landing/
├── public/
│   └── models/          (optional — 3D models if needed)
├── src/
│   ├── components/
│   │   ├── Nav.jsx
│   │   ├── Hero.jsx
│   │   ├── HeroScene3D.jsx     (Three.js scene: phone + particles + grid)
│   │   ├── Features.jsx
│   │   ├── FeatureCard3D.jsx   (3D tilt on hover)
│   │   ├── About.jsx
│   │   ├── Download.jsx
│   │   ├── Team.jsx
│   │   ├── Footer.jsx
│   │   ├── MatrixGrid.jsx      (animated grid background)
│   │   ├── ChLogo3D.jsx        (3D Ч letter)
│   │   └── ParticleSystem.jsx  (particles)
│   ├── hooks/
│   │   ├── useScrollParallax.js
│   │   └── useMousePosition.js
│   ├── i18n.jsx
│   ├── App.jsx
│   ├── App.css
│   └── main.jsx
├── index.html
├── vite.config.js
├── package.json
└── PROMPT.md (this file)
```

---

## i18n Implementation

Create a `i18n.jsx` with:
- A `translations` object with `ru` and `en` keys containing all translated strings
- A `LanguageProvider` React context
- A `useLanguage()` hook returning `{ lang, setLang, t }`
- Persist language choice to `localStorage`

---

## ACCEPTANCE CRITERIA

| # | Criterion |
|---|-----------|
| 1 | Page loads without errors |
| 2 | 3D phone is rendered and responds to mouse movement |
| 3 | 3D «Ч» is animated, responds to scroll + hover with particles |
| 4 | Matrix grid background is visible with parallax effect |
| 5 | Feature cards have 3D tilt on hover, staggered entrance on scroll |
| 6 | Particle burst on Download button hover |
| 7 | All hover/click micro-animations work smoothly |
| 8 | Language toggle works and persists across reload |
| 9 | Runs at 60fps on mobile (3D simplified) |
| 10 | Respects `prefers-reduced-motion` |
| 11 | Responsive at 375px, 768px, 1440px |
| 12 | No console errors |
| 13 | All external links open in new tab with rel=noopener |

---

## DELIVER

Build the full project at `C:\chtor\landing\`. The directory already exists with placeholder files — replace them completely. Run `npm install && npm run build` and verify no errors.
