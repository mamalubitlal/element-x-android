import { useRef, useMemo, useCallback } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import * as THREE from 'three'

function BurstParticles({ trigger, count = 40 }) {
  const ref = useRef()
  const posRef = useRef()
  const velRef = useRef()
  const lifeRef = useRef()
  const activeRef = useRef(false)
  const timeRef = useRef(0)

  const initialPos = useMemo(() => new Float32Array(count * 3), [count])
  const initialCol = useMemo(() => {
    const c = new Float32Array(count * 3)
    for (let i = 0; i < count; i++) {
      c[i * 3] = 0.22; c[i * 3 + 1] = 0.61; c[i * 3 + 2] = 1
    }
    return c
  }, [count])

  const burst = useCallback(() => {
    if (!ref.current) return
    const positions = ref.current.geometry.attributes.position.array
    const vel = new Float32Array(count * 3)
    for (let i = 0; i < count; i++) {
      const theta = Math.random() * Math.PI * 2
      const phi = Math.random() * Math.PI
      const speed = 2 + Math.random() * 4
      positions[i * 3] = 0
      positions[i * 3 + 1] = 0
      positions[i * 3 + 2] = 0
      vel[i * 3] = Math.sin(phi) * Math.cos(theta) * speed
      vel[i * 3 + 1] = Math.sin(phi) * Math.sin(theta) * speed
      vel[i * 3 + 2] = Math.cos(phi) * speed
    }
    velRef.current = vel
    ref.current.geometry.attributes.position.needsUpdate = true
    lifeRef.current = new Float32Array(count).fill(1)
    activeRef.current = true
    timeRef.current = 0
  }, [count])

  // Expose burst via ref
  posRef.current = burst

  useFrame((_, delta) => {
    if (!activeRef.current || !ref.current) return
    timeRef.current += delta
    if (timeRef.current > 2) {
      activeRef.current = false
      return
    }
    const pos = ref.current.geometry.attributes.position.array
    const vel = velRef.current
    for (let i = 0; i < count; i++) {
      pos[i * 3] += vel[i * 3] * delta
      pos[i * 3 + 1] += vel[i * 3 + 1] * delta
      pos[i * 3 + 2] += vel[i * 3 + 2] * delta
      vel[i * 3] *= 0.97
      vel[i * 3 + 1] *= 0.97
      vel[i * 3 + 2] *= 0.97
    }
    ref.current.geometry.attributes.position.needsUpdate = true
    ref.current.material.opacity = Math.max(0, 1 - timeRef.current / 2)
  })

  return (
    <points ref={ref}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          count={count}
          array={initialPos}
          itemSize={3}
        />
        <bufferAttribute
          attach="attributes-color"
          count={count}
          array={initialCol}
          itemSize={3}
        />
      </bufferGeometry>
      <pointsMaterial
        size={0.15}
        transparent
        opacity={0}
        blending={THREE.AdditiveBlending}
        depthWrite={false}
        vertexColors
      />
    </points>
  )
}

function FloatingStars({ count = 30 }) {
  const ref = useRef()
  const pos = useMemo(() => {
    const p = new Float32Array(count * 3)
    for (let i = 0; i < count; i++) {
      p[i * 3] = (Math.random() - 0.5) * 20
      p[i * 3 + 1] = (Math.random() - 0.5) * 10
      p[i * 3 + 2] = (Math.random() - 0.5) * 5
    }
    return p
  }, [count])

  useFrame((state) => {
    if (ref.current) {
      ref.current.rotation.y = Math.sin(state.clock.elapsedTime * 0.05) * 0.1
    }
  })

  return (
    <points ref={ref}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          count={count}
          array={pos}
          itemSize={3}
        />
      </bufferGeometry>
      <pointsMaterial
        size={0.04}
        color="#389CFF"
        transparent
        opacity={0.3}
        blending={THREE.AdditiveBlending}
        depthWrite={false}
      />
    </points>
  )
}

export { BurstParticles, FloatingStars }
