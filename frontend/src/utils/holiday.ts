const holidayMap: Record<string, { name: string; isOff: boolean }> = {
  '01-01': { name: '元旦', isOff: true },
  '01-02': { name: '元旦', isOff: true },
  '01-03': { name: '元旦', isOff: true },
  '02-09': { name: '除夕', isOff: true },
  '02-10': { name: '春节', isOff: true },
  '02-11': { name: '春节', isOff: true },
  '02-12': { name: '春节', isOff: true },
  '02-13': { name: '春节', isOff: true },
  '02-14': { name: '春节', isOff: true },
  '02-15': { name: '春节', isOff: true },
  '02-16': { name: '春节', isOff: true },
  '02-17': { name: '春节', isOff: true },
  '04-04': { name: '清明', isOff: true },
  '04-05': { name: '清明', isOff: true },
  '04-06': { name: '清明', isOff: true },
  '05-01': { name: '劳动节', isOff: true },
  '05-02': { name: '劳动节', isOff: true },
  '05-03': { name: '劳动节', isOff: true },
  '05-04': { name: '劳动节', isOff: true },
  '05-05': { name: '劳动节', isOff: true },
  '06-19': { name: '端午', isOff: true },
  '06-20': { name: '端午', isOff: true },
  '06-21': { name: '端午', isOff: true },
  '10-01': { name: '国庆', isOff: true },
  '10-02': { name: '国庆', isOff: true },
  '10-03': { name: '国庆', isOff: true },
  '10-04': { name: '国庆', isOff: true },
  '10-05': { name: '国庆', isOff: true },
  '10-06': { name: '国庆', isOff: true },
  '10-07': { name: '国庆', isOff: true },
}

export function getHolidayInfo(month: number, day: number): { name: string; isOff: boolean } | null {
  const key = `${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  return holidayMap[key] || null
}
