// ─── Language Toggle ──────────────────────────────────────────

const translations = {
  ru: {
    meta_title: "Чатор — Свободный мессенджер",
    logo_name: "Чатор",
    nav_features: "Возможности",
    nav_about: "О технологии",
    nav_screenshots: "Скриншоты",
    nav_install: "Установка",
    nav_contribute: "Участие",
    hero_title: "Свободный мессенджер",
    hero_subtitle: "Альтернатива навязанным государством мессенджерам. Работает, не блокируется, принадлежит вам.",
    hero_download: "Скачать",
    hero_github: "GitHub",
    features_title: "Возможности",
    feature_russian_title: "Полностью русский интерфейс",
    feature_russian_desc: "Весь интерфейс переведён на русский язык — без английских вставок и недопереведённых меню.",
    feature_brand_title: "Собственный брендинг",
    feature_brand_desc: "Уникальный логотип, цветовая палитра и название — Чатор это не просто клон.",
    feature_calls_title: "Групповые звонки",
    feature_calls_desc: "Голосовые и видеозвонки через Jitsi Meet — без ограничений на количество участников.",
    feature_enterprise_title: "Enterprise-модуль",
    feature_enterprise_desc: "Дополнительные возможности для корпоративного использования — SSO, OIDC и не только.",
    feature_crypto_title: "Шифрование",
    feature_crypto_desc: "Сквозное шифрование по умолчанию на базе Matrix протокола — никто не прочитает.",
    feature_decentral_title: "Децентрализация",
    feature_decentral_desc: "Работает на любом Matrix-сервере. Нет единой точки отказа — и никто не может заблокировать.",
    about_title: "О технологии",
    about_matrix_title: "Matrix",
    about_matrix_desc: "Открытый стандарт для децентрализованной коммуникации. Серверы общаются между собой — вы не привязаны к одному провайдеру. Протокол используется в правительственных организациях, армии и больницах по всему миру.",
    about_matrix_link: "Узнать больше о Matrix",
    about_tech_title: "Технологический стек",
    about_tech_rust: "matrix-rust-sdk",
    about_tech_rust_desc: "— ядро обработки протокола Matrix на Rust",
    about_tech_compose: "Jetpack Compose",
    about_tech_compose_desc: "— современный UI на Kotlin",
    about_tech_jitsi: "Jitsi Meet",
    about_tech_jitsi_desc: "— открытая видеоконференция",
    about_tech_sso: "OIDC / SSO",
    about_tech_sso_desc: "— корпоративная аутентификация",
    screenshots_title: "Скриншоты",
    screenshots_subtitle: "Скриншоты скоро появятся",
    screenshot_placeholder: "Скриншот",
    install_title: "Установка",
    install_apk_title: "⬇️ Скачать APK",
    install_apk_desc: "Последняя стабильная версия доступна на GitHub Releases.",
    install_apk_btn: "Перейти к релизам",
    install_build_title: "🛠️ Сборка из исходников",
    install_build_desc: "Склонируйте репозиторий и откройте проект в Android Studio.",
    install_build_btn: "Подробнее",
    install_reqs_title: "Требования",
    install_req_1: "<strong>Android 7.0</strong> (API 24) и выше — обычная версия",
    install_req_2: "<strong>Android 13</strong> (API 33) и выше — Enterprise-версия",
    contribute_title: "Участие в проекте",
    contribute_step_1: "Выберите задачу из <a href=\"https://github.com/mamalubitlal/element-x-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22\" target=\"_blank\" rel=\"noopener\">good first issues</a>",
    contribute_step_2: "Напишите в комментариях, что берёте её",
    contribute_step_3: "Прочитайте <a href=\"https://github.com/mamalubitlal/element-x-android/blob/develop/CONTRIBUTING.md\" target=\"_blank\" rel=\"noopener\">руководство по контрибьюции</a>",
    contribute_btn: "Смотреть задачи",
    footer_name: "Чатор",
    footer_legal: "Лицензия: AGPL-3.0",
    footer_copy: "© 2025 Element Creations Ltd. / 2022–2025 New Vector Ltd.",
  },
  en: {
    meta_title: "Chator — Free Messenger",
    logo_name: "Chator",
    nav_features: "Features",
    nav_about: "Technology",
    nav_screenshots: "Screenshots",
    nav_install: "Install",
    nav_contribute: "Contribute",
    hero_title: "Free Messenger",
    hero_subtitle: "An alternative to state-imposed messengers. It works, it's unblockable, it's yours.",
    hero_download: "Download",
    hero_github: "GitHub",
    features_title: "Features",
    feature_russian_title: "Fully Russian interface",
    feature_russian_desc: "The entire interface is translated into Russian — no English insertions or unfinished menus.",
    feature_brand_title: "Custom branding",
    feature_brand_desc: "Unique logo, colour palette and name — Chator isn't just a clone.",
    feature_calls_title: "Group calls",
    feature_calls_desc: "Voice and video calls via Jitsi Meet — no limits on the number of participants.",
    feature_enterprise_title: "Enterprise module",
    feature_enterprise_desc: "Extra features for corporate use — SSO, OIDC and more.",
    feature_crypto_title: "Encryption",
    feature_crypto_desc: "End-to-end encryption by default on the Matrix protocol — nobody can read your messages.",
    feature_decentral_title: "Decentralisation",
    feature_decentral_desc: "Runs on any Matrix server. No single point of failure — and nobody can block it.",
    about_title: "Technology",
    about_matrix_title: "Matrix",
    about_matrix_desc: "An open standard for decentralised communication. Servers talk to each other — you're not tied to a single provider. The protocol is used by governments, militaries and hospitals worldwide.",
    about_matrix_link: "Learn more about Matrix",
    about_tech_title: "Tech Stack",
    about_tech_rust: "matrix-rust-sdk",
    about_tech_rust_desc: "— Matrix protocol core in Rust",
    about_tech_compose: "Jetpack Compose",
    about_tech_compose_desc: "— modern Kotlin-based UI",
    about_tech_jitsi: "Jitsi Meet",
    about_tech_jitsi_desc: "— open video conferencing",
    about_tech_sso: "OIDC / SSO",
    about_tech_sso_desc: "— corporate authentication",
    screenshots_title: "Screenshots",
    screenshots_subtitle: "Screenshots coming soon",
    screenshot_placeholder: "Screenshot",
    install_title: "Installation",
    install_apk_title: "⬇️ Download APK",
    install_apk_desc: "Latest stable release available on GitHub Releases.",
    install_apk_btn: "View releases",
    install_build_title: "🛠️ Build from source",
    install_build_desc: "Clone the repository and open the project in Android Studio.",
    install_build_btn: "Details",
    install_reqs_title: "Requirements",
    install_req_1: "<strong>Android 7.0</strong> (API 24) and above — standard version",
    install_req_2: "<strong>Android 13</strong> (API 33) and above — Enterprise version",
    contribute_title: "Contribute",
    contribute_step_1: "Pick an issue from <a href=\"https://github.com/mamalubitlal/element-x-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22\" target=\"_blank\" rel=\"noopener\">good first issues</a>",
    contribute_step_2: "Leave a comment saying you're working on it",
    contribute_step_3: "Read the <a href=\"https://github.com/mamalubitlal/element-x-android/blob/develop/CONTRIBUTING.md\" target=\"_blank\" rel=\"noopener\">contribution guide</a>",
    contribute_btn: "View issues",
    footer_name: "Chator",
    footer_legal: "License: AGPL-3.0",
    footer_copy: "© 2025 Element Creations Ltd. / 2022–2025 New Vector Ltd.",
  },
};

