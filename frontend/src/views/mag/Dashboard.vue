<template>
  <div class="mag-screen" :class="{ 'mag-screen--pulse': pulseTick }">
    <canvas ref="canvasRef" class="mag-screen__canvas" aria-hidden="true" />
    <div class="mag-screen__grid" aria-hidden="true" />
    <div class="mag-screen__scan" aria-hidden="true" />

    <div class="mag-screen__content">
      <header class="mag-screen__header">
        <div class="mag-screen__title-block">
          <h1 class="mag-screen__title">MAG 运营大屏</h1>
          <p class="mag-screen__subtitle">多 Agent 协作 · 实时态势</p>
        </div>
        <div class="mag-screen__clock">{{ clockText }}</div>
      </header>

      <div class="mag-screen__toolbar">
        <div class="mag-screen__toolbar-inner">
          <label class="mag-screen__label">项目</label>
          <el-select
            v-model="projectId"
            placeholder="选择项目"
            filterable
            class="mag-screen__select"
            popper-class="mag-screen-select-popper"
            @change="onProjectChange"
          >
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-button class="mag-screen__btn" @click="load">刷新</el-button>
          <div class="mag-screen__ws" :class="{ 'mag-screen__ws--on': ws.connected }">
            <span class="mag-screen__ws-dot" />
            <span>{{ ws.connected ? '实时通道已连接' : '实时通道未连接' }}</span>
          </div>
        </div>
      </div>

      <section v-if="snapshot" class="mag-screen__metrics" :key="metricsKey">
        <article
          v-for="(item, i) in metricItems"
          :key="item.key"
          class="mag-tile"
          :style="{ '--d': `${i * 0.06}s` }"
        >
          <div class="mag-tile__corners" aria-hidden="true" />
          <div class="mag-tile__label">{{ item.label }}</div>
          <div class="mag-tile__value">
            <span class="mag-tile__num">{{ item.value }}</span>
          </div>
        </article>
      </section>

      <div v-else-if="projects.length === 0" class="mag-screen__empty">暂无项目，请先创建项目</div>
      <div v-else class="mag-screen__empty">请选择项目加载数据</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { magProjectsPage, magDashboardSnapshot } from '@/api/mag'
import { useMagWebSocket } from '@/composables/useMagWebSocket'

const projects = ref([])
const projectId = ref(null)
const snapshot = ref(null)
const ws = useMagWebSocket()
const canvasRef = ref(null)
const clockText = ref('')
const pulseTick = ref(false)
const metricsKey = ref(0)

let rafId = 0
let clockTimer = 0

const metricItems = computed(() => {
  const m = snapshot.value?.taskCountByState
  if (!m || typeof m !== 'object') return []
  return Object.entries(m).map(([label, value]) => ({
    key: label,
    label,
    value: Number(value) || 0,
  }))
})

