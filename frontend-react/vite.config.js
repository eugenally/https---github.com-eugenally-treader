import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'


// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // npm run build 실행시, outDir로 지정한 경로에 빌드된 결과물이 생성됨
  build: {
    outDir: fileURLToPath(
      new URL('../src/main/resources/static', import.meta.url)
    ),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  // 배포했을때 정적 파일 경로를 상대경로로 설정
  base: './',
})
