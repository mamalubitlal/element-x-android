import { useRef, useMemo } from 'react'
import { Canvas, useFrame, useThree } from '@react-three/fiber'

function GridLines({ scrollY }) {
  const ref = useRef()
  const { viewport } = useThree()

  const positions = useMemo(() => {
    const pts = []
    const w = viewport.width * 2
    const h = viewport.height * 2
    const step = 1.2
    for (let x = -w; x <= w; x += step) {
      pts.push(x, -h, 0, x, h, 0)
    }
    for (let y = -h; y <= h; y += step) {
      pts.push(-w, y, 0, w, y, 0)
    }
    return new Float32Array(pts)
  }, [viewport])

  useFrame((state) => {
    if (ref.current) {
      ref.current.position.y = (scrollY.current * 0.3) % 2
      ref.current.material.opacity = 0.06 + 0.04 * Math.sin(state.clock.elapsedTime * 0.3)
    }
  })

  return (
    <lineSegments ref={ref}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          count={positions.length / 3}
          array={positions}
          itemSize={3}
        />
      </bufferGeometry>
      <lineBasicMaterial color="#389CFF" transparent opacity={0.08} />
    </lineSegments>
  )
}

export default function MatrixGrid({ scrollY }) {
  return (
    <div className="matrix-grid">
      <Canvas camera={{ position: [0, 0, 5], fov: 60 }} dpr={[1, 1.5]} gl={{ antialias: false, alpha: true }}>
        <GridLines scrollY={scrollY} />
      </Canvas>
    </div>
  )
}
