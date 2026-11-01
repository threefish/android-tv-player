import { useEffect, useState } from 'react'
import axios from 'axios'
import EmptyState from '../components/EmptyState'
import FileTile from '../components/FileTile'
import Spinner from '../components/Spinner'
import { confirmDialog } from '../components/ConfirmHost'
import { toast } from '../components/Toaster'
import { RefreshIcon, TrashIcon, UndoIcon } from '../components/Icons'
import { formatDateTime, formatFileSize } from '../utils/format'

interface TrashFile {
  id: number
  path: string
  name: string
  isDirectory: boolean
  size: number
  modifiedTime: number
}

export default function Trash() {
  const [files, setFiles] = useState<TrashFile[]>([])
  const [loading, setLoading] = useState(true)
  const [restoring, setRestoring] = useState<string | null>(null)

  const loadTrash = async () => {
    setLoading(true)
    try {
      const res = await axios.get<TrashFile[]>('/api/trash')
      setFiles(res.data)
    } catch {
      toast.error('加载垃圾箱失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadTrash()
  }, [])

  const handleRestore = async (file: TrashFile) => {
    const confirmed = await confirmDialog({
      title: '恢复文件',
      description: `确定将「${file.name}」恢复到原目录吗？`,
      confirmText: '恢复'
    })
    if (!confirmed) return

    setRestoring(file.path)
    try {
      await axios.post(`/api/files/${encodeURIComponent(file.path)}/restore`)
      toast.success('文件已恢复')
      await loadTrash()
    } catch {
      toast.error('恢复失败')
    } finally {
      setRestoring(null)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-heading-text">
          <h1 className="page-title">垃圾箱</h1>
          <div className="page-subtitle">软删除文件不会从 WebDAV 中移除</div>
        </div>
        <div className="header-actions">
          <button
            className="btn btn-icon"
            onClick={() => void loadTrash()}
            disabled={loading}
            aria-label="刷新垃圾箱"
          >
            <RefreshIcon size={17} className={loading ? 'icon-spin' : undefined} />
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loading-block">
          <Spinner size={18} />
          正在加载垃圾箱…
        </div>
      ) : files.length === 0 ? (
        <EmptyState
          icon={<TrashIcon size={28} />}
          title="垃圾箱为空"
          description="在文件浏览或 AI 对话中删除的文件会出现在这里，可随时恢复"
        />
      ) : (
        <div className="trash-list">
          {files.map(file => (
            <div className="trash-card" key={file.path}>
              <div className="trash-head">
                <FileTile name={file.name} isDirectory={file.isDirectory} />
                <div className="trash-main">
                  <div className="trash-name">{file.name}</div>
                  <div className="trash-path">{file.path}</div>
                </div>
              </div>
              <div className="trash-meta">
                <span>{file.isDirectory ? '文件夹' : formatFileSize(file.size)}</span>
                <span>修改于 {formatDateTime(file.modifiedTime)}</span>
              </div>
              <div className="trash-actions">
                <button
                  className="btn btn-sm"
                  onClick={() => void handleRestore(file)}
                  disabled={restoring === file.path}
                >
                  {restoring === file.path ? <Spinner size={13} /> : <UndoIcon size={14} />}
                  恢复
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
