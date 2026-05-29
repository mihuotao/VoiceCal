export interface CalendarCellData {
  year: number
  month: number
  day: number
  isCurrentMonth: boolean
  isToday: boolean
  lunarDay: string
  lunarMonth: string
  festivals: string[]
  jieQi: string
  isWeekend: boolean
  dateKey: string
}

export interface GridRow {
  cells: CalendarCellData[]
}
