import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent as ReactKeyboardEvent } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { aiProviderApi } from '../api'
import type { AiProvider } from '../api'
import Spinner from '../components/Spinner'
import { toast } from '../components/Toaster'
import { BotIcon, PlusIcon, SendIcon } from '../components/Icons'
import { formatChatTime } from '../utils/format'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

interface ChatSession {
  id: number
  sessionId: string
  title: string
  createTime: number
  updateTime: number
}

interface ChatEvent {
  type: string
  content?: string
  message?: string
}

interface ChatResponse {
  sessionId: string
  events: ChatEvent[]
}

export default function Chat() {
  const [sessions, setSessions] = useState<ChatSession[]>([])
  const [currentSessionId, setCurrentSessionId] = useState('')
  const [isNewChat, setIsNewChat] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [messagesLoading, setMessagesLoading] = useState(false)
  const [providers, setProviders] = useState<AiProvider[]>([])
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null)
  const [aiConfig, setAiConfig] = useState({ baseUrl: '', apiKey: '', model: '' })

  const messagesRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  const selectProvider = (provider: AiProvider) => {
    setSelectedProviderId(provider.id)
    setAiConfig({
      baseUrl: provider.baseUrl,
      apiKey: provider.apiKey,
      model: provider.model
    })
  }

  const refreshSessions = async () => {
    try {
      const res = await axios.get<ChatSession[]>('/api/chat/sessions')
      setSessions(res.data)
    } catch {
      // 静默失败，不打断对话
    }
  }

  const loadMessages = async (sessionId: string) => {
    setMessagesLoading(true)
    try {
      const res = await axios.get<
        Array<{ id: number; role: 'user' | 'assistant'; content: string; createTime: number }>
      >(`/api/chat/sessions/${sessionId}/messages`)
      setMessages(
        res.data.map(item => ({
          id: String(item.id),
          role: item.role,
          content: item.content,
          timestamp: item.createTime
        }))
      )
    } catch {
      toast.error('加载对话记录失败')
    } finally {
      setMessagesLoading(false)
    }
  }

  // 首次进入：加载 Provider 与会话列表
  useEffect(() => {
    let alive = true

    const init = async () => {
      try {
        const providerRes = await aiProviderApi.list()
        if (!alive) return
        const list = providerRes.data
        setProviders(list)
        const preferred = list.find(item => item.isActive) ?? list[0]
        if (preferred) {
          setSelectedProviderId(preferred.id)
          setAiConfig({
            baseUrl: preferred.baseUrl,
            apiKey: preferred.apiKey,
            model: preferred.model
          })
        }
      } catch {
        if (alive) toast.error('加载 AI Provider 失败')
      }

      try {
        const sessionRes = await axios.get<ChatSession[]>('/api/chat/sessions')
        if (!alive) return
        setSessions(sessionRes.data)
        if (sessionRes.data.length > 0) {
          setCurrentSessionId(sessionRes.data[0].sessionId)
        }
      } catch {
        if (alive) toast.error('加载会话列表失败')
      }
    }

    void init()
    return () => {
      alive = false
    }
  }, [])

  useEffect(() => {
    if (currentSessionId) {
      void loadMessages(currentSessionId)
    }
  }, [currentSessionId])

  useEffect(() => {
    const el = messagesRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages, loading])

  const handleSelectSession = (sessionId: string) => {
    if (sessionId === currentSessionId && !isNewChat) return
    setIsNewChat(false)
    setMessages([])
    setCurrentSessionId(sessionId)
  }

  const handleNewChat = () => {
    setIsNewChat(true)
    setCurrentSessionId('')
    setMessages([])
    setInput('')
    window.setTimeout(() => inputRef.current?.focus(), 60)
  }

  const handleInputChange = (value: string) => {
    setInput(value)
    const el = inputRef.current
    if (el) {
      el.style.height = 'auto'
      el.style.height = `${Math.min(el.scrollHeight, 112)}px`
    }
  }

  const handleSend = async () => {
    const text = input.trim()
    if (!text || loading) return
    if (!aiConfig.apiKey) {
      toast.error('请先在设置中配置 AI Provider')
      return
    }

    const userMessage: Message = {
      id: `u-${Date.now()}`,
      role: 'user',
      content: text,
      timestamp: Date.now()
    }
    setMessages(prev => [...prev, userMessage])
    setInput('')
    if (inputRef.current) inputRef.current.style.height = 'auto'
    setLoading(true)

    const requestSessionId = isNewChat ? '' : currentSessionId

    try {
      const res = await axios.post<ChatResponse>('/api/chat/send', {
        sessionId: requestSessionId,
        message: text,
        ...aiConfig
      })

      let assistantContent = ''
      let errorMessage = ''
      for (const event of res.data.events ?? []) {
        if (event.type === 'response') {
          assistantContent += event.content ?? ''
        } else if (event.type === 'error') {
          errorMessage = event.message || 'AI 请求失败'
        }
      }

      if (errorMessage) toast.error(errorMessage)

      // 新会话由服务端创建，用返回的 sessionId 接管后续对话
      if (res.data.sessionId && res.data.sessionId !== requestSessionId) {
        setIsNewChat(false)
        setCurrentSessionId(res.data.sessionId)
      }

      if (assistantContent) {
        const assistantMessage: Message = {
          id: `a-${Date.now()}`,
          role: 'assistant',
          content: assistantContent,
          timestamp: Date.now()
        }
        setMessages(prev => [...prev, assistantMessage])
      } else if (!errorMessage) {
        toast.info('AI 没有返回内容')
      }

      await refreshSessions()
    } catch (error) {
      const detail = axios.isAxiosError(error)
        ? typeof error.response?.data === 'string'
          ? error.response.data
          : '请检查网络与 Provider 配置'
        : '发送失败'
      toast.error(`发送失败：${detail}`)
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (event: ReactKeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) return
    // 触屏设备保留回车换行，避免误发
    if (window.matchMedia('(pointer: coarse)').matches) return
    event.preventDefault()
    void handleSend()
  }

  return (
    <div className="chat-page">
      <div className="chat-top">
        <div className="chat-title-bar">
          <div className="chat-title">
            <h1>AI 对话</h1>
            <span>ReAct Agent</span>
          </div>
          <button className="btn btn-sm" onClick={handleNewChat}>
            <PlusIcon size={14} />
            新建
          </button>
        </div>

        <div className="provider-row">
          <span className="provider-label">模型</span>
          <div className="select-wrap">
            <select
              className="input select provider-select"
              value={selectedProviderId ?? ''}
              disabled={providers.length === 0}
              onChange={event => {
                const provider = providers.find(item => item.id === Number(event.target.value))
                if (provider) selectProvider(provider)
              }}
              aria-label="选择 AI Provider"
            >
              {providers.length === 0 ? (
                <option value="">未配置 Provider</option>
              ) : (
                providers.map(provider => (
                  <option key={provider.id} value={provider.id}>
                    {provider.name} · {provider.model}
                  </option>
                ))
              )}
            </select>
          </div>
        </div>

        {providers.length === 0 && (
          <div className="provider-hint">
            还没有可用的 AI Provider，请先到 <Link to="/settings/ai-providers">设置</Link> 添加。
          </div>
        )}

        {sessions.length > 0 && (
          <div className="session-strip">
            {sessions.map(session => (
              <button
                key={session.sessionId}
                className={`session-chip${
                  !isNewChat && session.sessionId === currentSessionId ? ' active' : ''
                }`}
                onClick={() => handleSelectSession(session.sessionId)}
              >
                {session.title || '未命名对话'}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="chat-messages" ref={messagesRef}>
        {messagesLoading && messages.length === 0 ? (
          <div className="loading-block">
            <Spinner size={18} />
            正在加载对话…
          </div>
        ) : messages.length === 0 && !loading ? (
          <div className="chat-empty">
            <div className="chat-empty-icon">
              <BotIcon size={28} />
            </div>
            <div className="chat-empty-title">有什么想看的，直接问我</div>
            <div className="chat-empty-desc">
              可以让我搜索影片、列出目录、整理文件，或者根据观看记录推荐内容。
            </div>
          </div>
        ) : (
          messages.map(message => (
            <div className={`msg ${message.role}`} key={message.id}>
              {message.role === 'assistant' && (
                <div className="msg-avatar">
                  <BotIcon size={15} />
                </div>
              )}
              <div className="msg-body">
                <div className="msg-bubble">{message.content}</div>
                <div className="msg-time">{formatChatTime(message.timestamp)}</div>
              </div>
            </div>
          ))
        )}

        {loading && (
          <div className="msg assistant">
            <div className="msg-avatar">
              <BotIcon size={15} />
            </div>
            <div className="msg-bubble">
              <span className="typing">
                <span />
                <span />
                <span />
              </span>
            </div>
          </div>
        )}
      </div>

      <div className="chat-composer">
        <textarea
          ref={inputRef}
          className="chat-input"
          rows={1}
          placeholder="输入消息…"
          value={input}
          onChange={event => handleInputChange(event.target.value)}
          onKeyDown={handleKeyDown}
          disabled={loading}
        />
        <button
          className="send-btn"
          onClick={() => void handleSend()}
          disabled={loading || !input.trim()}
          aria-label="发送"
        >
          {loading ? <Spinner size={16} /> : <SendIcon size={17} />}
        </button>
      </div>
    </div>
  )
}