let currentLang = localStorage.getItem("chator_lang") || "ru";

function translate(lang) {
  currentLang = lang;
  localStorage.setItem("chator_lang", lang);
  document.documentElement.lang = lang;

  const t = translations[lang];
  if (!t) return;

  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const key = el.getAttribute("data-i18n");
    const val = t[key];
    if (val === undefined) return;
    // Values containing "<" are HTML fragments — use innerHTML
    if (val.includes("<")) {
      el.innerHTML = val;
    } else {
      el.textContent = val;
    }
  });

  // Update lang switch button display
  const cur = document.querySelector(".lang-switch__current");
  const oth = document.querySelector(".lang-switch__other");
  if (cur) cur.textContent = lang.toUpperCase();
  if (oth) oth.textContent = lang === "ru" ? "EN" : "RU";
}

document.getElementById("langSwitch").addEventListener("click", () => {
  translate(currentLang === "ru" ? "en" : "ru");
});

// ─── Mobile Nav Toggle ────────────────────────────────────────

document.getElementById("navToggle").addEventListener("click", () => {
  document.getElementById("navLinks").classList.toggle("open");
});

document.querySelectorAll("#navLinks a").forEach((a) => {
  a.addEventListener("click", () => {
    document.getElementById("navLinks").classList.remove("open");
  });
});

// ─── Scroll Reveal ────────────────────────────────────────────

const revealObserver = new IntersectionObserver(
  (entries) => {
    entries.forEach((e) => {
      if (e.isIntersecting) {
        e.target.classList.add("visible");
        revealObserver.unobserve(e.target);
      }
    });
  },
  { threshold: 0.15 }
);

document
  .querySelectorAll(
    ".feature-card, .about__card, .install__card, .contribute__step, .screenshot-placeholder"
  )
  .forEach((el) => {
    el.classList.add("reveal");
    revealObserver.observe(el);
  });

// ─── Init ─────────────────────────────────────────────────────

translate(currentLang);
