import { useState, useEffect, useRef } from 'react'
import { useLanguage } from '../i18n'
import ChLogo3D from './ChLogo3D'

export default function Nav({ scrollY }) {
  const { t, lang, setLang } = useLanguage()
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 60)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const scrollTo = (id) => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <nav className={`nav${scrolled ? ' nav--scrolled' : ''}`}>
      <div className="nav__in">
        <button className="nav__logo" onClick={() => scrollTo('hero')}>
          <ChLogo3D scrollY={scrollY} small />
        </button>

        <div className="nav__links">
          <button onClick={() => scrollTo('features')}>{t.nav.features}</button>
          <button onClick={() => scrollTo('about')}>{t.nav.about}</button>
          <button onClick={() => scrollTo('download')}>{t.nav.download}</button>
        </div>

        <button
          className="nav__lang"
          onClick={() => setLang(lang === 'ru' ? 'en' : 'ru')}
        >
          <span className={lang === 'ru' ? 'active' : ''}>RU</span>
          <span className="sep">/</span>
          <span className={lang === 'en' ? 'active' : ''}>EN</span>
        </button>
      </div>
    </nav>
  )
}