function tickClock() {
  const d = new Date()
  clockText.value = d.toLocaleString('zh-CN', {
    hour12: false,
    weekday: 'short',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function flashPulse() {
  pulseTick.value = true
  requestAnimationFrame(() => {
    pulseTick.value = false
  })
}

async function loadProjects() {
  const res = await magProjectsPage({ pageNum: 1, pageSize: 100 })
  projects.value = res.data.list
  if (!projectId.value && projects.value.length) {
    projectId.value = projects.value[0].id
    load()
    connectWsForProject()
  }
}

async function load() {
  if (!projectId.value) return
  const res = await magDashboardSnapshot(projectId.value)
  snapshot.value = res.data
  metricsKey.value += 1
  flashPulse()
}

function connectWsForProject() {
  ws.disconnect()
  ws.connect({
    onOpen: () => {
      if (projectId.value) {
        ws.subscribeProject(projectId.value)
      }
    },
    onMessage: (data) => {
      if (
        data?.event === 'task.state.changed' &&
        Number(data?.projectId) === Number(projectId.value)
      ) {
        load()
      }
      if (data?.event === 'mag.alert.new' && Number(data?.projectId) === Number(projectId.value)) {
        load()
      }
    },
  })
}

function onProjectChange() {
  load()
  connectWsForProject()
}

/** 轻量粒子 + 邻近连线（常见数据大屏背景动效，无第三方依赖） */
function startParticleCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const state = {
    w: 0,
    h: 0,
    particles: [],
  }

  const COUNT = 55
  const LINK_DIST = 140
  const SPEED = 0.35

  function resize() {
    const parent = canvas.parentElement
    if (!parent) return
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    state.w = parent.clientWidth
    state.h = parent.clientHeight
    canvas.width = Math.floor(state.w * dpr)
    canvas.height = Math.floor(state.h * dpr)
    canvas.style.width = `${state.w}px`
    canvas.style.height = `${state.h}px`
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }

  function initParticles() {
    state.particles = Array.from({ length: COUNT }, () => ({
      x: Math.random() * state.w,
      y: Math.random() * state.h,
      vx: (Math.random() - 0.5) * SPEED,
      vy: (Math.random() - 0.5) * SPEED,
    }))
  }

  function step() {
    const { w, h } = state
    ctx.clearRect(0, 0, w, h)
    const pts = state.particles

    for (let i = 0; i < pts.length; i++) {
      const p = pts[i]
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1
    }

    for (let i = 0; i < pts.length; i++) {
      for (let j = i + 1; j < pts.length; j++) {
        const a = pts[i]
        const b = pts[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const dist = Math.hypot(dx, dy)
        if (dist < LINK_DIST) {
          const alpha = (1 - dist / LINK_DIST) * 0.22
          ctx.strokeStyle = `rgba(0, 212, 255, ${alpha})`
          ctx.lineWidth = 0.6
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }

    for (const p of pts) {
      ctx.fillStyle = 'rgba(0, 230, 255, 0.55)'
      ctx.beginPath()
      ctx.arc(p.x, p.y, 1.2, 0, Math.PI * 2)
      ctx.fill()
    }

    rafId = requestAnimationFrame(step)
  }

  const onResize = () => {
    resize()
    initParticles()
  }

  onResize()
  window.addEventListener('resize', onResize)
  step()

  return () => {
    window.removeEventListener('resize', onResize)
    cancelAnimationFrame(rafId)
  }
}

let stopParticles = () => {}

watch(
  () => ws.connected,
  (v) => {
    if (v) flashPulse()
  },
)

onMounted(() => {
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  loadProjects()
  stopParticles = startParticleCanvas() || (() => {})
})

onUnmounted(() => {
  clearInterval(clockTimer)
  stopParticles()
  ws.disconnect()
})
</script>

<style scoped>
.mag-screen {
  --mag-cyan: #00e8ff;
  --mag-cyan-dim: rgba(0, 232, 255, 0.35);
  --mag-bg: #050a12;
  --mag-panel: rgba(8, 20, 40, 0.72);
  position: relative;
  min-height: 100vh;
  width: 100%;
  margin: -20px -20px 0;
  padding: 20px 24px 32px;
  box-sizing: border-box;
  background: radial-gradient(ellipse 120% 80% at 50% -20%, rgba(0, 100, 180, 0.35), transparent 55%),
    radial-gradient(ellipse 80% 50% at 100% 50%, rgba(80, 0, 120, 0.12), transparent 50%),
    var(--mag-bg);
  color: #e8f4ff;
  overflow: hidden;
  transition: box-shadow 0.35s ease;
}

.mag-screen--pulse {
  box-shadow: inset 0 0 80px rgba(0, 232, 255, 0.08);
}

.mag-screen__canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.9;
}

.mag-screen__grid {
  position: absolute;
  inset: -50%;
  z-index: 0;
  pointer-events: none;
  background-image: linear-gradient(rgba(0, 232, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 232, 255, 0.04) 1px, transparent 1px);
  background-size: 48px 48px;
  transform: perspective(500px) rotateX(60deg) translateY(-12%);
  animation: mag-grid-drift 28s linear infinite;
  opacity: 0.5;
}

@keyframes mag-grid-drift {
  0% {
    transform: perspective(500px) rotateX(60deg) translateY(-12%) translateZ(0);
  }
  100% {
    transform: perspective(500px) rotateX(60deg) translateY(-12%) translateZ(0) translate(48px, 48px);
  }
}

.mag-screen__scan {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(0, 232, 255, 0.03) 48%,
    transparent 52%
  );
  background-size: 100% 220%;
  animation: mag-scan 7s ease-in-out infinite;
  opacity: 0.85;
}

@keyframes mag-scan {
  0%,
  100% {
    background-position: 0 -40%;
  }
  50% {
    background-position: 0 140%;
  }
}

.mag-screen__content {
  position: relative;
  z-index: 2;
  max-width: 1400px;
  margin: 0 auto;
}

.mag-screen__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 232, 255, 0.2);
  box-shadow: 0 1px 0 rgba(0, 232, 255, 0.06);
}

.mag-screen__title {
  margin: 0;
  font-size: clamp(1.5rem, 3.5vw, 2rem);
  font-weight: 600;
  letter-spacing: 0.12em;
  background: linear-gradient(90deg, #fff 0%, var(--mag-cyan) 45%, #7ecbff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 40px rgba(0, 232, 255, 0.3);
}

.mag-screen__subtitle {
  margin: 6px 0 0;
  font-size: 0.85rem;
  color: rgba(200, 230, 255, 0.55);
  letter-spacing: 0.2em;
}

.mag-screen__clock {
  font-family: ui-monospace, monospace;
  font-size: 0.95rem;
  color: var(--mag-cyan-dim);
  text-shadow: 0 0 12px rgba(0, 232, 255, 0.25);
}

.mag-screen__toolbar {
  margin-bottom: 28px;
}

.mag-screen__toolbar-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 16px;
  padding: 14px 18px;
  background: var(--mag-panel);
  border: 1px solid rgba(0, 232, 255, 0.18);
  border-radius: 4px;
  box-shadow: 0 0 24px rgba(0, 80, 120, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(8px);
}

.mag-screen__label {
  font-size: 0.8rem;
  color: rgba(200, 230, 255, 0.65);
}

.mag-screen__select {
  width: 260px;
}

.mag-screen__btn {
  --el-button-bg-color: rgba(0, 120, 160, 0.35);
  --el-button-border-color: rgba(0, 232, 255, 0.45);
  --el-button-text-color: #e8f4ff;
  --el-button-hover-bg-color: rgba(0, 160, 200, 0.45);
  --el-button-hover-border-color: var(--mag-cyan);
}

.mag-screen__ws {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  font-size: 0.8rem;
  color: rgba(200, 220, 240, 0.55);
}

.mag-screen__ws--on {
  color: rgba(120, 255, 200, 0.9);
}

.mag-screen__ws-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(120, 130, 150, 0.6);
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.4);
}

