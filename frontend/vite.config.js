import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: true, // allow access from other machines on the network
    cors: true,
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('react') || id.includes('react-router-dom')) return 'vendor';
            if (id.includes('@tanstack')) return 'query';
            if (id.includes('axios')) return 'http';
            return 'vendor-other';
          }
        },
      },
    },
  },
});
