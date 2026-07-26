import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // 백엔드와 동일하게 레포 루트의 .env 하나만 쓴다(VITE_ 접두사 붙은 값만 클라이언트에 노출됨).
  envDir: path.resolve(__dirname, '..'),
})
