import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { AlertCircleIcon, CheckCircleIcon, InfoIcon, XCircleIcon } from './Icons'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

interface ToastItem {
  id: number
  type: ToastType
  text: string
}

let seq = 0
let items: ToastItem[] = []
const listeners = new Set<(next: ToastItem[]) => void>()

function publish() {
  const snapshot = [...items]
  listeners.forEach(listener => listener(snapshot))
}

function dismiss(id: number) {
  items = items.filter(item => item.id !== id)
  publish()
}

function push(type: ToastType, text: string, duration = 2600) {
  const id = ++seq
  // 最多同时展示 3 条，避免遮挡页面
  items = [...items.slice(-2), { id, type, text }]
  publish()
  window.setTimeout(() => dismiss(id), duration)
}

export const toast = {
  success: (text: string) => push('success', text),
  error: (text: string) => push('error', text, 3200),
  info: (text: string) => push('info', text),
  warning: (text: string) => push('warning', text, 3000)
}

const iconMap = {
  success: CheckCircleIcon,
  error: XCircleIcon,
  warning: AlertCircleIcon,
  info: InfoIcon
}

export function Toaster() {
  const [list, setList] = useState<ToastItem[]>(items)

  useEffect(() => {
    listeners.add(setList)
    setList([...items])
    return () => {
      listeners.delete(setList)
    }
  }, [])

  return createPortal(
    <div className="toasts" role="status" aria-live="polite">
      {list.map(item => {
        const ToastIcon = iconMap[item.type]
        return (
          <div className={`toast toast-${item.type}`} key={item.id}>
            <ToastIcon className="toast-icon" size={17} />
            <span>{item.text}</span>
          </div>
        )
      })}
    </div>,
    document.body
  )
}
