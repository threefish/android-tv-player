import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export interface FileInfo {
  id: number
  path: string
  name: string
  isDirectory: boolean
  size: number
  modifiedTime: number
}

export interface PlayHistory {
  id: number
  filePath: string
  fileName: string
  position: number
  duration: number
  lastPlayedTime: number
}

export interface AiProvider {
  id: number
  name: string
  baseUrl: string
  apiKey: string
  model: string
  isActive: boolean
  createTime: number
  updateTime: number
}

export const aiProviderApi = {
  list: () =>
    api.get<AiProvider[]>('/config/ai-providers'),
  
  getActive: () =>
    api.get<AiProvider>('/config/ai-providers/active'),
  
  create: (provider: Omit<AiProvider, 'id' | 'createTime' | 'updateTime'>) =>
    api.post('/config/ai-providers', provider),
  
  update: (id: number, provider: Omit<AiProvider, 'id' | 'createTime' | 'updateTime'>) =>
    api.put(`/config/ai-providers/${id}`, provider),
  
  delete: (id: number) =>
    api.delete(`/config/ai-providers/${id}`),
  
  activate: (id: number) =>
    api.post(`/config/ai-providers/${id}/activate`)
}

export const fileApi = {
  list: (path: string = '/') => 
    api.get<FileInfo[]>('/files', { params: { path } }),
  
  search: (keyword: string, fileType: string = 'all') =>
    api.get<FileInfo[]>('/files/search', { params: { keyword, fileType } }),
  
  rename: (path: string, newName: string) =>
    api.post(`/files/${encodeURIComponent(path)}/rename`, { newName }),
  
  move: (path: string, targetDir: string) =>
    api.post(`/files/${encodeURIComponent(path)}/move`, { targetDir }),
  
  delete: (path: string) =>
    api.post(`/files/${encodeURIComponent(path)}/delete`),
  
  restore: (path: string) =>
    api.post(`/files/${encodeURIComponent(path)}/restore`),
  
  getTrash: () =>
    api.get<FileInfo[]>('/trash'),
  
  getHistory: () =>
    api.get<PlayHistory[]>('/history'),
  
  getRecentHistory: (limit: number = 10) =>
    api.get<PlayHistory[]>('/history/recent', { params: { limit } })
}

export default api