.mag-screen__ws--on .mag-screen__ws-dot {
  background: #3dff9a;
  box-shadow: 0 0 10px rgba(61, 255, 154, 0.75);
  animation: mag-pulse-dot 1.6s ease-in-out infinite;
}

@keyframes mag-pulse-dot {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.15);
    opacity: 0.85;
  }
}

.mag-screen__metrics {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.mag-tile {
  position: relative;
  padding: 22px 20px 20px;
  background: linear-gradient(145deg, rgba(12, 28, 52, 0.9), rgba(6, 14, 28, 0.92));
  border: 1px solid rgba(0, 232, 255, 0.22);
  border-radius: 2px;
  box-shadow: 0 0 20px rgba(0, 60, 100, 0.2), inset 0 0 40px rgba(0, 100, 180, 0.04);
  animation: mag-tile-in 0.65s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: var(--d, 0s);
  overflow: hidden;
}

.mag-tile::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 40%, rgba(0, 232, 255, 0.06) 50%, transparent 60%);
  background-size: 200% 100%;
  animation: mag-tile-shine 5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes mag-tile-in {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.98);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: none;
    filter: none;
  }
}

@keyframes mag-tile-shine {
  0%,
  100% {
    background-position: 100% 0;
  }
  50% {
    background-position: -100% 0;
  }
}

.mag-tile__corners::before,
.mag-tile__corners::after {
  content: '';
  position: absolute;
  width: 14px;
  height: 14px;
  border-color: var(--mag-cyan);
  border-style: solid;
  opacity: 0.65;
  pointer-events: none;
}

.mag-tile__corners::before {
  top: 6px;
  left: 6px;
  border-width: 2px 0 0 2px;
}

.mag-tile__corners::after {
  bottom: 6px;
  right: 6px;
  border-width: 0 2px 2px 0;
}

.mag-tile__label {
  font-size: 0.82rem;
  color: rgba(180, 210, 235, 0.75);
  margin-bottom: 10px;
  letter-spacing: 0.06em;
}

.mag-tile__value {
  font-variant-numeric: tabular-nums;
}

.mag-tile__num {
  font-size: clamp(2rem, 4vw, 2.75rem);
  font-weight: 700;
  color: #fff;
  text-shadow: 0 0 24px rgba(0, 232, 255, 0.45), 0 0 48px rgba(0, 120, 200, 0.25);
  animation: mag-num-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  animation-delay: calc(var(--d, 0s) + 0.1s);
}

@keyframes mag-num-pop {
  from {
    transform: scale(0.85);
    opacity: 0.5;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.mag-screen__empty {
  text-align: center;
  padding: 48px 24px;
  color: rgba(180, 210, 235, 0.45);
  font-size: 0.95rem;
}
</style>

<style>
/* 下拉浮层：与深色大屏一致 */
.mag-screen-select-popper.el-popper {
  --el-bg-color-overlay: rgba(10, 22, 42, 0.96);
  --el-border-color-light: rgba(0, 232, 255, 0.25);
  --el-text-color-regular: #e8f4ff;
  --el-fill-color-light: rgba(0, 100, 140, 0.35);
}
</style>
