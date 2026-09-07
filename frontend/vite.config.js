import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Frontend chodzi na porcie 5173, API na 8080. Zamiast konfigurować CORS
// po stronie Springa, serwer deweloperski Vite przekazuje żądania z /api
// do backendu — dla przeglądarki wszystko jest wtedy tym samym źródłem.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
