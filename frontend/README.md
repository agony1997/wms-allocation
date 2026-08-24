# 前端（Vue 3 + Vite）

`wms-allocation` 的 demo 前端，只做主路徑操作，不追求完整覆蓋後端所有端點。

```bash
npm install
npm run dev     # Vite dev server :5173，/api proxy 到 localhost:8080
npm run build   # 輸出到 ../src/main/resources/static，與後端打包成同一個 jar
```

目錄結構、API 呼叫、狀態管理、新增頁面 SOP 一律見 [docs/conventions/frontend.md](../docs/conventions/frontend.md)。
