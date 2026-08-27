// vite.config.ts
import { defineConfig } from "file:///D:/repos/CodingHub/frontend/node_modules/vite/dist/node/index.js";
import vue from "file:///D:/repos/CodingHub/frontend/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import { resolve } from "path";
var __vite_injected_original_dirname = "D:\\repos\\CodingHub\\frontend";
var backendPort = process.env.BACKEND_PORT || "8082";
var backendTarget = `http://localhost:${backendPort}`;
var ragPort = process.env.RAG_PORT || "8000";
var ragTarget = `http://localhost:${ragPort}`;
var vite_config_default = defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": resolve(__vite_injected_original_dirname, "src")
    }
  },
  server: {
    host: "0.0.0.0",
    port: 5173,
    proxy: {
      "/api/v1": {
        target: backendTarget,
        changeOrigin: true
      },
      "/api/forum": {
        target: backendTarget,
        changeOrigin: true
      },
      "/api/overview": {
        target: backendTarget,
        changeOrigin: true
      },
      "/rag": {
        target: ragTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/rag/, "")
      },
      "/ws": {
        target: backendTarget,
        changeOrigin: true,
        ws: true
      }
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJEOlxcXFxyZXBvc1xcXFxDb2RpbmdIdWJcXFxcZnJvbnRlbmRcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXHJlcG9zXFxcXENvZGluZ0h1YlxcXFxmcm9udGVuZFxcXFx2aXRlLmNvbmZpZy50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRDovcmVwb3MvQ29kaW5nSHViL2Zyb250ZW5kL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcclxuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXHJcbmltcG9ydCB7IHJlc29sdmUgfSBmcm9tICdwYXRoJ1xyXG5cclxuLy8gXHU1NDBFXHU3QUVGXHU3QUVGXHU1M0UzXHU1M0VGXHU5MDFBXHU4RkM3XHU3M0FGXHU1ODgzXHU1M0Q4XHU5MUNGIEJBQ0tFTkRfUE9SVCBcdTg5ODZcdTc2RDZcdUZGMENcdTlFRDhcdThCQTQgODA4MlxyXG5jb25zdCBiYWNrZW5kUG9ydCA9IHByb2Nlc3MuZW52LkJBQ0tFTkRfUE9SVCB8fCAnODA4MidcclxuY29uc3QgYmFja2VuZFRhcmdldCA9IGBodHRwOi8vbG9jYWxob3N0OiR7YmFja2VuZFBvcnR9YFxyXG5cclxuLy8gUkFHIFx1NjcwRFx1NTJBMVx1N0FFRlx1NTNFM1x1NTNFRlx1OTAxQVx1OEZDN1x1NzNBRlx1NTg4M1x1NTNEOFx1OTFDRiBSQUdfUE9SVCBcdTg5ODZcdTc2RDZcdUZGMENcdTlFRDhcdThCQTQgODAwMFxyXG5jb25zdCByYWdQb3J0ID0gcHJvY2Vzcy5lbnYuUkFHX1BPUlQgfHwgJzgwMDAnXHJcbmNvbnN0IHJhZ1RhcmdldCA9IGBodHRwOi8vbG9jYWxob3N0OiR7cmFnUG9ydH1gXHJcblxyXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoe1xyXG4gIHBsdWdpbnM6IFt2dWUoKV0sXHJcbiAgcmVzb2x2ZToge1xyXG4gICAgYWxpYXM6IHtcclxuICAgICAgJ0AnOiByZXNvbHZlKF9fZGlybmFtZSwgJ3NyYycpXHJcbiAgICB9XHJcbiAgfSxcclxuICBzZXJ2ZXI6IHtcclxuICAgIGhvc3Q6ICcwLjAuMC4wJyxcclxuICAgIHBvcnQ6IDUxNzMsXHJcbiAgICBwcm94eToge1xyXG4gICAgICAnL2FwaS92MSc6IHtcclxuICAgICAgICB0YXJnZXQ6IGJhY2tlbmRUYXJnZXQsXHJcbiAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlXHJcbiAgICAgIH0sXHJcbiAgICAgICcvYXBpL2ZvcnVtJzoge1xyXG4gICAgICAgIHRhcmdldDogYmFja2VuZFRhcmdldCxcclxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWVcclxuICAgICAgfSxcclxuICAgICAgJy9hcGkvb3ZlcnZpZXcnOiB7XHJcbiAgICAgICAgdGFyZ2V0OiBiYWNrZW5kVGFyZ2V0LFxyXG4gICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZVxyXG4gICAgICB9LFxyXG4gICAgICAnL3JhZyc6IHtcclxuICAgICAgICB0YXJnZXQ6IHJhZ1RhcmdldCxcclxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXHJcbiAgICAgICAgcmV3cml0ZTogKHBhdGg6IHN0cmluZykgPT4gcGF0aC5yZXBsYWNlKC9eXFwvcmFnLywgJycpXHJcbiAgICAgIH0sXHJcbiAgICAgICcvd3MnOiB7XHJcbiAgICAgICAgdGFyZ2V0OiBiYWNrZW5kVGFyZ2V0LFxyXG4gICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZSxcclxuICAgICAgICB3czogdHJ1ZVxyXG4gICAgICB9XHJcbiAgICB9XHJcbiAgfVxyXG59KVxyXG4iXSwKICAibWFwcGluZ3MiOiAiO0FBQTJRLFNBQVMsb0JBQW9CO0FBQ3hTLE9BQU8sU0FBUztBQUNoQixTQUFTLGVBQWU7QUFGeEIsSUFBTSxtQ0FBbUM7QUFLekMsSUFBTSxjQUFjLFFBQVEsSUFBSSxnQkFBZ0I7QUFDaEQsSUFBTSxnQkFBZ0Isb0JBQW9CLFdBQVc7QUFHckQsSUFBTSxVQUFVLFFBQVEsSUFBSSxZQUFZO0FBQ3hDLElBQU0sWUFBWSxvQkFBb0IsT0FBTztBQUU3QyxJQUFPLHNCQUFRLGFBQWE7QUFBQSxFQUMxQixTQUFTLENBQUMsSUFBSSxDQUFDO0FBQUEsRUFDZixTQUFTO0FBQUEsSUFDUCxPQUFPO0FBQUEsTUFDTCxLQUFLLFFBQVEsa0NBQVcsS0FBSztBQUFBLElBQy9CO0FBQUEsRUFDRjtBQUFBLEVBQ0EsUUFBUTtBQUFBLElBQ04sTUFBTTtBQUFBLElBQ04sTUFBTTtBQUFBLElBQ04sT0FBTztBQUFBLE1BQ0wsV0FBVztBQUFBLFFBQ1QsUUFBUTtBQUFBLFFBQ1IsY0FBYztBQUFBLE1BQ2hCO0FBQUEsTUFDQSxjQUFjO0FBQUEsUUFDWixRQUFRO0FBQUEsUUFDUixjQUFjO0FBQUEsTUFDaEI7QUFBQSxNQUNBLGlCQUFpQjtBQUFBLFFBQ2YsUUFBUTtBQUFBLFFBQ1IsY0FBYztBQUFBLE1BQ2hCO0FBQUEsTUFDQSxRQUFRO0FBQUEsUUFDTixRQUFRO0FBQUEsUUFDUixjQUFjO0FBQUEsUUFDZCxTQUFTLENBQUMsU0FBaUIsS0FBSyxRQUFRLFVBQVUsRUFBRTtBQUFBLE1BQ3REO0FBQUEsTUFDQSxPQUFPO0FBQUEsUUFDTCxRQUFRO0FBQUEsUUFDUixjQUFjO0FBQUEsUUFDZCxJQUFJO0FBQUEsTUFDTjtBQUFBLElBQ0Y7QUFBQSxFQUNGO0FBQ0YsQ0FBQzsiLAogICJuYW1lcyI6IFtdCn0K
