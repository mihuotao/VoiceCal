import { ref } from 'vue'
import axios from 'axios'
import type { SearchResponse, SearchResultItem, SearchType } from '@/types/search'

export function useSearch() {
  const results = ref<SearchResultItem[]>([])
  const summary = ref('')
  const searchQuery = ref('')
  const isSearching = ref(false)
  const searchError = ref<string | null>(null)

  async function search(query: string, type: SearchType = 'general') {
    searchQuery.value = query
    isSearching.value = true
    searchError.value = null
    results.value = []
    summary.value = ''

    try {
      const res = await axios.post<SearchResponse>('/api/search/web', { query, type })
      results.value = res.data.results
      summary.value = res.data.summary ?? ''
    } catch {
      mockSearch(query)
    } finally {
      isSearching.value = false
    }
  }

  function mockSearch(query: string) {
    const lower = query.toLowerCase()
    let items: SearchResultItem[]

    if (lower.includes('天气') || lower.includes('weather')) {
      items = [
        { title: '北京天气预报', snippet: '明日晴转多云，气温18-28°C，南风3-4级，适宜户外活动。', url: 'https://weather.com/beijing', icon: '☀️' },
        { title: '本周天气趋势', snippet: '周三起将有一轮降温，周末有小到中雨，请提前安排出行。', url: 'https://weather.com/trend', icon: '🌤️' }
      ]
      summary.value = '未来三天北京天气晴好，气温适宜，适合出行和户外活动。'
    } else if (lower.includes('餐厅') || lower.includes('restaurant') || lower.includes('吃')) {
      items = [
        { title: '京味轩中餐厅', snippet: '评分 4.5/5 · 人均 ¥120 · 朝阳区建国路88号 · 北京烤鸭推荐', url: 'https://dianping.com/jingweixuan', icon: '🍜' },
        { title: 'Sakura 日料', snippet: '评分 4.7/5 · 人均 ¥280 · 三里屯太古里 · 需提前预订', url: 'https://dianping.com/sakura', icon: '🍣' }
      ]
      summary.value = '为您找到 2 家附近热门餐厅，已根据评分排序。'
    } else {
      items = [
        { title: `${query} - 搜索结果`, snippet: `关于"${query}"的相关信息，点击查看详情。`, url: '#', icon: '🔍' },
        { title: `${query} - 百科`, snippet: `${query}的详细介绍和相关信息汇总。`, url: '#', icon: '📖' }
      ]
      summary.value = `为您找到关于"${query}"的相关结果。`
    }

    results.value = items
  }

  function clearSearch() {
    results.value = []
    summary.value = ''
    searchQuery.value = ''
    searchError.value = null
  }

  return {
    results,
    summary,
    searchQuery,
    isSearching,
    searchError,
    search,
    clearSearch
  }
}
