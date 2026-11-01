import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'

export interface ConfirmOptions {
  title: string
  description?: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
}

type ConfirmResolver = (value: boolean) => void
type ConfirmState = { options: ConfirmOptions; resolve: ConfirmResolver } | null
type ConfirmOpener = (options: ConfirmOptions) => Promise<boolean>

let openConfirm: ConfirmOpener | null = null

export function confirmDialog(options: ConfirmOptions): Promise<boolean> {
  if (openConfirm) {
    return openConfirm(options)
  }
  // 极端情况下（宿主未挂载）退化为原生确认框
  return Promise.resolve(
    window.confirm(`${options.title}${options.description ? `\n\n${options.description}` : ''}`)
  )
}

export function ConfirmHost() {
  const [state, setState] = useState<ConfirmState>(null)

  useEffect(() => {
    openConfirm = options =>
      new Promise<boolean>(resolve => {
        setState({ options, resolve })
      })
    return () => {
      openConfirm = null
    }
  }, [])

  const close = (value: boolean) => {
    if (!state) return
    state.resolve(value)
    setState(null)
  }

  useEffect(() => {
    if (!state) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') close(false)
      if (event.key === 'Enter') close(true)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  })

  if (!state) return null

  const { options } = state

  return createPortal(
    <div className="overlay overlay-center" onClick={() => close(false)}>
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-label={options.title}
        onClick={event => event.stopPropagation()}
      >
        <h3 className="dialog-title">{options.title}</h3>
        {options.description && <p className="dialog-desc">{options.description}</p>}
        <div className="dialog-actions">
          <button className="btn" type="button" onClick={() => close(false)}>
            {options.cancelText ?? '取消'}
          </button>
          <button
            className={`btn ${options.danger ? 'btn-danger-solid' : 'btn-primary'}`}
            type="button"
            onClick={() => close(true)}
          >
            {options.confirmText ?? '确定'}
          </button>
        </div>
      </div>
    </div>,
    document.body
  )
}
