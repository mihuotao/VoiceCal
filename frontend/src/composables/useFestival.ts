import { Solar } from 'lunar-javascript'

export interface FestivalInfo {
  name: string
  type: 'festival' | 'solarTerm' | 'anniversary'
  emoji: string
  greeting: string
}

const festivalGreetings: Record<string, { emoji: string; greeting: string }> = {
  '元旦': { emoji: '🎊', greeting: '新年新气象，万事如意！' },
  '情人节': { emoji: '💕', greeting: '愿有情人终成眷属！' },
  '妇女节': { emoji: '🌸', greeting: '女神节快乐！' },
  '植树节': { emoji: '🌳', greeting: '绿化家园，从我做起！' },
  '消费者权益日': { emoji: '📢', greeting: '维护权益，理性消费！' },
  '愚人节': { emoji: '🤡', greeting: '愚人节快乐，小心被整哦！' },
  '劳动节': { emoji: '🔨', greeting: '劳动最光荣，节日快乐！' },
  '青年节': { emoji: '🔥', greeting: '青春不息，奋斗不止！' },
  '儿童节': { emoji: '🎈', greeting: '童心未泯，快乐永在！' },
  '建党节': { emoji: '🎌', greeting: '不忘初心，牢记使命！' },
  '建军节': { emoji: '🎖️', greeting: '致敬最可爱的人！' },
  '教师节': { emoji: '📚', greeting: '师恩难忘，节日快乐！' },
  '国庆节': { emoji: '🇨🇳', greeting: '祖国繁荣昌盛！' },
  '万圣节': { emoji: '🎃', greeting: 'Trick or Treat！' },
  '感恩节': { emoji: '🦃', greeting: '感恩有你，一路同行！' },
  '平安夜': { emoji: '🎄', greeting: '平安夜，愿你平安喜乐！' },
  '圣诞节': { emoji: '🎅', greeting: 'Merry Christmas！' },
  '除夕': { emoji: '🧧', greeting: '辞旧迎新，阖家团圆！' },
  '春节': { emoji: '🏮', greeting: '新春快乐，万事大吉！' },
  '元宵节': { emoji: '🏮', greeting: '元宵快乐，团团圆圆！' },
  '龙抬头': { emoji: '🐲', greeting: '龙抬头，好运来！' },
  '上巳节': { emoji: '🌿', greeting: '上巳佳节，踏青赏春！' },
  '寒食节': { emoji: '🍃', greeting: '寒食安康！' },
  '清明节': { emoji: '🌿', greeting: '清明时节，缅怀先人！' },
  '端午节': { emoji: '🐉', greeting: '端午安康，粽叶飘香！' },
  '七夕节': { emoji: '💫', greeting: '七夕快乐，有情人终成眷属！' },
  '中元节': { emoji: '🕯️', greeting: '中元安康！' },
  '中秋节': { emoji: '🥮', greeting: '中秋快乐，阖家团圆！' },
  '重阳节': { emoji: '🏔️', greeting: '重阳登高，福寿安康！' },
  '寒衣节': { emoji: '🧥', greeting: '天凉了，注意保暖！' },
  '下元节': { emoji: '🏮', greeting: '下元安康！' },
  '腊八节': { emoji: '🥣', greeting: '腊八快乐，记得喝粥哦！' },
  '小年': { emoji: '🧹', greeting: '小年快乐，准备过年啦！' },
  '母亲节': { emoji: '💐', greeting: '妈妈辛苦了，母亲节快乐！' },
  '父亲节': { emoji: '👔', greeting: '爸爸辛苦了，父亲节快乐！' },
  '世界地球日': { emoji: '🌍', greeting: '保护地球，人人有责！' },
  '世界环境日': { emoji: '🌱', greeting: '爱护环境，从我做起！' },
}

const solarTermGreetings: Record<string, { emoji: string; greeting: string }> = {
  '立春': { emoji: '🌱', greeting: '立春到，万物复苏！' },
  '雨水': { emoji: '🌧️', greeting: '雨水时节，润物无声！' },
  '惊蛰': { emoji: '⚡', greeting: '惊蛰到，春雷响，万物长！' },
  '春分': { emoji: '🌸', greeting: '春分至，昼夜等长！' },
  '清明': { emoji: '🌿', greeting: '清明时节雨纷纷！' },
  '谷雨': { emoji: '🌾', greeting: '谷雨前后，种瓜点豆！' },
  '立夏': { emoji: '☀️', greeting: '立夏到，夏日来临！' },
  '小满': { emoji: '🌾', greeting: '小满不满，干断田坎！' },
  '芒种': { emoji: '🌾', greeting: '芒种忙，麦上场！' },
  '夏至': { emoji: '🌞', greeting: '夏至到，白昼最长！' },
  '小暑': { emoji: '🌡️', greeting: '小暑至，盛夏始！' },
  '大暑': { emoji: '🔥', greeting: '大暑炎热，注意防暑！' },
  '立秋': { emoji: '🍂', greeting: '立秋到，凉风至！' },
  '处暑': { emoji: '🍁', greeting: '处暑至，暑气渐消！' },
  '白露': { emoji: '💧', greeting: '白露秋风夜，一夜凉一夜！' },
  '秋分': { emoji: '🍁', greeting: '秋分至，昼夜等长！' },
  '寒露': { emoji: '🌫️', greeting: '寒露至，秋意浓！' },
  '霜降': { emoji: '❄️', greeting: '霜降到，添衣保暖！' },
  '立冬': { emoji: '🧣', greeting: '立冬到，注意保暖！' },
  '小雪': { emoji: '🌨️', greeting: '小雪到，冬始俏！' },
  '大雪': { emoji: '❄️', greeting: '大雪纷飞，注意保暖！' },
  '冬至': { emoji: '🥟', greeting: '冬至快乐，记得吃饺子！' },
  '小寒': { emoji: '🥶', greeting: '小寒至，严寒始！' },
  '大寒': { emoji: '❄️', greeting: '大寒到，注意防寒保暖！' },
}

export function useFestival() {
  function getFestivalInfo(year: number, month: number, day: number): FestivalInfo | null {
    try {
      const solar = Solar.fromYmd(year, month + 1, day)
      const lunar = solar.getLunar()

      const festivals: string[] = [
        ...(solar.getFestivals() as string[]),
        ...(solar.getOtherFestivals() as string[])
      ]

      for (const name of festivals) {
        const info = festivalGreetings[name]
        if (info) {
          return { name, type: 'festival', ...info }
        }
      }

      const jie = lunar.getJie() as string
      const qi = lunar.getQi() as string
      const jieQi = jie || qi || ''
      if (jieQi) {
        const info = solarTermGreetings[jieQi]
        if (info) {
          return { name: jieQi, type: 'solarTerm', ...info }
        }
      }

      return null
    } catch {
      return null
    }
  }

  function getFestivalForDate(dateKey: string): FestivalInfo | null {
    const [y, m, d] = dateKey.split('-').map(Number)
    return getFestivalInfo(y, m - 1, d)
  }

  return {
    getFestivalInfo,
    getFestivalForDate
  }
}
