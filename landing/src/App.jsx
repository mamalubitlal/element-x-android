import { useRef, useEffect } from 'react'
import Nav from './components/Nav'
import Hero from './components/Hero'
import Features from './components/Features'
import About from './components/About'
import Download from './components/Download'
import Team from './components/Team'
import Footer from './components/Footer'
import MatrixGrid from './components/MatrixGrid'
import './App.css'

export default function App() {
  const scrollY = useRef(0)

  useEffect(() => {
    const onScroll = () => { scrollY.current = window.scrollY }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <>
      <MatrixGrid scrollY={scrollY} />
      <Nav scrollY={scrollY} />
      <main>
        <Hero />
        <Features />
        <About />
        <Download />
        <Team scrollY={scrollY} />
      </main>
      <Footer />
    </>
  )
}
