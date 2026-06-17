import { useRef, useState, useMemo } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import * as THREE from 'three'

function Logo3D({ scrollY, small }) {
  const ref = useRef()
  const meshRef = useRef()
  const [hovered, setHovered] = useState(false)
  const size = small ? 0.5 : 1.2

  const texture = useMemo(() => {
    const c = document.createElement('canvas')
    c.width = 256
    c.height = 256
    const ctx = c.getContext('2d')

    ctx.clearRect(0, 0, 256, 256)

    // Outer glow
    ctx.shadowColor = '#389CFF'
    ctx.shadowBlur = 40

    // Ч letter — large enough to fill, crisp
    ctx.fillStyle = '#389CFF'
    ctx.font = 'bold 180px Inter, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('Ч', 128, 136)

    // Inner highlight
    ctx.shadowBlur = 0
    ctx.fillStyle = '#6BB3FF'
    ctx.fillText('Ч', 128, 136)

    const tex = new THREE.CanvasTexture(c)
    tex.minFilter = THREE.LinearMipmapLinearFilter
    tex.magFilter = THREE.LinearFilter
    tex.anisotropy = 4
    return tex
  }, [])

  useFrame((state) => {
    if (!ref.current) return
    const t = state.clock.elapsedTime
    ref.current.rotation.y = Math.sin(t * 0.35) * 0.3
    ref.current.rotation.x = (scrollY.current * 0.004) % 0.2

    const s = 1 + (hovered ? 0.08 : 0) + Math.sin(t * 0.5) * 0.02
    ref.current.scale.set(s, s, s)

    if (meshRef.current) {
      const glow = 0.2 + (hovered ? 0.4 : 0) + Math.sin(t * 1.5) * 0.05
      meshRef.current.material.emissiveIntensity = glow
    }
  })

  return (
    <group
      ref={ref}
      onPointerOver={() => setHovered(true)}
      onPointerOut={() => setHovered(false)}
    >
      {/* Glow ring behind */}
      <mesh position={[0, 0, -0.04]}>
        <planeGeometry args={[size * 1.4, size * 1.4]} />
        <meshBasicMaterial color="#389CFF" transparent opacity={0.06} />
      </mesh>

      {/* Ч fills the entire hitbox — no scaling down */}
      <mesh ref={meshRef} position={[0, 0, 0]}>
        <planeGeometry args={[size, size]} />
        <meshStandardMaterial
          map={texture}
          transparent
          emissive="#389CFF"
          emissiveIntensity={0.2}
          emissiveMap={texture}
        />
      </mesh>
    </group>
  )
}

export default function ChLogo3D({ scrollY, small = false }) {
  return (
    <div style={{ width: small ? 44 : 100, height: small ? 44 : 100 }}>
      <Canvas
        camera={{ position: [0, 0, small ? 1.8 : 3], fov: 40 }}
        dpr={[1, 2]}
        gl={{ alpha: true, antialias: true }}
      >
        <ambientLight intensity={0.5} />
        <pointLight position={[5, 5, 5]} intensity={0.6} />
        <pointLight position={[-3, -2, 3]} intensity={0.3} color="#6BB3FF" />
        <Logo3D scrollY={scrollY} small={small} />
      </Canvas>
    </div>
  )
}
