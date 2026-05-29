export interface SearchResultItem {
  title: string
  snippet: string
  url: string
  icon?: string
}

export interface SearchResponse {
  results: SearchResultItem[]
  summary?: string
  query: string
}

export type SearchType = 'weather' | 'place' | 'general' | 'news'
