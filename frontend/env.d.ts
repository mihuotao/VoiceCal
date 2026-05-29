/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

declare module 'lunar-javascript' {
  export class Solar {
    static fromYmd(year: number, month: number, day: number): Solar
    getLunar(): Lunar
    toFullString(): string
    getYear(): number
    getMonth(): number
    getDay(): number
  }

  export class Lunar {
    toFullString(): string
    getYear(): number
    getMonth(): number
    getDay(): number
    getMonthInChinese(): string
    getDayInChinese(): string
  }

  export class HolidayUtil {
    static getHoliday(year: number, month: number, day: number): Holiday | null
  }

  export class Holiday {
    getName(): string
  }
}
