const grad3: [number, number][] = [
  [1, 1], [-1, 1], [1, -1], [-1, -1],
  [1, 0], [-1, 0], [0, 1], [0, -1]
]

const p = new Uint8Array(512)
const perm = new Uint8Array(512)

function buildPermutation() {
  const seed = new Uint8Array(256)
  for (let i = 0; i < 256; i++) seed[i] = i
  for (let i = 255; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [seed[i], seed[j]] = [seed[j], seed[i]]
  }
  for (let i = 0; i < 512; i++) {
    p[i] = seed[i & 255]
    perm[i] = p[i]
  }
}

buildPermutation()

function fade(t: number): number {
  return t * t * t * (t * (t * 6 - 15) + 10)
}

function lerp(a: number, b: number, t: number): number {
  return a + t * (b - a)
}

function dot2(g: [number, number], x: number, y: number): number {
  return g[0] * x + g[1] * y
}

export function noise2D(x: number, y: number): number {
  const X = Math.floor(x) & 255
  const Y = Math.floor(y) & 255
  const xf = x - Math.floor(x)
  const yf = y - Math.floor(y)
  const u = fade(xf)
  const v = fade(yf)
  const aa = perm[perm[X] + Y]
  const ab = perm[perm[X] + Y + 1]
  const ba = perm[perm[X + 1] + Y]
  const bb = perm[perm[X + 1] + Y + 1]
  return lerp(
    lerp(dot2(grad3[aa % 8], xf, yf), dot2(grad3[ba % 8], xf - 1, yf), u),
    lerp(dot2(grad3[ab % 8], xf, yf - 1), dot2(grad3[bb % 8], xf - 1, yf - 1), u),
    v
  )
}

export function noise3D(x: number, y: number, z: number): number {
  return noise2D(x + z * 0.3, y + z * 0.7)
}
