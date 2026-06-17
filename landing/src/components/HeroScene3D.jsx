import { useRef, useMemo } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import { RoundedBox, Float } from '@react-three/drei'
import * as THREE from 'three'

// Chator/Element X dark theme colors (from app source)
const C = {
  canvasBg: '#101317',      // colorThemeBg
  statusBar: '#0C0E11',     // slightly darker than bg
  appBar: '#15181D',        // between bg and gray300
  white: '#EBEEF2',         // colorGray1400 (textPrimary)
  gray: '#808994',          // colorGray900 (textSecondary)
  grayDim: '#4A4F55',       // colorGray700 (iconQuaternary)
  msgMine: '#323539',       // colorGray500 (messageFromMeBackground)
  msgOther: '#26282D',      // colorGray400 (messageFromOtherBackground)
  accent: '#4187EB',        // colorBlue900 (bgAccentRest, send button)
  accentGlow: '#4187EB',    // for glow effect
  composer: '#1D1F24',      // colorGray300 (bgSubtleSecondary)
  borderIn: '#323539',      // colorGray500 (borderDisabled)
  encrypted: '#4187EB',     // accent blue for shield/encryption
}

const FONT_REGULAR = '14px system-ui, -apple-system, sans-serif'
const FONT_SMALL = '11px system-ui, -apple-system, sans-serif'
const FONT_BOLD = '600 15px system-ui, -apple-system, sans-serif'
const FONT_STATUS = '13px system-ui, -apple-system, sans-serif'

