export type SpringConfig = {
  type: 'spring'
  stiffness: number
  damping: number
  mass?: number
}

export const springPresets = {
  gentle: { type: 'spring' as const, stiffness: 300, damping: 20 },
  snappy: { type: 'spring' as const, stiffness: 400, damping: 15 },
  bouncy: { type: 'spring' as const, stiffness: 400, damping: 12 },
  stiff: { type: 'spring' as const, stiffness: 600, damping: 20 },
  drag: { type: 'spring' as const, stiffness: 200, damping: 25 },
  modal: { type: 'spring' as const, stiffness: 500, damping: 25 },
  slide: { type: 'spring' as const, stiffness: 350, damping: 22 }
}

export function useMotion() {
  function createSpring(stiffness: number, damping: number, mass = 1): SpringConfig {
    return { type: 'spring', stiffness, damping, mass }
  }

  function getPreset(name: keyof typeof springPresets): SpringConfig {
    return springPresets[name]
  }

  return {
    createSpring,
    getPreset,
    springPresets
  }
}
