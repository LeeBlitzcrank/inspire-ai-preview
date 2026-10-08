// vite.config.mjs
import {
    defineConfig
} from "file:///Users/lee/Desktop/Project/Study%20Project/Codex/inspire-ai-preview/frontend/node_modules/.pnpm/vite@5.4.21/node_modules/vite/dist/node/index.js";
import vue
    from "file:///Users/lee/Desktop/Project/Study%20Project/Codex/inspire-ai-preview/frontend/node_modules/.pnpm/@vitejs+plugin-vue@5.2.4_vite@5.4.21_vue@3.5.43/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import AutoImport
    from "file:///Users/lee/Desktop/Project/Study%20Project/Codex/inspire-ai-preview/frontend/node_modules/.pnpm/unplugin-auto-import@21.1.0_@vueuse+core@14.4.0_vue@3.5.43__esbuild@0.21.5_rollup@4.63.5_vite@5.4.21/node_modules/unplugin-auto-import/dist/vite.mjs";
import Components
    from "file:///Users/lee/Desktop/Project/Study%20Project/Codex/inspire-ai-preview/frontend/node_modules/.pnpm/unplugin-vue-components@32.1.0_esbuild@0.21.5_rollup@4.63.5_vite@5.4.21_vue@3.5.43/node_modules/unplugin-vue-components/dist/vite.mjs";
import {
    ElementPlusResolver
} from "file:///Users/lee/Desktop/Project/Study%20Project/Codex/inspire-ai-preview/frontend/node_modules/.pnpm/unplugin-vue-components@32.1.0_esbuild@0.21.5_rollup@4.63.5_vite@5.4.21_vue@3.5.43/node_modules/unplugin-vue-components/dist/resolvers.mjs";
import {fileURLToPath} from "node:url";
import {copyFileSync} from "node:fs";

