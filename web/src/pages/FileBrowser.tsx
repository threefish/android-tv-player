import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { fileApi } from '../api'
import type { FileInfo } from '../api'
import EmptyState from '../components/EmptyState'
import FileTile, { getFileKind } from '../components/FileTile'
import Spinner from '../components/Spinner'
import { toast } from '../components/Toaster'
import {
  AlertCircleIcon,
  ArrowLeftIcon,
  CheckCircleIcon,
  ChevronRightIcon,
  FolderIcon,
  PlayIcon,
  RefreshIcon,
  SyncIcon
} from '../components/Icons'
import { formatDateTime, formatFileSize } from '../utils/format'

interface SyncStatus {
  status: 'idle' | 'syncing' | 'completed' | 'error'
  current?: number
  total?: number
  path?: string
  fileCount?: number
  duration?: number
  failedCount?: number
  message?: string
}

export default function FileBrowser() {
  const [currentPath, setCurrentPath] = useState('/')
  const [files, setFiles] = useState<FileInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const [syncStatus, setSyncStatus] = useState<SyncStatus | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const lastSyncStatus = useRef('')

  useEffect(() => {
    let alive = true

    const loadFiles = async () => {
      setLoading(true)
      try {
        const res = await fileApi.list(currentPath)
        if (alive) setFiles(res.data)
      } catch {
        if (alive) toast.error('加载文件失败')
      } finally {
        if (alive) setLoading(false)
      }
    }

    void loadFiles()
    return () => {
      alive = false
    }
  }, [currentPath, reloadKey])

  useEffect(() => {
    let alive = true

    const check = async () => {
      try {
        const res = await axios.get<SyncStatus>('/api/sync/status')
        if (!alive) return
        setSyncStatus(res.data)
        setSyncing(res.data.status === 'syncing')
      } catch {
        // 同步状态接口不可用时静默处理
      }
    }

    void check()
    const timer = window.setInterval(check, 1000)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [])

  // 同步完成后自动刷新当前目录
  useEffect(() => {
    if (!syncStatus) return
    if (lastSyncStatus.current === 'syncing' && syncStatus.status === 'completed') {
      setReloadKey(key => key + 1)
      const failed = syncStatus.failedCount ?? 0
      if (failed > 0) {
        toast.warning(`同步完成，${failed} 项失败，可再次同步补齐`)
      } else {
        toast.success(`同步完成，共 ${syncStatus.fileCount ?? 0} 项`)
      }
    }
    lastSyncStatus.current = syncStatus.status
  }, [syncStatus])

  const loadFiles = async () => {
    setLoading(true)
    try {
      const res = await fileApi.list(currentPath)
      setFiles(res.data)
    } catch {
      toast.error('加载文件失败')
    } finally {
      setLoading(false)
    }
  }

  const handleSync = async () => {
    setSyncing(true)
    try {
      await axios.post('/api/sync/trigger')
      toast.success('已开始同步文件索引')
    } catch {
      toast.error('同步触发失败')
      setSyncing(false)
    }
  }

  const handleFileClick = (file: FileInfo) => {
    if (file.isDirectory) {
      setCurrentPath(file.path)
    } else {
      toast.info(`播放：${file.name}`)
    }
  }

  const handleBack = () => {
    if (currentPath === '/') return
    const parent = currentPath.substring(0, currentPath.lastIndexOf('/')) || '/'
    setCurrentPath(parent)
  }

  const segments = currentPath.split('/').filter(Boolean)
  const percent =
    syncStatus?.status === 'syncing' && syncStatus.total
      ? Math.min(100, Math.round(((syncStatus.current ?? 0) / syncStatus.total) * 100))
      : 0

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-heading">
          {currentPath !== '/' && (
            <button className="btn btn-icon btn-ghost" onClick={handleBack} aria-label="返回上级">
              <ArrowLeftIcon size={19} />
            </button>
          )}
          <div className="page-heading-text">
            <h1 className="page-title">文件浏览</h1>
            <div className="page-subtitle">{currentPath === '/' ? 'WebDAV 根目录' : currentPath}</div>
          </div>
        </div>
        <div className="header-actions">
          <button
            className="btn btn-icon"
            onClick={() => void loadFiles()}
            disabled={loading}
            aria-label="刷新文件列表"
          >
            <RefreshIcon size={17} className={loading ? 'icon-spin' : undefined} />
          </button>
          <button
            className="btn btn-primary btn-sm"
            onClick={() => void handleSync()}
            disabled={syncing}
          >
            {syncing ? <Spinner size={14} /> : <SyncIcon size={15} />}
            {syncing ? '同步中' : '同步'}
          </button>
        </div>
      </div>

      {syncStatus?.status === 'syncing' && (
        <div className="sync-banner">
          <div className="sync-info">
            <span className="sync-text">
              <Spinner size={14} />
              正在同步文件索引
            </span>
            <span className="muted">{percent}%</span>
          </div>
          <div className="progress">
            <div className="progress-bar" style={{ width: `${percent}%` }} />
          </div>
          {syncStatus.path && <div className="sync-note">{syncStatus.path}</div>}
        </div>
      )}

      {syncStatus?.status === 'completed' && (
        <div className={`sync-banner ${syncStatus.failedCount ? 'warn' : 'ok'}`}>
          <div className="sync-info">
            <span className="sync-text">
              {syncStatus.failedCount ? (
                <AlertCircleIcon size={15} />
              ) : (
                <CheckCircleIcon size={15} />
              )}
              同步完成
            </span>
            <span className="muted">{syncStatus.fileCount ?? 0} 个文件</span>
          </div>
          {!!syncStatus.duration && (
            <div className="sync-note">耗时 {(syncStatus.duration / 1000).toFixed(1)} 秒</div>
          )}
          {!!syncStatus.failedCount && (
            <div className="sync-note warn">
              有 {syncStatus.failedCount} 项未能同步，再次同步会重试
            </div>
          )}
        </div>
      )}

      {syncStatus?.status === 'error' && (
        <div className="sync-banner err">
          <div className="sync-info">
            <span className="sync-text">
              <AlertCircleIcon size={15} />
              同步失败
            </span>
          </div>
          <div className="sync-note">{syncStatus.message || '请检查 WebDAV 配置后重试'}</div>
        </div>
      )}

      <div className="crumbs">
        <button
          className={`crumb${currentPath === '/' ? ' current' : ''}`}
          onClick={() => setCurrentPath('/')}
        >
          根目录
        </button>
        {segments.map((segment, index) => {
          const path = '/' + segments.slice(0, index + 1).join('/')
          return (
            <span key={path} style={{ display: 'contents' }}>
              <span className="crumb-sep">/</span>
              <button
                className={`crumb${index === segments.length - 1 ? ' current' : ''}`}
                onClick={() => setCurrentPath(path)}
              >
                {segment}
              </button>
            </span>
          )
        })}
      </div>

      <div className="list-summary">
        <span>{loading ? '加载中…' : `共 ${files.length} 项`}</span>
        <span>点按文件夹进入</span>
      </div>

      {loading ? (
        <div className="file-list">
          {Array.from({ length: 6 }).map((_, index) => (
            <div className="skeleton-row" key={index}>
              <div className="skel skel-tile" />
              <div className="skel-lines">
                <div className="skel skel-line" style={{ width: `${62 + (index % 3) * 10}%` }} />
                <div className="skel skel-line skel-line-short" />
              </div>
            </div>
          ))}
        </div>
      ) : files.length === 0 ? (
        <EmptyState
          icon={<FolderIcon size={28} />}
          title="此文件夹为空"
          description="如果刚配置好 WebDAV，可以点击右上角「同步」拉取文件索引"
        />
      ) : (
        <div className="file-list">
          {files.map(file => (
            <button className="file-row" key={file.path} onClick={() => handleFileClick(file)}>
              <FileTile name={file.name} isDirectory={file.isDirectory} />
              <div className="file-main">
                <div className="file-name">{file.name}</div>
                <div className="file-meta">
                  <span>{file.isDirectory ? '文件夹' : formatFileSize(file.size)}</span>
                  <span className="file-meta-dot">·</span>
                  <span>{formatDateTime(file.modifiedTime)}</span>
                </div>
              </div>
              {file.isDirectory ? (
                <ChevronRightIcon size={17} className="file-arrow" />
              ) : (
                (getFileKind(file.name, false) === 'video' ||
                  getFileKind(file.name, false) === 'audio') && (
                  <PlayIcon size={15} className="file-arrow" />
                )
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
