import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import EmptyState from '../components/EmptyState'
import Sheet from '../components/Sheet'
import Spinner from '../components/Spinner'
import { confirmDialog } from '../components/ConfirmHost'
import { toast } from '../components/Toaster'
import {
  ArrowLeftIcon,
  BotIcon,
  CheckIcon,
  EditIcon,
  PlusIcon,
  TrashIcon
} from '../components/Icons'

interface AiProvider {
  id: number
  name: string
  baseUrl: string
  apiKey: string
  model: string
  isActive: boolean
}

interface ProviderForm {
  name: string
  baseUrl: string
  apiKey: string
  model: string
}

const emptyForm: ProviderForm = {
  name: '',
  baseUrl: '',
  apiKey: '',
  model: ''
}

export default function AiProviderSettings() {
  const navigate = useNavigate()
  const [providers, setProviders] = useState<AiProvider[]>([])
  const [loading, setLoading] = useState(true)
  const [sheetOpen, setSheetOpen] = useState(false)
  const [editing, setEditing] = useState<AiProvider | null>(null)
  const [form, setForm] = useState<ProviderForm>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [workingId, setWorkingId] = useState<number | null>(null)

  const loadProviders = async () => {
    setLoading(true)
    try {
      const res = await axios.get<AiProvider[]>('/api/config/ai-providers')
      setProviders(res.data)
    } catch {
      toast.error('加载 AI Provider 列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadProviders()
  }, [])

  const updateField = (field: keyof ProviderForm, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  const openCreate = () => {
    setEditing(null)
    setForm(emptyForm)
    setSheetOpen(true)
  }

  const openEdit = (provider: AiProvider) => {
    setEditing(provider)
    setForm({
      name: provider.name,
      baseUrl: provider.baseUrl,
      apiKey: provider.apiKey,
      model: provider.model
    })
    setSheetOpen(true)
  }

  const closeSheet = () => {
    if (saving) return
    setSheetOpen(false)
    setEditing(null)
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!form.name.trim() || !form.baseUrl.trim() || !form.apiKey.trim() || !form.model.trim()) {
      toast.warning('请填写完整的 Provider 信息')
      return
    }

    setSaving(true)
    try {
      const payload = {
        ...form,
        name: form.name.trim(),
        baseUrl: form.baseUrl.trim(),
        apiKey: form.apiKey.trim(),
        model: form.model.trim(),
        // 编辑时保留原有活动状态，避免保存后把当前 Provider 取消激活
        isActive: editing?.isActive ?? false
      }
      if (editing) {
        await axios.put(`/api/config/ai-providers/${editing.id}`, payload)
        toast.success('Provider 已更新')
      } else {
        await axios.post('/api/config/ai-providers', payload)
        toast.success('Provider 已添加')
      }
      setSheetOpen(false)
      setEditing(null)
      await loadProviders()
    } catch {
      toast.error(editing ? '更新失败' : '添加失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (provider: AiProvider) => {
    const confirmed = await confirmDialog({
      title: '删除 Provider',
      description: `确定要删除「${provider.name}」吗？该操作无法撤销。`,
      confirmText: '删除',
      danger: true
    })
    if (!confirmed) return

    setWorkingId(provider.id)
    try {
      await axios.delete(`/api/config/ai-providers/${provider.id}`)
      toast.success('Provider 已删除')
      await loadProviders()
    } catch {
      toast.error('删除失败')
    } finally {
      setWorkingId(null)
    }
  }

  const handleActivate = async (provider: AiProvider) => {
    setWorkingId(provider.id)
    try {
      await axios.post(`/api/config/ai-providers/${provider.id}/activate`)
      toast.success('已设为活动 Provider')
      await loadProviders()
    } catch {
      toast.error('设置失败')
    } finally {
      setWorkingId(null)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-heading">
          <button
            className="btn btn-icon btn-ghost"
            onClick={() => navigate('/settings')}
            aria-label="返回设置"
          >
            <ArrowLeftIcon size={19} />
          </button>
          <div className="page-heading-text">
            <h1 className="page-title">AI Provider</h1>
            <div className="page-subtitle">OpenAI 兼容接口</div>
          </div>
        </div>
        <button className="btn btn-primary btn-sm" onClick={openCreate}>
          <PlusIcon size={14} />
          添加
        </button>
      </div>

      {loading ? (
        <div className="loading-block">
          <Spinner size={18} />
          正在加载 Provider…
        </div>
      ) : providers.length === 0 ? (
        <EmptyState
          icon={<BotIcon size={28} />}
          title="还没有配置 AI Provider"
          description="添加一个 OpenAI 兼容服务，即可在 TV 和网页端使用 AI 对话"
          action={
            <button className="btn btn-primary" onClick={openCreate}>
              <PlusIcon size={15} />
              添加 Provider
            </button>
          }
        />
      ) : (
        <div className="provider-list">
          {providers.map(provider => (
            <div
              className={`provider-card${provider.isActive ? ' active' : ''}`}
              key={provider.id}
            >
              <div className="provider-top">
                <div className="provider-name">
                  {provider.name}
                  {provider.isActive && <span className="provider-badge">活动中</span>}
                </div>
              </div>
              <div className="provider-meta">
                <div className="provider-meta-row">
                  <b>模型</b> {provider.model}
                </div>
                <div className="provider-meta-row">
                  <b>接口</b> {provider.baseUrl}
                </div>
                <div className="provider-meta-row">
                  <b>密钥</b> ••••••••••
                </div>
              </div>
              <div className="provider-actions">
                {!provider.isActive && (
                  <button
                    className="btn btn-sm"
                    onClick={() => void handleActivate(provider)}
                    disabled={workingId === provider.id}
                  >
                    {workingId === provider.id ? <Spinner size={13} /> : <CheckIcon size={14} />}
                    启用
                  </button>
                )}
                <button className="btn btn-sm" onClick={() => openEdit(provider)}>
                  <EditIcon size={14} />
                  编辑
                </button>
                <button
                  className="btn btn-sm btn-danger"
                  onClick={() => void handleDelete(provider)}
                  disabled={workingId === provider.id}
                >
                  <TrashIcon size={14} />
                  删除
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Sheet
        open={sheetOpen}
        title={editing ? '编辑 Provider' : '添加 Provider'}
        onClose={closeSheet}
        footer={
          <>
            <button className="btn" type="button" onClick={closeSheet} disabled={saving}>
              取消
            </button>
            <button
              className="btn btn-primary"
              type="submit"
              form="provider-form"
              disabled={saving}
            >
              {saving && <Spinner size={14} />}
              {saving ? '保存中…' : '保存'}
            </button>
          </>
        }
      >
        <form id="provider-form" className="form-stack" onSubmit={event => void handleSubmit(event)}>
          <div className="field">
            <label className="field-label" htmlFor="provider-name">
              名称
            </label>
            <input
              id="provider-name"
              className="input"
              value={form.name}
              placeholder="例如：OpenAI、DeepSeek、本地模型"
              onChange={event => updateField('name', event.target.value)}
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="provider-base-url">
              API Base URL
            </label>
            <input
              id="provider-base-url"
              className="input"
              value={form.baseUrl}
              inputMode="url"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              placeholder="https://api.openai.com/v1"
              onChange={event => updateField('baseUrl', event.target.value)}
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="provider-api-key">
              API Key
            </label>
            <input
              id="provider-api-key"
              className="input"
              type="password"
              value={form.apiKey}
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              placeholder="输入 API Key"
              onChange={event => updateField('apiKey', event.target.value)}
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="provider-model">
              模型
            </label>
            <input
              id="provider-model"
              className="input"
              value={form.model}
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              placeholder="例如：gpt-4o-mini、qwen-plus"
              onChange={event => updateField('model', event.target.value)}
            />
            <span className="field-hint">需要与所填接口支持的模型名称一致</span>
          </div>
        </form>
      </Sheet>
    </div>
  )
}