var __vite_injected_original_import_meta_url = "file:///Users/lee/Desktop/Project/Study%20Project/Codex/inspire-ai-preview/frontend/vite.config.mjs";
var vite_config_default = defineConfig({
  base: "/",
  plugins: [
    vue(),
    // Element Plus 按需引入：模板里的 el-xxx 组件与消息弹窗按需打包 + 自动引入样式
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
    {
      name: "copy-404",
      closeBundle() {
        copyFileSync("dist/index.html", "dist/404.html");
        console.log("\u2713 \u5DF2\u751F\u6210 dist/404.html\uFF08SPA \u8DEF\u7531 fallback\uFF09");
      }
    }
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
    }
  },
  server: {
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
      "/uploads": { target: "http://localhost:8083", changeOrigin: true },
      "/upload": { target: "http://localhost:8088", changeOrigin: true }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ["vue", "vue-router"]
        }
      }
    },
    chunkSizeWarningLimit: 500
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcubWpzIl0sCiAgInNvdXJjZXNDb250ZW50IjogWyJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiL1VzZXJzL2xlZS9EZXNrdG9wL1Byb2plY3QvU3R1ZHkgUHJvamVjdC9Db2RleC9pbnNwaXJlLWFpLXByZXZpZXcvZnJvbnRlbmRcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9Vc2Vycy9sZWUvRGVza3RvcC9Qcm9qZWN0L1N0dWR5IFByb2plY3QvQ29kZXgvaW5zcGlyZS1haS1wcmV2aWV3L2Zyb250ZW5kL3ZpdGUuY29uZmlnLm1qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vVXNlcnMvbGVlL0Rlc2t0b3AvUHJvamVjdC9TdHVkeSUyMFByb2plY3QvQ29kZXgvaW5zcGlyZS1haS1wcmV2aWV3L2Zyb250ZW5kL3ZpdGUuY29uZmlnLm1qc1wiO2ltcG9ydCB7ZGVmaW5lQ29uZmlnfSBmcm9tICd2aXRlJ1xuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXG5pbXBvcnQgQXV0b0ltcG9ydCBmcm9tICd1bnBsdWdpbi1hdXRvLWltcG9ydC92aXRlJ1xuaW1wb3J0IENvbXBvbmVudHMgZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvdml0ZSdcbmltcG9ydCB7RWxlbWVudFBsdXNSZXNvbHZlcn0gZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvcmVzb2x2ZXJzJ1xuaW1wb3J0IHtmaWxlVVJMVG9QYXRofSBmcm9tICdub2RlOnVybCdcbmltcG9ydCB7Y29weUZpbGVTeW5jfSBmcm9tICdub2RlOmZzJ1xuXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoe1xuICBiYXNlOiAnLycsXG4gIHBsdWdpbnM6IFtcbiAgICB2dWUoKSxcbiAgICAvLyBFbGVtZW50IFBsdXMgXHU2MzA5XHU5NzAwXHU1RjE1XHU1MTY1XHVGRjFBXHU2QTIxXHU2NzdGXHU5MUNDXHU3Njg0IGVsLXh4eCBcdTdFQzRcdTRFRjZcdTRFMEVcdTZEODhcdTYwNkZcdTVGMzlcdTdBOTdcdTYzMDlcdTk3MDBcdTYyNTNcdTUzMDUgKyBcdTgxRUFcdTUyQThcdTVGMTVcdTUxNjVcdTY4MzdcdTVGMEZcbiAgICBBdXRvSW1wb3J0KHsgcmVzb2x2ZXJzOiBbRWxlbWVudFBsdXNSZXNvbHZlcigpXSB9KSxcbiAgICBDb21wb25lbnRzKHsgcmVzb2x2ZXJzOiBbRWxlbWVudFBsdXNSZXNvbHZlcigpXSB9KSxcbiAgICB7XG4gICAgICBuYW1lOiAnY29weS00MDQnLFxuICAgICAgY2xvc2VCdW5kbGUoKSB7XG4gICAgICAgIGNvcHlGaWxlU3luYygnZGlzdC9pbmRleC5odG1sJywgJ2Rpc3QvNDA0Lmh0bWwnKVxuICAgICAgICBjb25zb2xlLmxvZygnXHUyNzEzIFx1NURGMlx1NzUxRlx1NjIxMCBkaXN0LzQwNC5odG1sXHVGRjA4U1BBIFx1OERFRlx1NzUzMSBmYWxsYmFja1x1RkYwOScpXG4gICAgICB9XG4gICAgfVxuICBdLFxuICByZXNvbHZlOiB7XG4gICAgYWxpYXM6IHtcbiAgICAgICdAJzogZmlsZVVSTFRvUGF0aChuZXcgVVJMKCcuL3NyYycsIGltcG9ydC5tZXRhLnVybCkpXG4gICAgfVxuICB9LFxuICBzZXJ2ZXI6IHtcbiAgICBwcm94eToge1xuICAgICAgJy9hcGknOiB7IHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MCcsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgJy91cGxvYWRzJzogeyB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODMnLCBjaGFuZ2VPcmlnaW46IHRydWUgfSxcbiAgICAgICcvdXBsb2FkJzogeyB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODgnLCBjaGFuZ2VPcmlnaW46IHRydWUgfVxuICAgIH1cbiAgfSxcbiAgYnVpbGQ6IHtcbiAgICByb2xsdXBPcHRpb25zOiB7XG4gICAgICBvdXRwdXQ6IHtcbiAgICAgICAgbWFudWFsQ2h1bmtzOiB7XG4gICAgICAgICAgdmVuZG9yOiBbJ3Z1ZScsICd2dWUtcm91dGVyJ11cbiAgICAgICAgfVxuICAgICAgfVxuICAgIH0sXG4gICAgY2h1bmtTaXplV2FybmluZ0xpbWl0OiA1MDBcbiAgfVxufSlcbiJdLAogICJtYXBwaW5ncyI6ICI7QUFBb1osU0FBUSxvQkFBbUI7QUFDL2EsT0FBTyxTQUFTO0FBQ2hCLE9BQU8sZ0JBQWdCO0FBQ3ZCLE9BQU8sZ0JBQWdCO0FBQ3ZCLFNBQVEsMkJBQTBCO0FBQ2xDLFNBQVEscUJBQW9CO0FBQzVCLFNBQVEsb0JBQW1CO0FBTmtPLElBQU0sMkNBQTJDO0FBUTlTLElBQU8sc0JBQVEsYUFBYTtBQUFBLEVBQzFCLE1BQU07QUFBQSxFQUNOLFNBQVM7QUFBQSxJQUNQLElBQUk7QUFBQTtBQUFBLElBRUosV0FBVyxFQUFFLFdBQVcsQ0FBQyxvQkFBb0IsQ0FBQyxFQUFFLENBQUM7QUFBQSxJQUNqRCxXQUFXLEVBQUUsV0FBVyxDQUFDLG9CQUFvQixDQUFDLEVBQUUsQ0FBQztBQUFBLElBQ2pEO0FBQUEsTUFDRSxNQUFNO0FBQUEsTUFDTixjQUFjO0FBQ1oscUJBQWEsbUJBQW1CLGVBQWU7QUFDL0MsZ0JBQVEsSUFBSSw4RUFBc0M7QUFBQSxNQUNwRDtBQUFBLElBQ0Y7QUFBQSxFQUNGO0FBQUEsRUFDQSxTQUFTO0FBQUEsSUFDUCxPQUFPO0FBQUEsTUFDTCxLQUFLLGNBQWMsSUFBSSxJQUFJLFNBQVMsd0NBQWUsQ0FBQztBQUFBLElBQ3REO0FBQUEsRUFDRjtBQUFBLEVBQ0EsUUFBUTtBQUFBLElBQ04sT0FBTztBQUFBLE1BQ0wsUUFBUSxFQUFFLFFBQVEseUJBQXlCLGNBQWMsS0FBSztBQUFBLE1BQzlELFlBQVksRUFBRSxRQUFRLHlCQUF5QixjQUFjLEtBQUs7QUFBQSxNQUNsRSxXQUFXLEVBQUUsUUFBUSx5QkFBeUIsY0FBYyxLQUFLO0FBQUEsSUFDbkU7QUFBQSxFQUNGO0FBQUEsRUFDQSxPQUFPO0FBQUEsSUFDTCxlQUFlO0FBQUEsTUFDYixRQUFRO0FBQUEsUUFDTixjQUFjO0FBQUEsVUFDWixRQUFRLENBQUMsT0FBTyxZQUFZO0FBQUEsUUFDOUI7QUFBQSxNQUNGO0FBQUEsSUFDRjtBQUFBLElBQ0EsdUJBQXVCO0FBQUEsRUFDekI7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=
