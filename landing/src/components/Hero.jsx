import { useRef, useEffect } from 'react'
import { useLanguage } from '../i18n'
import HeroScene3D from './HeroScene3D'

export default function Hero() {
  const { t } = useLanguage()
  const mouse = useRef({ x: 0.5, y: 0.5 })

  useEffect(() => {
    const onMove = (e) => {
      mouse.current.x = e.clientX / window.innerWidth
      mouse.current.y = e.clientY / window.innerHeight
    }
    window.addEventListener('mousemove', onMove, { passive: true })
    return () => window.removeEventListener('mousemove', onMove)
  }, [])

  return (
    <section id="hero" className="hero">
      <div className="hero__bg" aria-hidden="true" />
      <div className="hero__in">
        <div className="hero__content">
          <div className="hero__badge">{t.hero.badge}</div>
          <h1 className="hero__title">{t.hero.headline}</h1>
          <p className="hero__sub">{t.hero.subhead}</p>
          <div className="hero__actions">
            <a
              href="https://github.com/chator-im/element-x-android/releases"
              className="btn btn--primary"
              target="_blank"
              rel="noopener noreferrer"
            >
              {t.hero.cta}
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M8 1v10m0 0L4 7m4 4l4-4M2 13h12" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
              </svg>
            </a>
            <a
              href="https://chator.space/web"
              className="btn btn--ghost"
              target="_blank"
              rel="noopener noreferrer"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <rect x="2" y="3" width="12" height="10" rx="1.5" stroke="currentColor" strokeWidth="1.5" fill="none"/>
                <circle cx="8" cy="9" r="1.5" fill="currentColor"/>
              </svg>
              {t.hero.web}
            </a>
          </div>
        </div>

        <div className="hero__mockup" aria-hidden="true">
          <HeroScene3D mouse={mouse} />
        </div>
      </div>
    </section>
  )
}
