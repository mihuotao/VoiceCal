import { Solar } from 'lunar-javascript'

export interface FestivalDetail {
  date: string          // 日期说明
  type: string          // 节日类型
  summary: string       // 简介
  customs: string[]     // 习俗
  food: string[]        // 美食
  poetry?: string       // 诗词
  poet?: string         // 诗人
}

export interface FestivalInfo {
  name: string
  type: 'festival' | 'solarTerm' | 'anniversary'
  emoji: string
  greeting: string
  detail?: FestivalDetail
}

const festivalDetails: Record<string, FestivalDetail> = {
  '元旦': {
    date: '公历1月1日',
    type: '世界性节日',
    summary: '元旦，即公历的1月1日，是世界多数国家通称的"新年"。元，谓"始"，凡数之始称为"元"；旦，谓"日"；"元旦"即"初始之日"的意思。',
    customs: ['放假休息', '跨年活动', '新年倒计时', '家庭聚会'],
    food: ['年糕', '饺子', '汤圆'],
    poetry: '爆竹声中一岁除，春风送暖入屠苏。',
    poet: '王安石《元日》'
  },
  '春节': {
    date: '农历正月初一',
    type: '中国传统节日',
    summary: '春节，即中国农历新年，俗称新春、新岁、岁旦等，口头上又称过年、过大年。春节历史悠久，由上古时代岁首祈岁祭祀演变而来。万物本乎天、人本乎祖，祈岁祭祀、敬天法祖，报本反始也。',
    customs: ['贴春联', '放鞭炮', '拜年', '发红包', '守岁', '舞龙舞狮'],
    food: ['饺子', '年糕', '春卷', '汤圆', '鱼'],
    poetry: '爆竹声中一岁除，春风送暖入屠苏。',
    poet: '王安石《元日》'
  },
  '元宵节': {
    date: '农历正月十五',
    type: '中国传统节日',
    summary: '元宵节，又称上元节、小正月、元夕或灯节，是中国的传统节日之一，时间为每年农历正月十五。正月是农历的元月，古人称"夜"为"宵"，正月十五是一年中第一个月圆之夜，所以称正月十五为"元宵节"。',
    customs: ['赏花灯', '吃元宵', '猜灯谜', '放烟花', '舞龙舞狮'],
    food: ['元宵', '汤圆', '面条'],
    poetry: '去年元夜时，花市灯如昼。',
    poet: '欧阳修《生查子·元夕》'
  },
  '清明节': {
    date: '公历4月5日前后',
    type: '中国传统节日',
    summary: '清明节，又称踏青节、行清节、三月节、祭祖节等，节期在仲春与暮春之交。清明节源自上古时代的祖先信仰与春祭礼俗，兼具自然与人文两大内涵，既是自然节气点，也是传统节日。',
    customs: ['扫墓祭祖', '踏青', '插柳', '放风筝', '荡秋千'],
    food: ['青团', '馓子', '清明果'],
    poetry: '清明时节雨纷纷，路上行人欲断魂。',
    poet: '杜牧《清明》'
  },
  '劳动节': {
    date: '公历5月1日',
    type: '世界性节日',
    summary: '国际劳动节，又称"五一国际劳动节"，是世界上80多个国家的全国性节日。定在每年的五月一日。它是全世界劳动人民共同拥有的节日。',
    customs: ['放假休息', '旅游出行', '表彰劳模'],
    food: ['无特定食物'],
    poetry: '锄禾日当午，汗滴禾下土。',
    poet: '李绅《悯农》'
  },
  '青年节': {
    date: '公历5月4日',
    type: '中国纪念日',
    summary: '五四青年节源于中国1919年反帝爱国的"五四运动"，五四爱国运动是一次彻底的反对帝国主义和封建主义的爱国运动，也是中国新民主主义革命的开始。1939年，陕甘宁边区西北青年救国联合会规定5月4日为中国青年节。',
    customs: ['青年活动', '志愿服务', '文艺演出'],
    food: ['无特定食物'],
    poetry: '恰同学少年，风华正茂。',
    poet: '毛泽东《沁园春·长沙》'
  },
  '端午节': {
    date: '农历五月初五',
    type: '中国传统节日',
    summary: '端午节，又称端阳节、龙舟节、重午节、天中节等，是集拜神祭祖、祈福辟邪、欢庆娱乐和饮食为一体的民俗大节。端午节源于自然天象崇拜，由上古时代祭龙演变而来。因传说战国时期的楚国诗人屈原在五月五日跳汨罗江自尽，后来人们亦将端午节作为纪念屈原的节日。',
    customs: ['赛龙舟', '吃粽子', '挂艾草', '饮雄黄酒', '佩香囊', '系五彩绳'],
    food: ['粽子', '雄黄酒', '五黄', '打糕'],
    poetry: '节分端午自谁言，万古传闻为屈原。',
    poet: '文秀《端午》'
  },
  '七夕节': {
    date: '农历七月初七',
    type: '中国传统节日',
    summary: '七夕节，又称七巧节、七姐节、女儿节、乞巧节等，是中国民间的传统节日。七夕节由星宿崇拜衍化而来，为传统意义上的七姐诞，因拜祭"七姐"活动在七月七晩上举行，故名"七夕"。经历史发展，七夕被赋予了"牛郎织女"的爱情传说，使其成为了象征爱情的节日。',
    customs: ['乞巧', '拜织女', '吃巧果', '观星'],
    food: ['巧果', '酥糖'],
    poetry: '两情若是久长时，又岂在朝朝暮暮。',
    poet: '秦观《鹊桥仙》'
  },
  '中秋节': {
    date: '农历八月十五',
    type: '中国传统节日',
    summary: '中秋节，又称祭月节、月光诞、月夕、秋节、仲秋节、拜月节、月娘节、月亮节、团圆节等，是中国民间的传统节日。中秋节源自天象崇拜，由上古时代秋夕祭月演变而来。中秋节自古便有祭月、赏月、吃月饼、看花灯、赏桂花、饮桂花酒等民俗，流传至今，经久不息。',
    customs: ['赏月', '吃月饼', '点灯笼', '猜灯谜', '饮桂花酒'],
    food: ['月饼', '桂花酒', '芋头', '田螺'],
    poetry: '但愿人长久，千里共婵娟。',
    poet: '苏轼《水调歌头》'
  },
  '重阳节': {
    date: '农历九月初九',
    type: '中国传统节日',
    summary: '重阳节，是中国民间传统节日，节期在每年农历九月初九日。"九"数在《易经》中为阳数，"九九"两阳数相重，故曰"重阳"；因日与月皆逢九，故又称为"重九"。九九归真，一元肇始，古人认为九九重阳是吉祥的日子。',
    customs: ['登高望远', '赏菊', '饮菊花酒', '佩插茱萸', '敬老活动'],
    food: ['重阳糕', '菊花酒', '螃蟹'],
    poetry: '遥知兄弟登高处，遍插茱萸少一人。',
    poet: '王维《九月九日忆山东兄弟》'
  },
  '国庆节': {
    date: '公历10月1日',
    type: '中国国家节日',
    summary: '中华人民共和国国庆节是中华人民共和国的建立纪念日。1949年10月1日，在北京天安门广场举行了开国大典，中华人民共和国中央人民政府正式成立。1949年12月2日，中央人民政府通过《关于中华人民共和国国庆日的决议》，规定每年10月1日为国庆日。',
    customs: ['放假休息', '阅兵仪式', '升旗仪式', '旅游出行'],
    food: ['无特定食物'],
    poetry: '中华儿女多奇志，不爱红装爱武装。',
    poet: '毛泽东《七绝·为女民兵题照》'
  },
  '除夕': {
    date: '农历十二月廿九或三十',
    type: '中国传统节日',
    summary: '除夕，为岁末的最后一天夜晚。岁末的最后一天称为"岁除"，意为旧岁至此而除，另换新岁。除，即去除之意；夕，指夜晚。"除夕"是岁除之夜的意思，又称大年夜、除夕夜、除夜等，时值年尾的最后一个晚上。',
    customs: ['守岁', '吃年夜饭', '贴春联', '放鞭炮', '发红包', '看春晚'],
    food: ['年夜饭', '饺子', '鱼', '年糕'],
    poetry: '故乡今夜思千里，霜鬓明朝又一年。',
    poet: '高适《除夜作》'
  },
  '母亲节': {
    date: '公历5月第二个星期日',
    type: '国际性节日',
    summary: '母亲节是一个感谢母亲的节日，现代母亲节起源于美国，由安娜·贾维斯发起。1913年，美国国会确定将每年5月的第二个星期日定为母亲节。母亲节在中国地区是在港澳台地区流行起来之后才进入大陆的。',
    customs: ['送康乃馨', '送礼物', '陪伴母亲', '家庭聚餐'],
    food: ['蛋糕', '母亲喜欢的食物'],
    poetry: '谁言寸草心，报得三春晖。',
    poet: '孟郊《游子吟》'
  },
  '父亲节': {
    date: '公历6月第三个星期日',
    type: '国际性节日',
    summary: '父亲节，顾名思义是感恩父亲的节日。约始于二十世纪初，起源于美国，现已广泛流传于世界各地，节日日期因地域而存在差异。最广泛的日期在每年6月的第三个星期日，世界上有52个国家和地区是在这一天过父亲节。',
    customs: ['送礼物', '陪伴父亲', '家庭聚餐'],
    food: ['无特定食物'],
    poetry: '无父何怙，无母何恃。',
    poet: '《诗经》'
  },
  '万圣节': {
    date: '公历10月31日',
    type: '西方传统节日',
    summary: '万圣节又叫诸圣节，在每年的11月1日，是西方的传统节日；而万圣节前夜的10月31日是这个节日最热闹的时刻。在中文里，常常把万圣节前夜（Halloween）讹译为万圣节。',
    customs: ['南瓜灯', '不给糖就捣蛋', '化妆舞会', '鬼屋'],
    food: ['南瓜派', '苹果糖', '糖果'],
    poetry: '',
    poet: ''
  },
  '圣诞节': {
    date: '公历12月25日',
    type: '西方传统节日',
    summary: '圣诞节又称耶诞节，译名为"基督弥撒"，西方传统节日，在每年12月25日。弥撒是教会的一种礼拜仪式。圣诞节是一个宗教节，因为把它当作耶稣的诞辰来庆祝，故名"耶诞节"。',
    customs: ['装饰圣诞树', '交换礼物', '圣诞老人', '唱圣诞歌'],
    food: ['火鸡', '姜饼', '圣诞蛋糕'],
    poetry: '',
    poet: ''
  },
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

// 农历节日映射：key 为 "月-日" 格式
const lunarFestivalMap: Record<string, string> = {
  '1-1': '春节',
  '1-15': '元宵节',
  '2-2': '龙抬头',
  '3-3': '上巳节',
  '4-4': '寒食节',
  '5-5': '端午节',
  '7-7': '七夕节',
  '7-15': '中元节',
  '8-15': '中秋节',
  '9-9': '重阳节',
  '10-1': '寒衣节',
  '10-15': '下元节',
  '12-8': '腊八节',
  '12-23': '小年',
  '12-30': '除夕',
  '12-29': '除夕',  // 小月时除夕是二十九
}

export function useFestival() {
  function getFestivalInfo(year: number, month: number, day: number): FestivalInfo | null {
    try {
      const solar = Solar.fromYmd(year, month + 1, day)
      const lunar = solar.getLunar()

      // 1. 检查公历节日
      const solarFestivals: string[] = [
        ...(solar.getFestivals() as string[]),
        ...(solar.getOtherFestivals() as string[])
      ]

      for (const name of solarFestivals) {
        const info = festivalGreetings[name]
        if (info) {
          const detail = festivalDetails[name]
          return { name, type: 'festival', ...info, detail }
        }
      }

      // 2. 检查农历节日
      const lunarMonth = lunar.getMonth()
      const lunarDay = lunar.getDay()
      const lunarKey = `${lunarMonth}-${lunarDay}`
      const lunarFestivalName = lunarFestivalMap[lunarKey]

      if (lunarFestivalName) {
        const info = festivalGreetings[lunarFestivalName]
        if (info) {
          const detail = festivalDetails[lunarFestivalName]
          return { name: lunarFestivalName, type: 'festival', ...info, detail }
        }
      }

      // 3. 检查节气
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
