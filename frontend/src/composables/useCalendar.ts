import { ref, computed } from 'vue'

export type MonthDirection = 'left' | 'right' | 'none'

export function useCalendar() {
  const now = new Date()
  const currentYear = ref(now.getFullYear())
  const currentMonth = ref(now.getMonth())
  const transitionDirection = ref<MonthDirection>('none')
  const isTransitioning = ref(false)
  const supportsViewTransition = typeof document !== 'undefined'
    && 'startViewTransition' in document

  function getDaysInMonth(year: number, month: number): number {
    return new Date(year, month + 1, 0).getDate()
  }

  function getFirstDayOfMonth(year: number, month: number): number {
    return new Date(year, month, 1).getDay()
  }

  const monthYearLabel = computed(() => {
    return `${currentYear.value}年${currentMonth.value + 1}月`
  })

  const yearLabel = computed(() => {
    return `${currentYear.value}年`
  })

  const todayLabel = computed(() => {
    const d = new Date()
    return `${d.getMonth() + 1}月${d.getDate()}日`
  })

  const isCurrentMonth = computed(() => {
    const d = new Date()
    return d.getFullYear() === currentYear.value && d.getMonth() === currentMonth.value
  })

  const daysInMonth = computed(() => {
    return getDaysInMonth(currentYear.value, currentMonth.value)
  })

  const firstDayOfMonth = computed(() => {
    return getFirstDayOfMonth(currentYear.value, currentMonth.value)
  })

  const prevMonthDays = computed(() => {
    const prevMonth = currentMonth.value === 0 ? 11 : currentMonth.value - 1
    const prevYear = currentMonth.value === 0 ? currentYear.value - 1 : currentYear.value
    return getDaysInMonth(prevYear, prevMonth)
  })

  const totalGridCells = computed(() => {
    const total = firstDayOfMonth.value + daysInMonth.value
    return Math.ceil(total / 7) * 7
  })

  function applyMonth(delta: 1 | -1) {
    const newMonth = currentMonth.value + delta
    if (newMonth < 0) {
      currentMonth.value = 11
      currentYear.value--
    } else if (newMonth > 11) {
      currentMonth.value = 0
      currentYear.value++
    } else {
      currentMonth.value = newMonth
    }
  }

  function navigateMonth(delta: 1 | -1) {
    if (isTransitioning.value) return

    transitionDirection.value = delta > 0 ? 'left' : 'right'
    isTransitioning.value = true

    const doUpdate = () => applyMonth(delta)
    if (supportsViewTransition) {
      const transition = document.startViewTransition(doUpdate)
      transition.finished.finally(() => {
        isTransitioning.value = false
        transitionDirection.value = 'none'
      })
    } else {
      doUpdate()
      setTimeout(() => {
        isTransitioning.value = false
        transitionDirection.value = 'none'
      }, 350)
    }
  }

  function nextMonth() {
    navigateMonth(1)
  }

  function prevMonth() {
    navigateMonth(-1)
  }

  function goToToday() {
    const d = new Date()
    currentYear.value = d.getFullYear()
    currentMonth.value = d.getMonth()
    transitionDirection.value = 'none'
  }

  return {
    currentYear,
    currentMonth,
    monthYearLabel,
    yearLabel,
    todayLabel,
    isCurrentMonth,
    daysInMonth,
    firstDayOfMonth,
    prevMonthDays,
    totalGridCells,
    transitionDirection,
    isTransitioning,
    nextMonth,
    prevMonth,
    goToToday,
    navigateMonth,
    supportsViewTransition
  }
}
