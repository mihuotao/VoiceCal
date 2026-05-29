declare module 'lunar-javascript' {
  export class Solar {
    static fromYmd(year: number, month: number, day: number): Solar
    getLunar(): Lunar
    toFullString(): string
    getYear(): number
    getMonth(): number
    getDay(): number
    getWeekInChinese(): string
    getFestivals(): string[]
    getOtherFestivals(): string[]
  }

  export class Lunar {
    toFullString(): string
    getYear(): number
    getMonth(): number
    getDay(): number
    getMonthInChinese(): string
    getDayInChinese(): string
    getYearInChinese(): string
    getYearShengXiao(): string
    getJie(): string
    getQi(): string
    getFestivals(): string[]
    getOtherFestivals(): string[]
  }

  export class SolarUtil {
    static getFestivals(month: number, day: number): string[]
  }

  export class LunarUtil {
    static getFestivals(month: number, day: number): string[]
  }

  export class HolidayUtil {
    static getHoliday(year: number, month: number, day: number): Holiday | null
  }

  export class Holiday {
    getName(): string
  }
}
