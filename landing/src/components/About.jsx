import { useEffect, useRef } from 'react'
import { useLanguage } from '../i18n'

function Sect({ children }) {
  const ref = useRef(null)
  useEffect(() => {
    const el = ref.current
    if (!el) return
    const io = new IntersectionObserver(
      ([e]) => { if (e.isIntersecting) { e.target.classList.add('vis'); io.unobserve(e.target) } },
      { threshold: 0.15 }
    )
    io.observe(el)
    return () => io.disconnect()
  }, [])
  return <div ref={ref} className="reveal">{children}</div>
}

export default function About() {
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
    <section id="about" className="about">
      <div className="about__in">
        <h2 className="sec-title" ref={ref}>{t.about.title}</h2>
        <div className="about__grid">
          <Sect>
            <h3 className="about__sub">{t.about.matrix}</h3>
            <p className="about__text">{t.about.matrixDesc}</p>
          </Sect>
          <Sect>
            <h3 className="about__sub">{t.about.tech}</h3>
            <p className="about__text">{t.about.techDesc}</p>
            <div className="about__tags">
              <span className="tag">Rust SDK</span>
              <span className="tag">Jetpack Compose</span>
              <span className="tag">Kotlin</span>
            </div>
          </Sect>
        </div>
      </div>
    </section>
  )
}
