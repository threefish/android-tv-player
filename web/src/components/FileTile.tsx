import { FileIcon, FilmIcon, FolderIcon, ImageIcon, MusicIcon } from './Icons'

type FileKind = 'folder' | 'video' | 'audio' | 'image' | 'other'

const videoExts = new Set(['mp4', 'mkv', 'avi', 'mov', 'wmv', 'flv', 'webm', 'ts', 'm2ts', 'rmvb', 'mpg', 'mpeg'])
const audioExts = new Set(['mp3', 'flac', 'wav', 'aac', 'm4a', 'ogg', 'wma', 'ape'])
const imageExts = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'])

export function getFileKind(name: string, isDirectory: boolean): FileKind {
  if (isDirectory) return 'folder'
  const ext = name.includes('.') ? name.split('.').pop()?.toLowerCase() ?? '' : ''
  if (videoExts.has(ext)) return 'video'
  if (audioExts.has(ext)) return 'audio'
  if (imageExts.has(ext)) return 'image'
  return 'other'
}

interface FileTileProps {
  name: string
  isDirectory: boolean
  size?: number
}

export default function FileTile({ name, isDirectory, size = 21 }: FileTileProps) {
  const kind = getFileKind(name, isDirectory)
  const icon = {
    folder: <FolderIcon size={size} />,
    video: <FilmIcon size={size} />,
    audio: <MusicIcon size={size} />,
    image: <ImageIcon size={size} />,
    other: <FileIcon size={size} />
  }[kind]

  return <span className={`file-tile file-tile-${kind}`}>{icon}</span>
}
