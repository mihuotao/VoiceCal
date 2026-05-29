import { ref, computed } from 'vue'
import { getLunarInfo } from '@/utils/lunar'
import type { CalendarCellData } from '@/types/calendar'

export type MonthDirection = 'left' | 'right' | 'none'

export function useCalendar() {
  const now = new Date()
  const currentYear = ref(now.getFullYear())
  const currentMonth = ref(now.getMonth())
  const selectedDate = ref<string>(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`)
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

  const calendarCells = computed<CalendarCellData[]>(() => {
    const cells: CalendarCellData[] = []
    const today = new Date()
    const todayStr = `${today.getFullYear()}-${today.getMonth() + 1}-${today.getDate()}`

    const totalCells = totalGridCells.value
    const firstDay = firstDayOfMonth.value
    const prevMonth = currentMonth.value === 0 ? 11 : currentMonth.value - 1
    const prevYear = currentMonth.value === 0 ? currentYear.value - 1 : currentYear.value
    const prevDays = getDaysInMonth(prevYear, prevMonth)

    for (let i = 0; i < totalCells; i++) {
      let cellYear: number, cellMonth: number, cellDay: number
      let isCurrentMonth: boolean

      if (i < firstDay) {
        cellYear = prevYear
        cellMonth = prevMonth
        cellDay = prevDays - firstDay + i + 1
        isCurrentMonth = false
      } else if (i >= firstDay + daysInMonth.value) {
        const nextMonth = currentMonth.value === 11 ? 0 : currentMonth.value + 1
        const nextYear = currentMonth.value === 11 ? currentYear.value + 1 : currentYear.value
        cellYear = nextYear
        cellMonth = nextMonth
        cellDay = i - firstDay - daysInMonth.value + 1
        isCurrentMonth = false
      } else {
        cellYear = currentYear.value
        cellMonth = currentMonth.value
        cellDay = i - firstDay + 1
        isCurrentMonth = true
      }

      const dateKey = `${cellYear}-${String(cellMonth + 1).padStart(2, '0')}-${String(cellDay).padStart(2, '0')}`
      const isToday = dateKey === todayStr
      const date = new Date(cellYear, cellMonth, cellDay)

      const lunar = getLunarInfo(cellYear, cellMonth, cellDay)

      cells.push({
        year: cellYear,
        month: cellMonth,
        day: cellDay,
        isCurrentMonth,
        isToday,
        lunarDay: lunar.lunarDay,
        lunarMonth: lunar.lunarMonth,
        festivals: lunar.festivals,
        jieQi: lunar.jieQi,
        isWeekend: date.getDay() === 0 || date.getDay() === 6,
        dateKey
      })
    }

    return cells
  })

  function selectDate(dateKey: string) {
    selectedDate.value = dateKey
  }

  function goToToday() {
    const d = new Date()
    currentYear.value = d.getFullYear()
    currentMonth.value = d.getMonth()
    selectedDate.value = `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
    transitionDirection.value = 'none'
  }

  function goToDate(year: number, month: number) {
    currentYear.value = year
    currentMonth.value = month
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
    calendarCells,
    selectedDate,
    transitionDirection,
    isTransitioning,
    nextMonth,
    prevMonth,
    goToToday,
    goToDate,
    selectDate,
    navigateMonth,
    supportsViewTransition
  }
}
