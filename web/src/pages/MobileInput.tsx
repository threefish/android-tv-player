import { useEffect, useState } from 'react'
import type { KeyboardEvent as ReactKeyboardEvent } from 'react'
import { useParams } from 'react-router-dom'
import axios from 'axios'
import Spinner from '../components/Spinner'
import { toast } from '../components/Toaster'
import {
  AlertCircleIcon,
  CheckCircleIcon,
  SendIcon,
  TvIcon
} from '../components/Icons'

export default function MobileInput() {
  const { token } = useParams<{ token: string }>()
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState('')

  const validateToken = async () => {
    if (!token) {
      setLoading(false)
      setError('二维码无效，请在 TV 端重新生成')
      return
    }
    setLoading(true)
    setError('')
    try {
      await axios.get(`/api/input/${token}`)
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setError('二维码已过期或无效，请在 TV 端重新生成')
      } else {
        setError('连接失败，请确认手机与 TV 处在同一局域网')
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void validateToken()
  }, [token])

  const handleSubmit = async () => {
    if (!input.trim()) {
      toast.warning('请输入要发送的内容')
      return
    }
    setSubmitting(true)
    try {
      await axios.post(`/api/input/${token}/submit`, { input: input.trim() })
      setSubmitted(true)
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setError('二维码已过期，请在 TV 端重新生成')
      } else {
        toast.error('发送失败，请重试')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const handleKeyDown = (event: ReactKeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault()
      void handleSubmit()
    }
  }

  return (
    <div className="solo-page">
      <div className="solo-brand">
        <span className="brand-mark">
          <TvIcon size={21} />
        </span>
        <span className="solo-brand-name">棒棒TV</span>
      </div>

      <div className="solo-content">
        {loading ? (
          <div className="loading-block">
            <Spinner size={20} />
            正在连接 TV…
          </div>
        ) : error ? (
          <div className="result-block">
            <div className="result-icon error">
              <AlertCircleIcon size={30} />
            </div>
            <div className="result-title">无法访问</div>
            <div className="result-desc">{error}</div>
            <div className="result-action">
              <button className="btn btn-block" onClick={() => void validateToken()}>
                重新验证
              </button>
            </div>
          </div>
        ) : submitted ? (
          <div className="result-block">
            <div className="result-icon success">
              <CheckCircleIcon size={32} />
            </div>
            <div className="result-title">发送成功</div>
            <div className="result-desc">内容已发送到 TV，请查看电视屏幕</div>
            <div className="result-action">
              <button
                className="btn btn-block"
                onClick={() => {
                  setInput('')
                  setSubmitted(false)
                }}
              >
                再发一条
              </button>
            </div>
          </div>
        ) : (
          <div className="solo-card">
            <h1 className="solo-title">发送到 TV</h1>
            <p className="solo-sub">输入文字或链接，发送后 TV 端会立即收到</p>

            <div className="solo-form">
              <textarea
                className="textarea"
                rows={6}
                value={input}
                placeholder="输入要发送到 TV 的内容…"
                onChange={event => setInput(event.target.value)}
                onKeyDown={handleKeyDown}
              />
              <div className="form-actions">
                <button
                  className="btn btn-primary btn-block"
                  onClick={() => void handleSubmit()}
                  disabled={submitting}
                >
                  {submitting ? <Spinner size={15} /> : <SendIcon size={16} />}
                  {submitting ? '发送中…' : '发送到 TV'}
                </button>
              </div>
              <div className="solo-hint">按 Ctrl / ⌘ + Enter 快速发送</div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
