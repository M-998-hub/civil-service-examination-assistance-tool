# 认证授权API

<cite>
**本文引用的文件**
- [UmsAdminController.java](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java)
- [UmsAdminLoginParam.java](file://src/main/java/com/macro/mall/tiny/modules/ums/dto/UmsAdminLoginParam.java)
- [UpdateAdminPasswordParam.java](file://src/main/java/com/macro/mall/tiny/modules/ums/dto/UpdateAdminPasswordParam.java)
- [JwtTokenUtil.java](file://src/main/java/com/macro/mall/tiny/security/util/JwtTokenUtil.java)
- [JwtAuthenticationTokenFilter.java](file://src/main/java/com/macro/mall/tiny/security/component/JwtAuthenticationTokenFilter.java)
- [SecurityConfig.java](file://src/main/java/com/macro/mall/tiny/security/config/SecurityConfig.java)
- [IgnoreUrlsConfig.java](file://src/main/java/com/macro/mall/tiny/security/config/IgnoreUrlsConfig.java)
- [RestAuthenticationEntryPoint.java](file://src/main/java/com/macro/mall/tiny/security/component/RestAuthenticationEntryPoint.java)
- [RestfulAccessDeniedHandler.java](file://src/main/java/com/macro/mall/tiny/security/component/RestfulAccessDeniedHandler.java)
- [CommonResult.java](file://src/main/java/com/macro/mall/tiny/common/api/CommonResult.java)
- [application.yml](file://src/main/resources/application.yml)
- [UmsAdminService.java](file://src/main/java/com/macro/mall/tiny/modules/ums/service/UmsAdminService.java)
- [UmsAdminServiceImpl.java](file://src/main/java/com/macro/mall/tiny/modules/ums/service/impl/UmsAdminServiceImpl.java)
- [UmsAdmin.java](file://src/main/java/com/macro/mall/tiny/modules/ums/model/UmsAdmin.java)
- [GlobalExceptionHandler.java](file://src/main/java/com/macro/mall/tiny/common/exception/GlobalExceptionHandler.java)
- [UmsAdminControllerTest.java](file://src/test/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminControllerTest.java)
</cite>

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖分析](#依赖分析)
7. [性能考虑](#性能考虑)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本文件面向认证授权模块的API接口文档，覆盖用户登录、注册、密码找回、令牌刷新、用户信息获取等认证相关接口。文档同时阐述JWT令牌的生成、验证、刷新机制，以及认证流程（用户名密码验证、角色权限检查、资源权限验证）。提供每个接口的HTTP方法、URL路径、请求参数、响应格式、状态码含义，并给出完整的接口调用示例与错误处理机制说明。

## 项目结构
认证授权相关的核心文件分布如下：
- 控制层：UmsAdminController 提供认证相关REST接口
- DTO：UmsAdminLoginParam、UpdateAdminPasswordParam 定义登录与改密请求参数
- 安全配置：SecurityConfig、IgnoreUrlsConfig、JwtAuthenticationTokenFilter 组成Spring Security与JWT过滤链
- 工具类：JwtTokenUtil 负责JWT生成、解析与刷新
- 异常处理：RestAuthenticationEntryPoint、RestfulAccessDeniedHandler、GlobalExceptionHandler 统一处理未登录、权限不足与全局异常
- 响应封装：CommonResult 统一返回结构
- 配置：application.yml 中定义JWT与白名单等配置项

```mermaid
graph TB
subgraph "控制层"
C1["UmsAdminController<br/>认证接口控制器"]
end
subgraph "服务层"
S1["UmsAdminService<br/>接口"]
S2["UmsAdminServiceImpl<br/>实现"]
end
subgraph "安全配置"
K1["SecurityConfig<br/>安全过滤链配置"]
K2["IgnoreUrlsConfig<br/>白名单配置"]
K3["JwtAuthenticationTokenFilter<br/>JWT过滤器"]
E1["RestAuthenticationEntryPoint<br/>未登录处理"]
E2["RestfulAccessDeniedHandler<br/>权限不足处理"]
end
subgraph "工具与模型"
T1["JwtTokenUtil<br/>JWT工具"]
M1["UmsAdmin<br/>用户模型"]
R1["CommonResult<br/>统一响应"]
Y1["application.yml<br/>配置"]
end
subgraph "异常处理"
X1["GlobalExceptionHandler<br/>全局异常处理"]
end
C1 --> S1
S1 --> S2
S2 --> T1
K1 --> K3
K1 --> E1
K1 --> E2
K2 --> K1
K3 --> T1
C1 --> R1
S2 --> M1
Y1 --> K1
Y1 --> T1
X1 --> R1
```

图表来源
- [UmsAdminController.java:46-350](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L46-L350)
- [UmsAdminService.java:19-90](file://src/main/java/com/macro/mall/tiny/modules/ums/service/UmsAdminService.java#L19-L90)
- [UmsAdminServiceImpl.java:47-200](file://src/main/java/com/macro/mall/tiny/modules/ums/service/impl/UmsAdminServiceImpl.java#L47-L200)
- [SecurityConfig.java:46-84](file://src/main/java/com/macro/mall/tiny/security/config/SecurityConfig.java#L46-L84)
- [IgnoreUrlsConfig.java:16-22](file://src/main/java/com/macro/mall/tiny/security/config/IgnoreUrlsConfig.java#L16-L22)
- [JwtAuthenticationTokenFilter.java:26-59](file://src/main/java/com/macro/mall/tiny/security/component/JwtAuthenticationTokenFilter.java#L26-L59)
- [JwtTokenUtil.java:28-171](file://src/main/java/com/macro/mall/tiny/security/util/JwtTokenUtil.java#L28-L171)
- [RestAuthenticationEntryPoint.java:17-28](file://src/main/java/com/macro/mall/tiny/security/component/RestAuthenticationEntryPoint.java#L17-L28)
- [RestfulAccessDeniedHandler.java:17-30](file://src/main/java/com/macro/mall/tiny/security/component/RestfulAccessDeniedHandler.java#L17-L30)
- [CommonResult.java:7-125](file://src/main/java/com/macro/mall/tiny/common/api/CommonResult.java#L7-L125)
- [application.yml:24-57](file://src/main/resources/application.yml#L24-L57)
- [GlobalExceptionHandler.java:22-100](file://src/main/java/com/macro/mall/tiny/common/exception/GlobalExceptionHandler.java#L22-L100)

章节来源
- [UmsAdminController.java:46-350](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L46-L350)
- [application.yml:24-57](file://src/main/resources/application.yml#L24-L57)

## 核心组件
- 控制器UmsAdminController：提供登录、注册、忘记密码、刷新令牌、获取当前用户信息等接口
- 服务UmsAdminService/Impl：实现登录、注册、刷新令牌、角色与资源权限查询、密码更新等逻辑
- 安全配置SecurityConfig：配置白名单、异常处理器、JWT过滤器、动态权限过滤器
- JWT工具JwtTokenUtil：生成、解析、校验、刷新JWT
- 过滤器JwtAuthenticationTokenFilter：从请求头提取JWT并注入认证上下文
- 统一响应CommonResult：统一封装响应结构
- 配置application.yml：JWT密钥、过期时间、请求头、白名单等

章节来源
- [UmsAdminController.java:46-350](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L46-L350)
- [UmsAdminService.java:19-90](file://src/main/java/com/macro/mall/tiny/modules/ums/service/UmsAdminService.java#L19-L90)
- [UmsAdminServiceImpl.java:47-200](file://src/main/java/com/macro/mall/tiny/modules/ums/service/impl/UmsAdminServiceImpl.java#L47-L200)
- [SecurityConfig.java:46-84](file://src/main/java/com/macro/mall/tiny/security/config/SecurityConfig.java#L46-L84)
- [JwtTokenUtil.java:28-171](file://src/main/java/com/macro/mall/tiny/security/util/JwtTokenUtil.java#L28-L171)
- [JwtAuthenticationTokenFilter.java:26-59](file://src/main/java/com/macro/mall/tiny/security/component/JwtAuthenticationTokenFilter.java#L26-L59)
- [CommonResult.java:7-125](file://src/main/java/com/macro/mall/tiny/common/api/CommonResult.java#L7-L125)
- [application.yml:24-57](file://src/main/resources/application.yml#L24-L57)

## 架构总览
认证授权的整体流程如下：
- 客户端向登录接口提交用户名与密码
- 服务端验证凭据，成功则签发JWT
- 客户端在后续请求中携带JWT到请求头
- 过滤器从请求头解析JWT，加载用户详情并校验有效性
- 若令牌有效，进入业务处理；若无效或过期，触发未登录或权限不足处理

```mermaid
sequenceDiagram
participant Client as "客户端"
participant Ctrl as "UmsAdminController"
participant Svc as "UmsAdminServiceImpl"
participant Jwt as "JwtTokenUtil"
participant Sec as "JwtAuthenticationTokenFilter"
participant Chain as "SecurityFilterChain"
Client->>Ctrl : "POST /admin/login"
Ctrl->>Svc : "login(username,password)"
Svc->>Jwt : "generateToken(userDetails)"
Jwt-->>Svc : "token"
Svc-->>Ctrl : "token"
Ctrl-->>Client : "200 成功，返回token"
Client->>Sec : "GET /xxx (Authorization : Bearer xxx)"
Sec->>Jwt : "getUserNameFromToken(token)"
Jwt-->>Sec : "username"
Sec->>Svc : "loadUserByUsername(username)"
Svc-->>Sec : "UserDetails"
Sec->>Jwt : "validateToken(token,userDetails)"
Jwt-->>Sec : "valid?"
Sec->>Chain : "注入认证上下文"
Chain-->>Client : "业务响应"
```

图表来源
- [UmsAdminController.java:166-196](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L166-L196)
- [UmsAdminServiceImpl.java:98-119](file://src/main/java/com/macro/mall/tiny/modules/ums/service/impl/UmsAdminServiceImpl.java#L98-L119)
- [JwtTokenUtil.java:117-122](file://src/main/java/com/macro/mall/tiny/security/util/JwtTokenUtil.java#L117-L122)
- [JwtAuthenticationTokenFilter.java:37-56](file://src/main/java/com/macro/mall/tiny/security/component/JwtAuthenticationTokenFilter.java#L37-L56)
- [SecurityConfig.java:46-84](file://src/main/java/com/macro/mall/tiny/security/config/SecurityConfig.java#L46-L84)

## 详细组件分析

### 接口清单与规范
以下为认证授权相关接口的HTTP方法、URL路径、请求参数、响应格式与状态码说明。所有响应均采用统一结构CommonResult，包含code、message、data字段。

- 登录接口
  - 方法：POST
  - 路径：/admin/login
  - 请求体：UmsAdminLoginParam
    - username：字符串，必填
    - password：字符串，必填
  - 响应：CommonResult<Map<String,Object>>
    - data.token：字符串，JWT令牌
    - data.tokenHead：字符串，令牌前缀（如Bearer）
    - data.roles：数组，用户角色名称列表
    - data.resources：数组，用户可访问的资源URL列表
  - 状态码：200 成功；400 参数校验失败或用户名/密码错误；500 服务器内部错误

- 注册接口
  - 方法：POST
  - 路径：/admin/register
  - 请求体：Map<String,String>
    - username：字符串，必填
    - password：字符串，必填
    - confirmPassword：字符串，必填，需与password一致
    - nickName：字符串，可选
    - email：字符串，可选
  - 响应：CommonResult<Void>
  - 状态码：200 成功；400 参数校验失败；500 操作失败

- 忘记密码-发送验证码
  - 方法：POST
  - 路径：/admin/forgot/send-code
  - 请求体：Map<String,String>
    - email：字符串，必填
  - 响应：CommonResult<Map<String,Object>>
    - data.message：字符串，提示信息
    - data.code：字符串（演示），实际应由邮件服务下发
  - 状态码：200 成功；400 参数校验失败；500 操作失败

- 忘记密码-重置密码
  - 方法：POST
  - 路径：/admin/forgot/reset
  - 请求体：Map<String,String>
    - email：字符串，必填
    - code：字符串，必填，验证码
    - newPassword：字符串，必填
    - confirmPassword：字符串，必填，需与newPassword一致
  - 响应：CommonResult<Void>
  - 状态码：200 成功；400 参数校验失败或验证码无效；500 操作失败

- 刷新令牌
  - 方法：GET
  - 路径：/admin/refreshToken
  - 请求头：Authorization: Bearer {token}
  - 响应：CommonResult<Map<String,String>>
    - data.token：字符串，新的JWT令牌
    - data.tokenHead：字符串，令牌前缀
  - 状态码：200 成功；401 令牌已过期；500 服务器内部错误

- 获取当前登录用户信息
  - 方法：GET
  - 路径：/admin/info
  - 请求头：Authorization: Bearer {token}
  - 响应：CommonResult<Map<String,Object>>
    - data.username：字符串
    - data.icon：字符串
    - data.menus：数组，用户菜单树
    - data.roles：数组，用户角色名称列表
    - data.resources：数组，用户可访问的资源URL列表
  - 状态码：200 成功；401 未登录；500 服务器内部错误

- 登出接口
  - 方法：POST
  - 路径：/admin/logout
  - 响应：CommonResult<Void>
  - 状态码：200 成功

章节来源
- [UmsAdminController.java:62-257](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L62-L257)
- [UmsAdminLoginParam.java:15-22](file://src/main/java/com/macro/mall/tiny/modules/ums/dto/UmsAdminLoginParam.java#L15-L22)
- [application.yml:24-28](file://src/main/resources/application.yml#L24-L28)

### JWT令牌生成、验证与刷新机制
- 生成
  - 使用JwtTokenUtil根据UserDetails生成JWT，包含用户名与创建时间等声明
- 验证
  - 从请求头读取Authorization: Bearer {token}
  - 解析token获取用户名，结合UserDetailsService加载用户详情
  - 校验签名与过期时间，通过则注入认证上下文
- 刷新
  - 支持在未过期前提下刷新token，避免频繁登录
  - 若token已过期或格式不正确，则无法刷新

```mermaid
flowchart TD
Start(["开始"]) --> Parse["解析请求头 Authorization"]
Parse --> HasPrefix{"是否以 tokenHead 开头？"}
HasPrefix --> |否| Pass["放行至后续过滤器"]
HasPrefix --> |是| Extract["去除 tokenHead，得到 token"]
Extract --> Validate["解析并校验 token"]
Validate --> Valid{"是否有效且未过期？"}
Valid --> |否| Deny["拒绝访问未登录/过期"]
Valid --> |是| Inject["加载用户详情并注入认证上下文"]
Inject --> Pass
```

图表来源
- [JwtAuthenticationTokenFilter.java:37-56](file://src/main/java/com/macro/mall/tiny/security/component/JwtAuthenticationTokenFilter.java#L37-L56)
- [JwtTokenUtil.java:76-96](file://src/main/java/com/macro/mall/tiny/security/util/JwtTokenUtil.java#L76-L96)

章节来源
- [JwtTokenUtil.java:28-171](file://src/main/java/com/macro/mall/tiny/security/util/JwtTokenUtil.java#L28-L171)
- [JwtAuthenticationTokenFilter.java:26-59](file://src/main/java/com/macro/mall/tiny/security/component/JwtAuthenticationTokenFilter.java#L26-L59)

### 认证流程与权限检查
- 用户名密码验证
  - 登录接口接收UmsAdminLoginParam，服务端通过UserDetailsService加载用户并比对密码
- 角色权限检查
  - 登录成功后，返回用户角色列表，前端据此控制界面元素
- 资源权限验证
  - 登录成功后，返回用户可访问的资源URL列表，前端路由与按钮可据此隐藏/显示
- 动态权限过滤
  - SecurityConfig中配置了动态权限过滤器，结合IgnoreUrlsConfig实现白名单与动态资源映射

```mermaid
sequenceDiagram
participant Client as "客户端"
participant Ctrl as "UmsAdminController"
participant Svc as "UmsAdminServiceImpl"
participant Role as "UmsRoleService"
participant Res as "UmsResourceService"
Client->>Ctrl : "GET /admin/info"
Ctrl->>Svc : "getAdminByUsername(principal.name)"
Svc-->>Ctrl : "UmsAdmin"
Ctrl->>Role : "getMenuList(adminId)"
Role-->>Ctrl : "menus"
Ctrl->>Svc : "getRoleList(adminId)"
Svc-->>Ctrl : "roles"
Ctrl->>Svc : "getResourceList(adminId)"
Svc-->>Ctrl : "resources"
Ctrl-->>Client : "返回用户信息与权限"
```

图表来源
- [UmsAdminController.java:213-250](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L213-L250)
- [UmsAdminService.java:68-73](file://src/main/java/com/macro/mall/tiny/modules/ums/service/UmsAdminService.java#L68-L73)

章节来源
- [UmsAdminController.java:177-194](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L177-L194)
- [UmsAdminService.java:68-73](file://src/main/java/com/macro/mall/tiny/modules/ums/service/UmsAdminService.java#L68-L73)

### 接口调用示例
- 登录获取令牌
  - 请求：POST /admin/login
  - 请求体：{
      "username": "admin",
      "password": "123456"
    }
  - 响应：{
      "code": 200,
      "message": "操作成功",
      "data": {
        "token": "eyJhbGciOiJIUzUxMiJ9...",
        "tokenHead": "Bearer ",
        "roles": ["ROLE_ADMIN"],
        "resources": ["/admin/**"]
      }
    }
- 携带令牌访问受保护接口
  - 请求：GET /admin/info
  - 请求头：Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
  - 响应：{
      "code": 200,
      "message": "操作成功",
      "data": {
        "username": "admin",
        "icon": "",
        "menus": [...],
        "roles": ["ROLE_ADMIN"],
        "resources": ["/admin/**"]
      }
    }
- 令牌刷新
  - 请求：GET /admin/refreshToken
  - 请求头：Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
  - 响应：{
      "code": 200,
      "message": "操作成功",
      "data": {
        "token": "new-jwt-token",
        "tokenHead": "Bearer "
      }
    }

章节来源
- [UmsAdminControllerTest.java:50-110](file://src/test/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminControllerTest.java#L50-L110)
- [UmsAdminController.java:166-211](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L166-L211)

## 依赖分析
- 控制器依赖服务接口与DTO，服务实现依赖JWT工具与密码编码器
- 安全配置依赖白名单、JWT过滤器、异常处理器与动态权限组件
- 统一响应与异常处理贯穿整个认证流程

```mermaid
graph LR
Ctrl["UmsAdminController"] --> SvcI["UmsAdminService"]
SvcI --> SvcImpl["UmsAdminServiceImpl"]
SvcImpl --> Jwt["JwtTokenUtil"]
SvcImpl --> Pwd["PasswordEncoder"]
SecCfg["SecurityConfig"] --> JwtFilter["JwtAuthenticationTokenFilter"]
SecCfg --> Ign["IgnoreUrlsConfig"]
SecCfg --> Unauth["RestAuthenticationEntryPoint"]
SecCfg --> Deny["RestfulAccessDeniedHandler"]
Ctrl --> Resp["CommonResult"]
Yml["application.yml"] --> SecCfg
Yml --> Jwt
```

图表来源
- [UmsAdminController.java:46-350](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L46-L350)
- [UmsAdminService.java:19-90](file://src/main/java/com/macro/mall/tiny/modules/ums/service/UmsAdminService.java#L19-L90)
- [UmsAdminServiceImpl.java:47-200](file://src/main/java/com/macro/mall/tiny/modules/ums/service/impl/UmsAdminServiceImpl.java#L47-L200)
- [SecurityConfig.java:46-84](file://src/main/java/com/macro/mall/tiny/security/config/SecurityConfig.java#L46-L84)
- [IgnoreUrlsConfig.java:16-22](file://src/main/java/com/macro/mall/tiny/security/config/IgnoreUrlsConfig.java#L16-L22)
- [JwtAuthenticationTokenFilter.java:26-59](file://src/main/java/com/macro/mall/tiny/security/component/JwtAuthenticationTokenFilter.java#L26-L59)
- [JwtTokenUtil.java:28-171](file://src/main/java/com/macro/mall/tiny/security/util/JwtTokenUtil.java#L28-L171)
- [CommonResult.java:7-125](file://src/main/java/com/macro/mall/tiny/common/api/CommonResult.java#L7-L125)
- [application.yml:24-57](file://src/main/resources/application.yml#L24-L57)

章节来源
- [UmsAdminController.java:46-350](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L46-L350)
- [UmsAdminServiceImpl.java:47-200](file://src/main/java/com/macro/mall/tiny/modules/ums/service/impl/UmsAdminServiceImpl.java#L47-L200)
- [SecurityConfig.java:46-84](file://src/main/java/com/macro/mall/tiny/security/config/SecurityConfig.java#L46-L84)

## 性能考虑
- JWT无状态设计避免服务端会话存储，降低扩展性压力
- 白名单配置减少不必要的认证开销
- 缓存策略：服务层对用户与资源列表进行缓存，降低数据库压力
- 密码加密采用BCrypt，保证安全性的同时兼顾性能

## 故障排查指南
- 认证失败（用户名或密码错误）
  - 现象：登录接口返回参数校验失败或“用户名或密码错误”
  - 排查：确认用户名与密码是否正确，检查密码是否经过客户端加密
- 令牌过期
  - 现象：访问受保护接口返回未登录
  - 排查：调用刷新接口获取新令牌，或重新登录
- 权限不足
  - 现象：返回403禁止访问
  - 排查：确认用户角色与资源URL权限是否匹配
- 参数校验失败
  - 现象：返回参数校验失败
  - 排查：检查请求体JSON格式与必填字段
- 全局异常
  - 现象：系统异常返回统一错误信息
  - 排查：查看日志定位具体异常

章节来源
- [UmsAdminController.java:166-196](file://src/main/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminController.java#L166-L196)
- [GlobalExceptionHandler.java:22-100](file://src/main/java/com/macro/mall/tiny/common/exception/GlobalExceptionHandler.java#L22-L100)
- [RestAuthenticationEntryPoint.java:17-28](file://src/main/java/com/macro/mall/tiny/security/component/RestAuthenticationEntryPoint.java#L17-L28)
- [RestfulAccessDeniedHandler.java:17-30](file://src/main/java/com/macro/mall/tiny/security/component/RestfulAccessDeniedHandler.java#L17-L30)

## 结论
本认证授权模块基于Spring Security与JWT实现了完整的登录、注册、密码找回、令牌刷新与用户信息获取能力。通过白名单与动态权限过滤，系统在保证安全的同时具备良好的扩展性。建议在生产环境中完善邮件验证码服务、接入更严格的动态权限数据源，并持续监控与优化缓存策略。

## 附录
- 配置项说明
  - jwt.tokenHeader：存储JWT的请求头键名
  - jwt.tokenHead：JWT前缀
  - jwt.secret：JWT加解密密钥
  - jwt.expiration：JWT过期时间（秒）
  - secure.ignored.urls：免认证白名单路径

章节来源
- [application.yml:24-57](file://src/main/resources/application.yml#L24-L57)