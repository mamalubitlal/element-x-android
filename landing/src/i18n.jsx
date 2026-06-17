import { createContext, useContext, useState, useEffect } from 'react'

export const translations = {
  ru: {
    nav: { features: 'Возможности', about: 'О проекте', download: 'Скачать' },
    hero: {
      headline: '«Чатор» — приватный мессенджер, которому можно доверять',
      subhead: 'Чатор — форк Element X на Matrix. Сквозное шифрование, русский интерфейс. Без компромиссов.',
      cta: 'Скачать APK',
      web: 'Веб-версия',
      badge: 'Для Android 8.0+',
    },
    features: {
      title: 'Что умеет Чатор',
      items: [
        { icon: '', title: 'Сквозное шифрование', desc: 'Сообщения видите только вы и собеседник. Ни сервер, ни провайдер не могут их прочесть' },
        { icon: '', title: 'Русский интерфейс', desc: 'Полная локализация: меню, уведомления, подсказки — всё на родном языке с первого запуска' },
        { icon: '', title: 'Аудио- и видеозвонки', desc: 'Защищённые вызовы через Matrix VoIP с end-to-end шифрованием' },
        { icon: '', title: 'Богатые сообщения', desc: 'Реакции, опросы, голосовые сообщения, редактирование, пересылка и спейсы' },
      ],
    },
    about: {
      title: 'О проекте',
      matrix: 'Надёжный протокол Matrix',
      matrixDesc: 'Matrix — это децентрализованный протокол для коммуникации. Вы выбираете сервер, контролируете данные и общаетесь с кем угодно, независимо от приложения. Чатор использует Matrix Rust SDK — ту же надёжную основу, что и Element.',
      tech: 'Стек технологий',
      techDesc: 'Rust SDK (шифрование), Jetpack Compose (UI), Kotlin (логика). Минимум зависимостей, максимум контроля.',
    },
    download: {
      title: 'Скачать',
      apk: 'APK для Android',
      apkDesc: 'Последняя стабильная версия. Установите вручную.',
    },
    team: {
      title: 'Разработчик',
      role: 'Создатель проекта',
      bio: 'Сделано одним разработчиком, которому не хватило русскоязычного, приватного и надёжного мессенджера на Android. Так появился Чатор.',
      link: 'Контакты',
    },
    footer: {
      caption: 'Сделано с целью.',
    },
  },
  en: {
    nav: { features: 'Features', about: 'About', download: 'Download' },
    hero: {
      headline: 'Chator — a private messenger you can trust',
      subhead: 'Chator is a secure messenger based on the Matrix protocol, featuring end-to-end encryption and a clean interface.',
      cta: 'Download APK',
      web: 'Web App',
      badge: 'For Android 8.0+',
    },
    features: {
      title: 'What Chator can do',
      items: [
        { icon: '', title: 'E2E Encryption', desc: 'Only you and the recipient can read your messages. Not even the server can decrypt them' },
        { icon: '', title: 'Russian UI', desc: 'Fully localized interface from the first launch' },
        { icon: '', title: 'Voice & Video Calls', desc: 'Secure calls via Matrix VoIP with end-to-end encryption' },
        { icon: '', title: 'Rich Messaging', desc: 'Reactions, polls, voice messages, edits, forwarding, and spaces' },
      ],
    },
    about: {
      title: 'About',
      matrix: 'The reliable Matrix protocol',
      matrixDesc: 'Matrix is a decentralized protocol for communication. You choose your server, control your data, and talk to anyone regardless of what app they use. Chator uses the Matrix Rust SDK — the same reliable foundation as Element.',
      tech: 'Tech stack',
      techDesc: 'Rust SDK (encryption), Jetpack Compose (UI), Kotlin (logic). Minimal dependencies, maximum control.',
    },
    download: {
      title: 'Download',
      apk: 'APK for Android',
      apkDesc: 'Latest stable release. Install manually.',
    },
    team: {
      title: 'Developer',
      role: 'Project creator',
      bio: 'Built by one developer who needed a private, reliable, Russian-language messenger on Android. The result was Chator.',
      link: 'Contact',
    },
    footer: {
      caption: 'Built with purpose.',
    },
  },
}

const LanguageContext = createContext()

export function LanguageProvider({ children }) {
  const [lang, setLang] = useState(() => {
    try { return localStorage.getItem('chator-lang') || 'ru' } catch { return 'ru' }
  })

  useEffect(() => {
    try { localStorage.setItem('chator-lang', lang) } catch {}
    document.documentElement.lang = lang
  }, [lang])

  return (
    <LanguageContext.Provider value={{ lang, setLang, t: translations[lang] }}>
      {children}
    </LanguageContext.Provider>
  )
}

export function useLanguage() {
  const ctx = useContext(LanguageContext)
  if (!ctx) throw new Error('useLanguage must be used within LanguageProvider')
  return ctx
}
