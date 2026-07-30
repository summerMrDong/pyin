# 导出工坊前端开发

## 内嵌快速预览

先启动 Pyin 后端与主前端壳，并登录主壳。然后在本目录运行：

```powershell
npm run dev:embedded
```

该命令会监听 `src/`，直接把模块联邦产物写入内嵌后端的
`backend/target/classes/plugin-static/export-workshop/`。每次修改 Vue 或 CSS 后，等待终端显示构建完成，再在已登录的主壳页面按 `Ctrl + R` 刷新即可查看效果；不需要执行 Maven，也不需要重新登录。

提交或打包前仍执行：

```powershell
npm run build
mvn -pl pyin-plugins/pyin-plugin-export-workshop/backend -am package -DskipTests
```
