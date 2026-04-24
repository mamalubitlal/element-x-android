# Чатор Website Specification

## Project Overview
- **Project name**: Чатор Landing Page
- **Type**: Marketing website (single page)
- **Core functionality**: Landing page for Russian Matrix messenger Android app
- **Target users**: Russian-speaking Android users looking for a private messenger

## Tech Stack
- **Framework**: Vite + React 19
- **Styling**: Vanilla CSS (no Tailwind)
- **Language**: Russian (primary), English (toggle)

## UI/UX Specification

### Layout Structure
```
[Nav: Logo | Links | Lang Toggle]
[Hero: Headline | Subhead | CTA | Phone Mockup]
[Features: 4-column grid]
[About: Text + Matrix explanation]
[Download: APK + Build instructions]
[Team: Solo dev card]
[Footer]
```

### Visual Design

**Theme**: "Soft Brutalist" — Minimal but alive
- Clean layouts with subtle depth
- Monochromatic base with single accent
- Hidden animations that reveal on scroll/interaction

**Colors**:
- `--bg`: `#F5F5F0` (warm off-white)
- `--surface`: `#FFFFFF`
- `--text`: `#1A1A1A`
- `--text-muted`: `#6B6B6B`
- `--accent`: `#E63946` (bold red)
- `--line`: `#E0E0E0`

**Typography**:
- Headings: "Unbounded" (Google Fonts) — geometric, distinctive
- Body: "IBM Plex Sans" — technical, readable

**Spacing**:
- Section padding: `clamp(4rem, 10vw, 8rem)`
- Content max-width: `1100px`
- Grid gap: `1.5rem`

### Components

**Nav**:
- Fixed top, transparent → white on scroll
- Logo (Чаtor icon + text)
- Links: Функции, О программе, Скачать
- Lang toggle: RU ↔ EN

**Hero**:
- Large headline with accent word
- 2-sentence subhead
- Two buttons: Скачать (primary), Исходный код (outline)
- Phone mockup with chat UI (CSS only, no image needed)

**Features Grid** (4 items):
1. 🇷🇺 Russian interface
2. 🔒 E2E encryption
3. 📞 Jitsi calls
4. 🔓 DPI bypass

**About Section**:
- What is Matrix
- Tech stack: Rust, Compose, Kotlin
- Open source + AGPL

**Download Section**:
- Direct APK link (GitHub Releases)
- Build from source instructions
- Requirements: Android 8.0+

**Team Section**:
- Solo developer card
- Name, role (creator)
- Contact (GitHub link)
- Brief bio

**Footer**:
- AGPL license
- GitHub link
- "Created by [name]"

### Animations
- Reveal on scroll (transform + opacity)
- Link underlines on hover
- Button fill on hover
- Phone mockup subtle float
- No jarring transitions

## Functionality

### Core Features
- Language toggle ( RU ↔ EN, persists to localStorage )
- Smooth scroll to sections
- Responsive (mobile → desktop)
- External links to GitHub/Releases

### Interactions
- Nav links → smooth scroll
- CTA buttons → open in new tab
- Lang toggle → instant switch, no reload

## Content (Russian)

### Hero
- **Headline**:「Чатор」— Matrix-мессенджер для Android
- **Subhead**: Приватный чат с шифрованием, без сбора данных и с обходом DPI

### Features
- Русский интерфейс
- Сквозное шифрование
- Звонки через Jitsi
- Обход DPI-фильтрации

### About
- Что такое Matrix — открытый протокол для коммуникации
- Технологии: Rust SDK, Jetpack Compose, Kotlin

### Download
- Скачать APK
- Собрать из исходников

### Team
- Один разработчик (you)

---

## Acceptance Criteria

- [ ] Page loads without errors
- [ ] All sections visible and styled
- [ ] Lang toggle works and persists
- [ ] Smooth scroll works
- [ ] Responsive at 375px, 768px, 1440px
- [ ] No console errors
- [ ] External links open correctly