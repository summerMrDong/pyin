# 字典管理前端开发

## 内嵌热部署

先启动 Pyin 后端和主前端壳，并登录主壳；然后在本目录执行：

```powershell
npm run dev:embedded
```

该命令会监听 `src/`，并把模块联邦产物直接写入内嵌字典插件的
`backend/target/classes/plugin-static/dict/`。修改 Vue 或 CSS 后，等待终端完成构建，再在已登录的主壳按 `Ctrl + R` 刷新即可看到更新；无需重启后端、重新登录或执行 Maven。

提交或打包前仍执行：

```powershell
npm run build
mvn -pl pyin-plugins/pyin-plugin-dict/backend -am package -DskipTests
```
