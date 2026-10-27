import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export default defineConfig({
  plugins: [
    react(),
    {
      name: 'copy-to-spring-resources',
      closeBundle() {
        try {
          const srcDir = path.resolve(__dirname, 'dist');
          const destDir = path.resolve(__dirname, '../src/main/resources/static/react-dist');
          if (fs.existsSync(srcDir)) {
            fs.mkdirSync(destDir, { recursive: true });
            fs.cpSync(srcDir, destDir, { recursive: true, force: true });
            console.log('Synchronized bundle to Spring Boot resources:', destDir);
          }
        } catch (e) {
          console.warn('Note: Spring Boot resource sync skipped (safe in isolated CI/Netlify builds):', e.message);
        }
      }
    }
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://localhost:8085',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
});