function Phone({ mouse }) {
  const ref = useRef()
  const canvasRef = useRef()


  // Build message data once
  const messages = useMemo(() => [
    { text: 'Привет! Давно не виделись 👋', from: 'other', ts: '12:28' },
    { text: 'Привет! Согласен, как сам?', from: 'me', ts: '12:28' },
    { text: 'Отлично! Слушай, есть идея...', from: 'other', ts: '12:29' },
    { text: 'Давай, рассказывай 🔥', from: 'me', ts: '12:29' },
    { text: 'Новый проект хочу запустить\nна Matrix, нужна команда', from: 'other', ts: '12:30' },
    { text: 'Я в деле! Что за проект?', from: 'me', ts: '12:31' },
    { text: 'Мессенджер с открытым\nисходным кодом', from: 'other', ts: '12:31' },
    { text: 'Ого, серьёзно? Круто!', from: 'me', ts: '12:32' },
  ], [])

  const texture = useMemo(() => {
    const c = document.createElement('canvas')
    c.width = 460
    c.height = 940
    canvasRef.current = c
    // Pre-fill with app background so first frame isn't blank
    const ctx = c.getContext('2d')
    ctx.fillStyle = '#101317'
    ctx.fillRect(0, 0, 460, 940)
    ctx.fillStyle = '#EBEEF2'
    ctx.font = '16px system-ui'
    ctx.textAlign = 'center'
    ctx.fillText('Загрузка...', 230, 470)
    ctx.textAlign = 'left'
    const tex = new THREE.CanvasTexture(c)
    tex.minFilter = THREE.LinearFilter
    tex.magFilter = THREE.LinearFilter
    return tex
  }, [])

  function drawStatusBar(ctx, w) {
    // Status bar background
    ctx.fillStyle = C.statusBar
    ctx.fillRect(0, 0, w, 36)

    // Time
    ctx.fillStyle = C.white
    ctx.font = FONT_STATUS
    ctx.textAlign = 'center'
    ctx.fillText('12:30', w / 2, 24)
    ctx.textAlign = 'left'

    // Right icons (battery, signal dots)
    ctx.fillStyle = C.white
    // Signal dots
    const sx = w - 78
    for (let i = 0; i < 4; i++) {
      ctx.fillRect(sx + i * 5, 18 - i * 2, 3, 6 + i * 2)
    }
    // Battery outline
    const bx = w - 46, by = 14
    ctx.strokeStyle = C.white
    ctx.lineWidth = 1.5
    ctx.strokeRect(bx, by, 22, 9)
    ctx.fillRect(bx + 22, by + 2, 2, 5)
    // Battery fill
    ctx.fillStyle = C.white
    ctx.fillRect(bx + 2, by + 2, 14, 5)
  }

  function drawAppBar(ctx, w) {
    // Bar background
    ctx.fillStyle = C.appBar
    ctx.fillRect(0, 36, w, 40)

    // Back arrow
    ctx.fillStyle = C.white
    ctx.font = '18px system-ui'
    ctx.textAlign = 'center'
    ctx.fillText('‹', 20, 61)
    ctx.textAlign = 'left'

    // Room name
    ctx.fillStyle = C.white
    ctx.font = FONT_BOLD
    ctx.fillText('Общий чат', 38, 62)

    // Unread badge
    ctx.fillStyle = C.accent
    ctx.beginPath()
    ctx.arc(w - 48, 52, 4, 0, Math.PI * 2)
    ctx.fill()

    // Three dots
    ctx.fillStyle = C.gray
    ctx.font = '18px system-ui'
    ctx.fillText('···', w - 30, 60)
  }

  function drawBubble(ctx, msg, x, y, w, isMine, radius) {
    const r = typeof radius === 'number'
      ? { tl: radius, tr: radius, bl: radius, br: radius }
      : radius
    ctx.beginPath()
    ctx.moveTo(x + r.tl, y)
    ctx.lineTo(x + w - r.tr, y)
    ctx.quadraticCurveTo(x + w, y, x + w, y + r.tr)
    ctx.lineTo(x + w, y + msg.h - r.br)
    ctx.quadraticCurveTo(x + w, y + msg.h, x + w - r.br, y + msg.h)
    ctx.lineTo(x + r.bl, y + msg.h)
    ctx.quadraticCurveTo(x, y + msg.h, x, y + msg.h - r.bl)
    ctx.lineTo(x, y + r.tl)
    ctx.quadraticCurveTo(x, y, x + r.tl, y)
    ctx.closePath()
    ctx.fill()
  }

  function drawMessage(ctx, msg, i, animAlpha) {
    const isMine = msg.from === 'me'
    const padX = 16
    const maxBubbleW = (460 - padX * 2) * 0.78
    const innerPadX = 14
    const innerPadY = 9

    // Measure text lines
    ctx.font = FONT_REGULAR
    const lines = msg.text.split('\n')
    let maxLineW = 0
    for (const line of lines) {
      const lw = ctx.measureText(line).width
      if (lw > maxLineW) maxLineW = lw
    }
    const bubbleW = Math.max(Math.min(maxLineW + innerPadX * 2, maxBubbleW), 100)
    const lineH = 20
    const textH = lines.length * lineH
    const bubbleH = textH + innerPadY * 2

    msg.w = bubbleW
    msg.h = bubbleH

    // Position
    const bx = isMine ? 460 - padX - bubbleW : padX
    const by = msg.y

    // Bubble shape (matching app: RoundedCornerShape 12dp)
    // Outgoing: tl=12, tr=12, bl=12, br=4 (tail on bottom-right)
    // Incoming: tl=12, tr=12, bl=4, br=12 (tail on bottom-left)
    const tailR = isMine ? 4 : 4
    const rad = { tl: 12, tr: 12, bl: isMine ? 12 : tailR, br: isMine ? tailR : 12 }

    // Background
    ctx.globalAlpha = animAlpha
    ctx.fillStyle = isMine ? C.msgMine : C.msgOther
    drawBubble(ctx, msg, bx, by, bubbleW, isMine, rad)

    // Encrypted badge (small shield icon on incoming messages)
    if (!isMine) {
      ctx.fillStyle = C.encrypted
      ctx.globalAlpha = animAlpha * 0.5
      ctx.font = '9px system-ui'
      ctx.fillText('•', bx + 8, by + bubbleH - 4)
      ctx.globalAlpha = animAlpha
    }

    // Read status on outgoing
    if (isMine) {
      ctx.fillStyle = C.accent
      ctx.globalAlpha = animAlpha * 0.6
      ctx.font = '10px system-ui'
      ctx.textAlign = 'right'
      ctx.fillText('✓✓', bx + bubbleW - innerPadX, by + bubbleH - 3)
      ctx.textAlign = 'left'
      ctx.globalAlpha = animAlpha
    }

    // Text
    ctx.fillStyle = C.white
    ctx.font = FONT_REGULAR
    for (let li = 0; li < lines.length; li++) {
      ctx.fillText(lines[li], bx + innerPadX, by + innerPadY + 14 + li * lineH)
    }

    // Timestamp
    ctx.fillStyle = C.gray
    ctx.globalAlpha = animAlpha * 0.7
    ctx.font = FONT_SMALL
    const tsX = isMine ? bx + bubbleW - innerPadX : bx + innerPadX
    const tsY = by + bubbleH + 13
    ctx.textAlign = isMine ? 'right' : 'left'
    ctx.fillText(msg.ts, tsX, tsY)
    ctx.textAlign = 'left'
    ctx.globalAlpha = 1
  }

  function drawComposer(ctx, w) {
    const cy = 864
    // Composer background bar
    ctx.fillStyle = C.composer
    ctx.fillRect(0, cy, w, 76)

    // Separator line
    ctx.fillStyle = C.borderIn
    ctx.fillRect(0, cy, w, 0.5)

    // Attachment button
    const attachCX = 30, attachCY = cy + 38
    ctx.fillStyle = C.gray
    ctx.beginPath()
    ctx.arc(attachCX, attachCY, 14, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = C.canvasBg
    ctx.font = '18px system-ui'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('+', attachCX, attachCY)
    ctx.textBaseline = 'alphabetic'

    // Input box
    const inputX = 56, inputY = cy + 16, inputW = w - 56 - 56, inputH = 44
    ctx.fillStyle = C.canvasBg
    roundRectCanvas(ctx, inputX, inputY, inputW, inputH, 12)
    ctx.fill()
    ctx.strokeStyle = C.borderIn
    ctx.lineWidth = 1
    roundRectCanvas(ctx, inputX, inputY, inputW, inputH, 12)
    ctx.stroke()

    // Placeholder text
    ctx.fillStyle = C.grayDim
    ctx.font = FONT_REGULAR
    ctx.textAlign = 'left'
    ctx.fillText('Сообщение...', inputX + 14, inputY + 28)

    // Send button (matches app: 36dp circle, bgAccentRest, white arrow)
    const btnCX = w - 28, btnCY = cy + 38
    ctx.fillStyle = C.accent
    ctx.beginPath()
    ctx.arc(btnCX, btnCY, 18, 0, Math.PI * 2)
    ctx.fill()

    // Send arrow (↑)
    ctx.fillStyle = C.canvasBg
    ctx.font = '16px system-ui'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('↑', btnCX + 1, btnCY + 0.5)
    ctx.textBaseline = 'alphabetic'
    ctx.textAlign = 'left'
  }

  useFrame((state) => {
    if (!ref.current) return
    const t = state.clock.elapsedTime

    if (mouse.current) {
      const mx = (mouse.current.x - 0.5) * 2
      const my = (mouse.current.y - 0.5) * 2
      ref.current.rotation.y = mx * 0.06
      ref.current.rotation.x = my * -0.03
    }

    // Draw screen
    const ctx = canvasRef.current?.getContext('2d')
    if (!ctx) return
    const w = canvasRef.current.width
    const h = canvasRef.current.height

    // Clear
    ctx.clearRect(0, 0, w, h)

    // Canvas background
    ctx.fillStyle = C.canvasBg
    ctx.fillRect(0, 0, w, h)

    // UI layers
    drawStatusBar(ctx, w)
    drawAppBar(ctx, w)

    // Messages — positions computed from bottom (newest)
    // Each message with spacing: bubbleH + 8 (gap between) + 16 (timestamp gap)
    const gap = 6
    let cursorY = 856
    for (let i = messages.length - 1; i >= 0; i--) {
      const msg = messages[i]
      const isMine = msg.from === 'me'
      // Measure text for this message to get height
      ctx.font = FONT_REGULAR
      const lines = msg.text.split('\n')
      let maxLineW = 0
      for (const line of lines) {
        const lw = ctx.measureText(line).width
        if (lw > maxLineW) maxLineW = lw
      }
      const maxBubbleW = (460 - 32) * 0.78
      const bubbleW = Math.max(Math.min(maxLineW + 28, maxBubbleW), 100)
      const lineH = 20
      const textH = lines.length * lineH
      const bubbleH = textH + 18
      const msgTotalH = bubbleH + 16 // bubble + timestamp
      cursorY -= msgTotalH + gap
      msg.y = cursorY
      msg.w = bubbleW
      msg.h = bubbleH
    }

    // Draw messages with animation
    const visible = Math.min(messages.length, Math.floor(t / 0.7) + 1)
    for (let i = 0; i < visible; i++) {
      const alpha = Math.min(1, Math.max(0, (t - i * 0.7) * 3))
      drawMessage(ctx, messages[i], i, alpha)
    }

    // Composer
    drawComposer(ctx, w)

    texture.needsUpdate = true
  })

  return (
    <Float speed={1} rotationIntensity={0.02} floatIntensity={0.5}>
      <group ref={ref}>
      {/* Outer rim — visible edge */}
      <RoundedBox args={[2.7, 5.2, 0.22]} radius={0.4} smoothness={8}>
        <meshStandardMaterial color="#8A8E92" />
      </RoundedBox>

      {/* Body — light gray, visible against dark bg */}
      <RoundedBox args={[2.6, 5.1, 0.18]} radius={0.38} smoothness={8}>
        <meshStandardMaterial color="#6A6E72" />
      </RoundedBox>

      {/* Screen — PlaneGeometry (canvas draws rounded corners) */}
      <mesh position={[0, 0, 0.2]}>
        <planeGeometry args={[2.3, 4.7]} />
        <meshBasicMaterial map={texture} />
      </mesh>

      {/* Glass overlay */}
      <mesh position={[0, 0, 0.202]}>
        <planeGeometry args={[2.3, 4.7]} />
        <meshStandardMaterial
          color="white"
          transparent
          opacity={0.04}
        />
      </mesh>
    </group>
    </Float>
  )
}

function roundRectCanvas(ctx, x, y, w, h, r) {
  if (typeof r === 'number') r = { tl: r, tr: r, bl: r, br: r }
  ctx.beginPath()
  ctx.moveTo(x + r.tl, y)
  ctx.lineTo(x + w - r.tr, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r.tr)
  ctx.lineTo(x + w, y + h - r.br)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r.br, y + h)
  ctx.lineTo(x + r.bl, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r.bl)
  ctx.lineTo(x, y + r.tl)
  ctx.quadraticCurveTo(x, y, x + r.tl, y)
  ctx.closePath()
}

export default function HeroScene3D({ mouse }) {
  return (
    <div className="hero-3d">
      <Canvas
        camera={{ position: [0, 0, 12], fov: 30 }}
        dpr={[1, 1.5]}
        gl={{ alpha: true, antialias: true }}
      >
        <ambientLight intensity={1.0} />
        <hemisphereLight args={["#ffffff", "#6BB3FF", 0.6]} />
        <directionalLight position={[5, 8, 5]} intensity={1.2} />
        <directionalLight position={[-5, -3, 4]} intensity={0.6} color="#6BB3FF" />
        <directionalLight position={[0, -6, 6]} intensity={0.4} color="#389CFF" />
        <Phone mouse={mouse} />
      </Canvas>
    </div>
  )
}
