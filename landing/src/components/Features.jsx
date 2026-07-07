import { useEffect, useRef } from 'react'
import { useLanguage } from '../i18n'
import FeatureCard3D from './FeatureCard3D'
import { FiLock, FiGlobe, FiPhone, FiMessageCircle } from 'react-icons/fi'

const icons = [FiLock, FiGlobe, FiPhone, FiMessageCircle]

export default function Features() {
  const ref = useRef(null)
  const { t } = useLanguage()

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
    <section id="features" className="feat">
      <div className="feat__in">
        <h2 className="sec-title" ref={ref}>{t.features.title}</h2>
        <div className="feat__grid">
          {t.features.items.map((item, i) => (
            <FeatureCard3D key={item.title} icon={icons[i]} title={item.title} desc={item.desc} i={i} />
          ))}
        </div>
      </div>
    </section>
  )
}
