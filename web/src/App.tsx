import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import FileBrowser from './pages/FileBrowser'
import Chat from './pages/Chat'
import Settings from './pages/Settings'
import Trash from './pages/Trash'
import AiProviderSettings from './pages/AiProviderSettings'
import MobileInput from './pages/MobileInput'
import Layout from './components/Layout'
import { Toaster } from './components/Toaster'
import { ConfirmHost } from './components/ConfirmHost'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/files" replace />} />
          <Route path="files" element={<FileBrowser />} />
          <Route path="trash" element={<Trash />} />
          <Route path="chat" element={<Chat />} />
          <Route path="settings" element={<Settings />} />
          <Route path="settings/ai-providers" element={<AiProviderSettings />} />
        </Route>
        <Route path="/input/:token" element={<MobileInput />} />
        <Route path="*" element={<Navigate to="/files" replace />} />
      </Routes>
      <Toaster />
      <ConfirmHost />
    </BrowserRouter>
  )
}

export default App
