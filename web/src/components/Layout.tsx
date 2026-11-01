import { useEffect, useRef } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { ChatIcon, FolderIcon, SettingsIcon, TrashIcon, TvIcon } from './Icons'

const tabs = [
  { to: '/files', label: '文件', icon: FolderIcon },
  { to: '/trash', label: '垃圾箱', icon: TrashIcon },
  { to: '/chat', label: 'AI 对话', icon: ChatIcon },
  { to: '/settings', label: '设置', icon: SettingsIcon }
]

export default function Layout() {
  const location = useLocation()
  const mainRef = useRef<HTMLElement>(null)

  // 路由切换时回到内容顶部
  useEffect(() => {
    mainRef.current?.scrollTo({ top: 0 })
  }, [location.pathname])

  return (
    <div className="app-shell">
      <header className="app-bar">
        <div className="brand">
          <span className="brand-mark">
            <TvIcon size={19} />
          </span>
          <span className="brand-name">棒棒TV</span>
        </div>
        <span className="app-bar-note">Web 控制台</span>
      </header>

      <main className="app-main" ref={mainRef}>
        <Outlet />
      </main>

      <nav className="tabbar" aria-label="主导航">
        {tabs.map(tab => {
          const TabIcon = tab.icon
          return (
            <NavLink
              key={tab.to}
              to={tab.to}
              className={({ isActive }) => `tab${isActive ? ' active' : ''}`}
            >
              <span className="tab-icon">
                <TabIcon size={21} />
              </span>
              <span>{tab.label}</span>
            </NavLink>
          )
        })}
      </nav>
    </div>
  )
}
