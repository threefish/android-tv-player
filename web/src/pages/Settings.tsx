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
  BotIcon,
  CheckIcon,
  ChevronRightIcon,
  EditIcon,
  EyeIcon,
  EyeOffIcon,
  FolderIcon,
  PlayIcon,
  PlusIcon,
  TrashIcon
} from '../components/Icons'

interface WebDavConfig {
  id?: number
  name: string
  url: string
  username: string
  password: string
  rootPath: string
  isEnabled?: boolean
}

const emptyConfig: WebDavConfig = {
  name: '',
  url: '',
  username: '',
  password: '',
  rootPath: '/'
}

export default function Settings() {
  const navigate = useNavigate()
  const [configs, setConfigs] = useState<WebDavConfig[]>([])
  const [loading, setLoading] = useState(true)
  const [sheetOpen, setSheetOpen] = useState(false)
  const [editing, setEditing] = useState<WebDavConfig | null>(null)
  const [form, setForm] = useState<WebDavConfig>(emptyConfig)
  const [saving, setSaving] = useState(false)
  const [testing, setTesting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [workingId, setWorkingId] = useState<number | null>(null)
  const [reminderEnabled, setReminderEnabled] = useState(false)
  const [reminderPlayMinutes, setReminderPlayMinutes] = useState('15')
  const [reminderRestMinutes, setReminderRestMinutes] = useState('5')
  const [reminderAutoResume, setReminderAutoResume] = useState(false)
  const [reminderSaving, setReminderSaving] = useState(false)

  useEffect(() => {
    axios.get<{ enabled: boolean; playMinutes: number; restMinutes: number; autoResume: boolean }>('/api/config/play-reminder')
      .then(res => {
        setReminderEnabled(res.data.enabled)
        setReminderPlayMinutes(String(res.data.playMinutes))
        setReminderRestMinutes(String(res.data.restMinutes))
        setReminderAutoResume(res.data.autoResume)
      })
      .catch(() => toast.error('加载播放提醒设置失败'))
  }, [])

  const handleSaveReminder = async () => {
    const playMinutes = Number(reminderPlayMinutes)
    const restMinutes = Number(reminderRestMinutes)
    if (!Number.isInteger(playMinutes) || playMinutes < 1 || playMinutes > 240) {
      toast.warning('连续播放时长需为 1-240 之间的整数分钟')
      return
    }
    if (!Number.isInteger(restMinutes) || restMinutes < 1 || restMinutes > 240) {
      toast.warning('休息时长需为 1-240 之间的整数分钟')
      return
    }
    setReminderSaving(true)
    try {
      await axios.put('/api/config/play-reminder', {
        enabled: reminderEnabled,
        playMinutes,
        restMinutes,
        autoResume: reminderAutoResume
      })
      toast.success('播放提醒已保存')
    } catch {
      toast.error('保存播放提醒失败')
    } finally {
      setReminderSaving(false)
    }
  }

  const loadConfigs = async () => {
    setLoading(true)
    try {
      const res = await axios.get<WebDavConfig[]>('/api/config/webdav/list')
      setConfigs(res.data)
    } catch {
      toast.error('加载 WebDAV 配置失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadConfigs()
  }, [])

  const updateField = (field: keyof WebDavConfig, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  const openCreate = () => {
    setEditing(null)
    setForm(emptyConfig)
    setShowPassword(false)
    setSheetOpen(true)
  }

  const openEdit = (config: WebDavConfig) => {
    setEditing(config)
    setForm({
      ...config,
      rootPath: config.rootPath || '/'
    })
    setShowPassword(false)
    setSheetOpen(true)
  }

  const closeSheet = () => {
    if (saving) return
    setSheetOpen(false)
    setEditing(null)
  }

  const validate = () => {
    if (!form.url.trim()) {
      toast.warning('请填写服务器地址')
      return false
    }
    if (!form.username.trim()) {
      toast.warning('请填写用户名')
      return false
    }
    if (!form.password) {
      toast.warning('请填写密码')
      return false
    }
    return true
  }

  const buildPayload = () => ({
    name: form.name.trim(),
    url: form.url.trim(),
    username: form.username.trim(),
    password: form.password,
    rootPath: form.rootPath.trim() || '/',
    isEnabled: editing?.isEnabled ?? true
  })

  const handleTest = async () => {
    if (!validate()) return
    setTesting(true)
    try {
      const res = await axios.post<{ success: boolean }>('/api/config/webdav/test', buildPayload())
      if (res.data.success) {
        toast.success('连接成功')
      } else {
        toast.error('连接失败，请检查地址与账号密码')
      }
    } catch {
      toast.error('连接测试失败')
    } finally {
      setTesting(false)
    }
  }

  const handleSave = async (event: FormEvent) => {
    event.preventDefault()
    if (!validate()) return

    setSaving(true)
    try {
      if (editing?.id) {
        await axios.put(`/api/config/webdav/${editing.id}`, buildPayload())
        toast.success('WebDAV 配置已更新')
      } else {
        await axios.post('/api/config/webdav', buildPayload())
        toast.success('WebDAV 配置已添加并启用')
      }
      setSheetOpen(false)
      setEditing(null)
      await loadConfigs()
    } catch {
      toast.error(editing ? '更新失败' : '添加失败')
    } finally {
      setSaving(false)
    }
  }

  const handleActivate = async (config: WebDavConfig) => {
    if (!config.id) return
    setWorkingId(config.id)
    try {
      await axios.post(`/api/config/webdav/${config.id}/activate`)
      toast.success('已切换 WebDAV 来源')
      await loadConfigs()
    } catch {
      toast.error('切换失败')
    } finally {
      setWorkingId(null)
    }
  }

  const handleDelete = async (config: WebDavConfig) => {
    if (!config.id) return
    const confirmed = await confirmDialog({
      title: '删除 WebDAV 来源',
      description: `确定要删除「${config.name.trim() || config.url}」吗？该操作无法撤销。`,
      confirmText: '删除',
      danger: true
    })
    if (!confirmed) return

    setWorkingId(config.id)
    try {
      await axios.delete(`/api/config/webdav/${config.id}`)
      toast.success('WebDAV 来源已删除')
      await loadConfigs()
    } catch {
      toast.error('删除失败')
    } finally {
      setWorkingId(null)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-heading-text">
          <h1 className="page-title">设置</h1>
          <div className="page-subtitle">WebDAV、AI 服务与播放提醒配置</div>
        </div>
      </div>

      <div className="settings-stack">
        <button className="nav-card" onClick={() => navigate('/settings/ai-providers')}>
          <span className="nav-card-icon">
            <BotIcon size={21} />
          </span>
          <span className="nav-card-main">
            <span className="nav-card-title">AI Provider</span>
            <span className="nav-card-desc">配置 OpenAI 兼容模型服务与默认模型</span>
          </span>
          <ChevronRightIcon size={18} className="file-arrow" />
        </button>

        <section className="card">
          <div className="card-head">
            <div>
              <div className="card-title">
                <PlayIcon size={17} />
                播放提醒
              </div>
              <div className="card-sub">连续播放达到设定时长后自动暂停进入休息，休息结束可手动确认或自动继续，休息屏显示今日累计观看分钟数</div>
            </div>
          </div>
          <div className="settings-card-body">
            <div className="form-stack">
              <div className="field">
                <label className="field-label" htmlFor="reminder-enabled">
                  <input
                    id="reminder-enabled"
                    type="checkbox"
                    checked={reminderEnabled}
                    onChange={event => setReminderEnabled(event.target.checked)}
                    style={{ marginRight: 8 }}
                  />
                  启用播放提醒
                </label>
              </div>
              <div className="field">
                <label className="field-label" htmlFor="reminder-play-minutes">
                  连续播放时长（分钟）
                </label>
                <input
                  id="reminder-play-minutes"
                  className="input"
                  type="number"
                  min={1}
                  max={240}
                  step={1}
                  value={reminderPlayMinutes}
                  disabled={!reminderEnabled}
                  onChange={event => setReminderPlayMinutes(event.target.value)}
                  style={{ maxWidth: 200 }}
                />
                <span className="field-hint">例如 15 表示每连续播放 15 分钟自动暂停一次，继续播放后重新计时</span>
              </div>
              <div className="field">
                <label className="field-label" htmlFor="reminder-rest-minutes">
                  休息时长（分钟）
                </label>
                <input
                  id="reminder-rest-minutes"
                  className="input"
                  type="number"
                  min={1}
                  max={240}
                  step={1}
                  value={reminderRestMinutes}
                  disabled={!reminderEnabled}
                  onChange={event => setReminderRestMinutes(event.target.value)}
                  style={{ maxWidth: 200 }}
                />
                <span className="field-hint">休息屏倒计时时长，倒计时结束前不能继续播放，退出应用或切换视频也不会绕过休息</span>
              </div>
              <div className="field">
                <label className="field-label">休息结束后</label>
                <label className="field-label" htmlFor="reminder-manual">
                  <input
                    id="reminder-manual"
                    type="radio"
                    name="reminder-resume-mode"
                    checked={!reminderAutoResume}
                    disabled={!reminderEnabled}
                    onChange={() => setReminderAutoResume(false)}
                    style={{ marginRight: 8 }}
                  />
                  手动确认继续（电视上按 OK）
                </label>
                <label className="field-label" htmlFor="reminder-auto">
                  <input
                    id="reminder-auto"
                    type="radio"
                    name="reminder-resume-mode"
                    checked={reminderAutoResume}
                    disabled={!reminderEnabled}
                    onChange={() => setReminderAutoResume(true)}
                    style={{ marginRight: 8 }}
                  />
                  自动继续播放
                </label>
                <span className="field-hint">手动模式倒计时结束后停在休息屏等待确认；自动模式倒计时结束直接继续播放</span>
              </div>
              <div>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => void handleSaveReminder()}
                  disabled={reminderSaving}
                >
                  {reminderSaving && <Spinner size={14} />}
                  {reminderSaving ? '保存中…' : '保存'}
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className="card">
          <div className="card-head">
            <div>
              <div className="card-title">
                <FolderIcon size={17} />
                WebDAV 来源
              </div>
              <div className="card-sub">可保存多个媒体库，同一时间使用一个</div>
            </div>
            <button className="btn btn-primary btn-sm" onClick={openCreate}>
              <PlusIcon size={14} />
              新增
            </button>
          </div>

          <div className="settings-card-body">
            {loading ? (
              <div className="loading-block">
                <Spinner size={18} />
                正在加载配置…
              </div>
            ) : configs.length === 0 ? (
              <EmptyState
                icon={<FolderIcon size={28} />}
                title="还没有 WebDAV 来源"
                description="添加 NAS、Alist 或其它 WebDAV 服务作为媒体库"
                action={
                  <button className="btn btn-primary" onClick={openCreate}>
                    <PlusIcon size={15} />
                    添加来源
                  </button>
                }
              />
            ) : (
              <div className="webdav-list">
                {configs.map(config => (
                  <div
                    className={`webdav-card${config.isEnabled ? ' active' : ''}`}
                    key={config.id ?? config.url}
                  >
                    <div className="provider-top">
                      <div className="provider-name">
                        {config.name.trim() || config.url}
                        {config.isEnabled && <span className="provider-badge">使用中</span>}
                      </div>
                    </div>
                    <div className="provider-meta">
                      <div className="provider-meta-row">
                        <b>地址</b> {config.url}
                      </div>
                      <div className="provider-meta-row">
                        <b>根路径</b> {config.rootPath || '/'}
                      </div>
                    </div>
                    <div className="provider-actions">
                      {!config.isEnabled && (
                        <button
                          className="btn btn-sm"
                          onClick={() => void handleActivate(config)}
                          disabled={workingId === config.id}
                        >
                          {workingId === config.id ? <Spinner size={13} /> : <CheckIcon size={14} />}
                          启用
                        </button>
                      )}
                      <button className="btn btn-sm" onClick={() => openEdit(config)}>
                        <EditIcon size={14} />
                        编辑
                      </button>
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={() => void handleDelete(config)}
                        disabled={workingId === config.id}
                      >
                        <TrashIcon size={14} />
                        删除
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      </div>

      <Sheet
        open={sheetOpen}
        title={editing ? '编辑 WebDAV 来源' : '新增 WebDAV 来源'}
        onClose={closeSheet}
        footer={
          <>
            <button className="btn" type="button" onClick={() => void handleTest()} disabled={testing || saving}>
              {testing && <Spinner size={14} />}
              {testing ? '测试中…' : '测试连接'}
            </button>
            <button className="btn" type="button" onClick={closeSheet} disabled={saving}>
              取消
            </button>
            <button
              className="btn btn-primary"
              type="submit"
              form="webdav-form"
              disabled={saving || testing}
            >
              {saving && <Spinner size={14} />}
              {saving ? '保存中…' : '保存'}
            </button>
          </>
        }
      >
        <form
          id="webdav-form"
          className="form-stack"
          autoComplete="off"
          onSubmit={event => void handleSave(event)}
        >
          <div className="field">
            <label className="field-label" htmlFor="webdav-name">
              名称
            </label>
            <input
              id="webdav-name"
              className="input"
              value={form.name}
              placeholder="例如：家庭 NAS、Alist"
              onChange={event => updateField('name', event.target.value)}
            />
            <span className="field-hint">留空时使用服务器地址作为名称</span>
          </div>

          <div className="field">
            <label className="field-label" htmlFor="webdav-url">
              服务器地址
            </label>
            <input
              id="webdav-url"
              className="input"
              value={form.url}
              inputMode="url"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              placeholder="http://192.168.1.100:5005"
              onChange={event => updateField('url', event.target.value)}
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="webdav-username">
              用户名
            </label>
            <input
              id="webdav-username"
              className="input"
              value={form.username}
              autoComplete="off"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              placeholder="WebDAV 用户名"
              onChange={event => updateField('username', event.target.value)}
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="webdav-password">
              密码
            </label>
            <span className="input-affix">
              <input
                id="webdav-password"
                className="input"
                type={showPassword ? 'text' : 'password'}
                value={form.password}
                autoComplete="new-password"
                autoCapitalize="none"
                autoCorrect="off"
                spellCheck={false}
                placeholder="WebDAV 密码"
                onChange={event => updateField('password', event.target.value)}
              />
              <button
                className="affix-btn"
                type="button"
                onClick={() => setShowPassword(value => !value)}
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
              >
                {showPassword ? <EyeOffIcon size={17} /> : <EyeIcon size={17} />}
              </button>
            </span>
          </div>

          <div className="field">
            <label className="field-label" htmlFor="webdav-root">
              根路径
            </label>
            <input
              id="webdav-root"
              className="input"
              value={form.rootPath}
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              placeholder="/"
              onChange={event => updateField('rootPath', event.target.value)}
            />
            <span className="field-hint">留空或填写 / 表示媒体库根目录</span>
          </div>
        </form>
      </Sheet>
    </div>
  )
}
