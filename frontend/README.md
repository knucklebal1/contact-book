# Contact Book 前端

React + Vite 单页应用，对接仓库 `backend/` 中的 Spring Boot API。

## 本地启动

要求 Node.js 20.19+ 或 22.12+，以及可用的后端（默认 `http://localhost:8080`）。

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问终端显示的地址。开发服务器会将 `/api` 代理到 `http://localhost:8080`。
也可以在部署时设置 `VITE_API_BASE_URL` 指向后端地址（不要以 `/` 结尾），然后执行 `npm run build`。

```powershell
npm test
npm run build
```

功能：联系人分页与搜索、收藏筛选、增删改、多种联系方式、Excel 导入/导出及模板下载。导入仅接受不超过 5 MB 的 `.xlsx` 文件。列表导出遵循当前搜索和收藏筛选条件。
