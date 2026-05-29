import { Solar } from 'lunar-javascript'

export interface LunarInfo {
  lunarDay: string
  lunarMonth: string
  festivals: string[]
  jieQi: string
}

const lunarDayMap: Record<number, string> = {
  1: '初一', 2: '初二', 3: '初三', 4: '初四', 5: '初五',
  6: '初六', 7: '初七', 8: '初八', 9: '初九', 10: '初十',
  11: '十一', 12: '十二', 13: '十三', 14: '十四', 15: '十五',
  16: '十六', 17: '十七', 18: '十八', 19: '十九', 20: '二十',
  21: '廿一', 22: '廿二', 23: '廿三', 24: '廿四', 25: '廿五',
  26: '廿六', 27: '廿七', 28: '廿八', 29: '廿九', 30: '三十'
}

export function getLunarInfo(year: number, month: number, day: number): LunarInfo {
  try {
    const solar = Solar.fromYmd(year, month + 1, day)
    const lunar = solar.getLunar()
    const ld = lunar.getDay()

    const festivals: string[] = [
      ...(solar.getFestivals() as string[]),
      ...(solar.getOtherFestivals() as string[])
    ]

    const jie = lunar.getJie() as string
    const qi = lunar.getQi() as string
    const jieQi = jie || qi || ''

    const lunarDay = ld === 1 ? `${lunar.getMonthInChinese()}月` : (lunarDayMap[ld] || `${ld}`)
    const lunarMonth = `${lunar.getMonthInChinese()}月`

    return { lunarDay, lunarMonth, festivals, jieQi }
  } catch {
    return { lunarDay: '', lunarMonth: '', festivals: [], jieQi: '' }
  }
}
