# 插件注解优化说明

## 优化目标

简化插件 API 注解的使用，避免重复声明 path 和 method，让代码更简洁清晰。

## 主要改动

### 1. 新增 `@Permission` 子注解

将 `permissionCode` 和 `permissionName` 合并为一个独立的权限注解：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    String code();  // 权限编码
    String name();  // 权限名称
}
```

### 2. 移除 `AdminApi`、`ClientSdkApi`、`InternalApi` 中的 path 和 method

这些字段会自动从 Spring MVC 注解（`@GetMapping`、`@PostMapping` 等）中解析，无需重复声明。

### 3. 自动过滤固定路由前缀

扫描器会自动移除以下固定前缀：
- **Admin 控制器**：移除 `/admin` 前缀
- **Client 控制器**：移除 `/client` 前缀

这些前缀是网关层面的路由规则，不应该出现在插件 API 定义中。

**示例：**
```java
// Admin 控制器
@RequestMapping("/admin")
@GetMapping("/types")
// 扫描结果：path = "/types" (自动移除 /admin)

// Client 控制器
@RequestMapping("/client")
@GetMapping("/dict/label")
// 扫描结果：path = "/dict/label" (自动移除 /client)
```

### 4. 扫描器增强

`PluginApiScanner` 现在会：
- 自动从 Spring MVC 注解中解析 HTTP 方法（GET、POST、PUT、DELETE、PATCH）
- 自动从 Spring MVC 注解中解析路径
- 自动移除固定的 `/admin` 或 `/client` 前缀
- 验证方法必须包含有效的 Spring MVC 映射注解

## 迁移示例

### AdminApi 迁移

**旧写法：**

```java
@AdminApi(
    path = "/admin/types", 
    method = "GET", 
    permissionCode = "dict:view", 
    permissionName = "字典查看"
)
@GetMapping("/types")
public Result<List<Map<String, Object>>> listTypes() {
    return Result.ok(dictAdminService.listTypes());
}
```

**新写法：**

```java
@AdminApi(permission = @Permission(code = "dict:view", name = "字典查看"))
@GetMapping("/types")
public Result<List<Map<String, Object>>> listTypes() {
    return Result.ok(dictAdminService.listTypes());
}
```

### ClientSdkApi 迁移

**旧写法：**

```java
@ClientSdkApi(path = "/client/dict/label", method = "GET")
@GetMapping("/client/dict/label")
public Result<Map<String, String>> label() {
    return Result.ok(Map.of("label", "演示标签"));
}
```

**新写法：**

```java
@ClientSdkApi
@GetMapping("/client/dict/label")
public Result<Map<String, String>> label() {
    return Result.ok(Map.of("label", "演示标签"));
}
```

### InternalApi 迁移

**旧写法：**

```java
@InternalApi(path = "/internal/sync", method = "POST")
@PostMapping("/internal/sync")
public Result<?> sync() {
    return Result.ok();
}
```

**新写法：**

```java
@InternalApi
@PostMapping("/internal/sync")
public Result<?> sync() {
    return Result.ok();
}
```

## 优势

1. **减少重复**：不再需要在自定义注解和 Spring 注解中重复声明 path 和 method
2. **单一事实来源**：path 和 method 只在 Spring MVC 注解中声明一次
3. **更清晰的权限定义**：使用 `@Permission` 子注解使权限信息更结构化
4. **编译时检查**：如果忘记添加 Spring MVC 映射注解，会在启动时报错
5. **代码更简洁**：注解参数大幅减少，可读性更好

## 注意事项

1. **必须保留 Spring MVC 映射注解**：`@GetMapping`、`@PostMapping` 等不能省略
2. **路径以 Spring 注解为准**：扫描器会从 Spring 注解中提取实际的路径
3. **HTTP 方法以 Spring 注解为准**：扫描器会根据使用的注解类型推断 HTTP 方法
4. **控制器路径规范**：
   - **Admin 控制器**：使用 `@RequestMapping("/admin")` 作为类级别前缀
   - **Client 控制器**：使用 `@RequestMapping("/client")` 作为类级别前缀
   - 扫描器会自动移除这些固定前缀，生成干净的 API 路径
5. **向后兼容**：如果需要，可以保留旧的 path/method 字段作为备选（当前实现已完全移除）

### 路由映射规则

```
网关层路由：
  /api/plugins/{pluginId}/admin/**  → 插件内部 /admin/**
  /capi/plugins/{pluginId}/client/** → 插件内部 /client/**

插件内部路径：
  Admin: @RequestMapping("/admin") + @GetMapping("/types") = /admin/types
  Client: @RequestMapping("/client") + @GetMapping("/dict/label") = /client/dict/label

扫描器生成的 API 定义（去除固定前缀）：
  Admin API: path = "/types"
  Client API: path = "/dict/label"
```

## 批量迁移建议

可以使用 IDE 的查找替换功能批量迁移：

1. 查找：`@AdminApi(path = "([^"]+)", method = "([^"]+)", permissionCode = "([^"]+)", permissionName = "([^"]+)")`
2. 替换为：`@AdminApi(permission = @Permission(code = "$3", name = "$4"))`

对于没有权限的接口：

1. 查找：`@ClientSdkApi\(path = "[^"]+", method = "[^"]+"\)`
2. 替换为：`@ClientSdkApi`
