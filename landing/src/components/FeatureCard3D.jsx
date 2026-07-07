import { useRef, useEffect, useState } from 'react'

export default function FeatureCard3D({ icon: Icon, title, desc, i }) {
  const ref = useRef(null)
  const [style, setStyle] = useState({})
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const io = new IntersectionObserver(
      ([e]) => { if (e.isIntersecting) { setVisible(true); io.unobserve(e.target) } },
      { threshold: 0.15 }
    )
    io.observe(el)
    return () => io.disconnect()
  }, [])

  const handleMouseMove = (e) => {
    const el = ref.current
    if (!el) return
    const rect = el.getBoundingClientRect()
    const x = (e.clientX - rect.left) / rect.width
    const y = (e.clientY - rect.top) / rect.height
    const tiltX = (y - 0.5) * 12
    const tiltY = (x - 0.5) * -12
    setStyle({
      transform: `perspective(800px) rotateX(${tiltX}deg) rotateY(${tiltY}deg) translateZ(10px)`,
      boxShadow: `0 12px 40px rgba(56,156,255,${0.06 + Math.abs(x - 0.5) * 0.08 + Math.abs(y - 0.5) * 0.08})`,
      borderColor: '#389CFF',
    })
  }

  const handleMouseLeave = () => {
    setStyle({
      transform: 'perspective(800px) rotateX(0deg) rotateY(0deg) translateZ(0px)',
      boxShadow: 'none',
      borderColor: '#323539',
      transition: 'transform 0.5s ease, box-shadow 0.5s ease, border-color 0.5s ease',
    })
    setTimeout(() => setStyle({ transition: '' }), 500)
  }

  return (
    <article
      ref={ref}
      className={`feat-card ${visible ? 'vis' : ''}`}
      style={{
        ...style,
        transitionDelay: `${i * 80}ms`,
        transition: visible ? 'opacity 0.45s ease, transform 0.45s ease' : '',
      }}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
    >
      <span className="feat-card__icon">
        {Icon && <Icon size={24} />}
      </span>
      <h3 className="feat-card__title">{title}</h3>
      <p className="feat-card__desc">{desc}</p>
    </article>
  )
}
