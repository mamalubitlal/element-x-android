import { useEffect, useRef } from 'react'
import { useLanguage } from '../i18n'

export default function Download() {
  const { t } = useLanguage()
  const ref = useRef(null)
  useEffect(() => {
    const el = ref.current
    if (!el) return
    const io = new IntersectionObserver(
      ([e]) => { if (e.isIntersecting) { e.target.classList.add('vis'); io.unobserve(e.target) } },
      { threshold: 0.1 }
    )
    io.observe(el)
    return () => io.disconnect()
  }, [])

  return (
    <section id="download" className="down">
      <div className="down__in">
        <h2 className="sec-title" ref={ref}>{t.download.title}</h2>
        <div className="down__cards">
          <a
            href="https://github.com/chator-im/element-x-android/releases"
            target="_blank"
            rel="noopener noreferrer"
            className="down__card down__card--primary"
          >
            <div className="down__card-top">
              <span className="down__card-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M12 3v13m0 0l-5-5m5 5l5-5M4 19h16"/></svg>
              </span>
              <div>
                <h3 className="down__card-title">{t.download.apk}</h3>
                <p className="down__card-desc">{t.download.apkDesc}</p>
              </div>
            </div>
            <span className="down__arrow">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M7 3l7 7-7 7" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/></svg>
            </span>
          </a>
        </div>
      </div>
    </section>
  )
}
