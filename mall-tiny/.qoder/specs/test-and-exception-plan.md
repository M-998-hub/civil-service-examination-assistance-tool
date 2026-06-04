# mall-tiny 测试与异常处理落地方案

## Context

mall-tiny 项目当前仅有 1 个 `contextLoads()` 测试，pom.xml 中 `skipTests=true`，测试覆盖率为零。全局异常处理器仅覆盖 3 种异常类型，缺少通用兜底、日志记录和防信息泄露机制。本方案旨在：
1. 为核心业务逻辑补充单元测试
2. 设计轻量级 API 集成测试（大部分 @WebMvcTest + 核心流程 @SpringBootTest + H2）
3. 增强全局异常处理（补全异常类型、统一错误码、日志记录、屏蔽内部细节）

---

## 一、统一异常处理增强

### 1.1 修改 ResultCode 枚举

**文件**: `src/main/java/com/macro/mall/tiny/common/api/ResultCode.java`

```java
public enum ResultCode implements IErrorCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数检验失败"),          // 404 → 400
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    // === 新增业务错误码 ===
    IMPORT_SESSION_EXPIRED(1001, "导入会话不存在或已过期"),
    IMPORT_FILE_INVALID(1002, "导入文件格式无效"),
    MATCH_ARCHIVE_MISSING(1003, "用户档案不存在，请先完善个人信息"),
    USER_DUPLICATE(1004, "用户名已存在"),
    PASSWORD_MISMATCH(1005, "旧密码不正确");
    // ...
}
```

### 1.2 增强 GlobalExceptionHandler

**文件**: `src/main/java/com/macro/mall/tiny/common/exception/GlobalExceptionHandler.java`

改用 `@RestControllerAdvice`，添加 SLF4J Logger，新增以下处理器：

| 异常类型 | 响应码 | 返回消息 | 日志级别 |
|---------|-------|---------|---------|
| `ApiException` | 来自 errorCode | 原始消息 | WARN |
| `MethodArgumentNotValidException` | 400 | 字段+错误信息 | WARN |
| `BindException` | 400 | 字段+错误信息 | WARN |
| `HttpMessageNotReadableException` | 400 | "请求体格式错误" | WARN |
| `MissingServletRequestParameterException` | 400 | "缺少参数: {name}" | WARN |
| `HttpRequestMethodNotSupportedException` | 405 | "不支持的请求方法" | WARN |
| `MaxUploadSizeExceededException` | 400 | "文件大小超出限制" | WARN |
| `AccessDeniedException` | 403 | "没有相关权限" | WARN |
| `Exception` (兜底) | 500 | "系统繁忙，请稍后再试" | **ERROR** (含完整堆栈) |

**关键原则**:
- 兜底 handler 的 ERROR 日志记录完整堆栈用于服务端排查
- 响应体永不暴露内部类名、SQL、堆栈信息
- 所有 handler 统一返回 `CommonResult` 格式

### 1.3 业务代码异常重构

**文件**: `src/main/java/com/macro/mall/tiny/modules/ums/service/impl/AdminImportServiceImpl.java`

- 将 `throw new RuntimeException("会话不存在...")` 替换为 `Asserts.fail(ResultCode.IMPORT_SESSION_EXPIRED)`
- 将 `throw new RuntimeException("文件格式...")` 替换为 `Asserts.fail(ResultCode.IMPORT_FILE_INVALID)`

**文件**: `src/main/java/com/macro/mall/tiny/modules/ums/controller/AdminImportController.java`

- 移除冗余的 try/catch 块，让异常自然传播到 GlobalExceptionHandler

---

## 二、单元测试方案

### 2.1 技术选型

| 组件 | 说明 |
|------|------|
| **JUnit 5** (Jupiter) | 测试框架（已包含在 spring-boot-starter-test） |
| **Mockito** | Mock 框架（已包含） |
| **AssertJ** | 流式断言（已包含） |
| **ReflectionTestUtils** | 访问 private 方法（Spring Test 提供） |

无需新增测试依赖（H2 除外，见集成测试部分）。

### 2.2 测试目录结构

```
src/test/java/com/macro/mall/tiny/
├── MallTinyApplicationTests.java                     (已有，保留)
├── common/exception/
│   └── GlobalExceptionHandlerTest.java               (异常处理器验证)
└── modules/ums/
    ├── service/
    │   ├── MatchServiceTest.java                     (匹配算法)
    │   ├── AdminImportServiceTest.java               (导入解析逻辑)
    │   └── UmsAdminServiceTest.java                  (认证逻辑)
    └── controller/
        ├── UmsAdminControllerIntegrationTest.java    (登录注册 API)
        ├── MatchControllerTest.java                  (匹配 API)
        └── AdminImportControllerTest.java            (导入 API)
```

### 2.3 MatchServiceTest（最高优先级）

**测试对象**: `MatchServiceImpl` — 职位匹配算法

**注解**: `@ExtendWith(MockitoExtension.class)`  
**Mock**: `PositionService`, `UserArchiveService`  
**访问私有方法**: `ReflectionTestUtils.invokeMethod()`

#### 测试用例

| 分组 | 测试方法 | 输入 | 预期 |
|------|---------|------|------|
| **硬条件-学历** | `checkEducation_userMeetsRequirement_true` | user=本科, req=本科 | true |
| | `checkEducation_userExceedsRequirement_true` | user=硕士, req=本科 | true |
| | `checkEducation_userBelowRequirement_false` | user=大专, req=本科 | false |
| | `checkEducation_noRequirement_true` | req=null | true |
| **硬条件-政治面貌** | `checkPolitical_partyMemberMeetsAll_true` | user=中共党员, req=共青团员 | true |
| | `checkPolitical_massesCantMeetParty_false` | user=群众, req=中共党员 | false |
| | `checkPolitical_noRequirement_true` | req=不限 | true |
| **硬条件-应届** | `checkFreshGrad_required_userIsFresh_true` | req=true, user=true | true |
| | `checkFreshGrad_required_userNotFresh_false` | req=true, user=false | false |
| **评分-专业** | `calculateScore_exactMajorMatch_30pts` | user=软件工程, pos=软件工程 | 30 |
| | `calculateScore_sameCategoryMatch_15pts` | user=软件工程, pos=计算机科学 | 15 |
| | `calculateScore_noMajorMatch_0pts` | user=法学, pos=计算机类 | 0 |
| **评分-学历** | `calculateScore_educationExceeds_10pts` | user=硕士, req=本科 | 10 |
| | `calculateScore_educationEquals_5pts` | user=本科, req=本科 | 5 |
| **评分-政治** | `calculateScore_politicalExceeds_5pts` | user=中共党员, req=共青团员 | 5 |
| **推荐整合** | `recommend_filtersAndSortsByScore` | 3个职位(1个不满足硬条件) | 返回2个，按分数降序 |
| | `recommend_noPositions_emptyList` | 空职位列表 | [] |
| **分类** | `isInSameCategory_sameCategory_true` | 软件工程, 网络工程 | true |
| | `isInSameCategory_diffCategory_false` | 法学, 计算机科学 | false |

### 2.4 AdminImportServiceTest（高优先级）

**测试对象**: `AdminImportServiceImpl` — Excel 导入解析逻辑

**注解**: `@ExtendWith(MockitoExtension.class)`  
**Mock**: `PositionService`, `ImportTemplateService`

#### 测试用例

| 分组 | 测试方法 | 输入 | 预期 |
|------|---------|------|------|
| **学历解析** | `parseEducation_benkeAndAbove_returnsBenke` | "本科及以上" | "本科" |
| | `parseEducation_masterOnly_returnsMaster` | "仅限硕士" | "硕士" |
| | `parseEducation_multiLevel_returnsLowest` | "本科或硕士研究生" | "本科" |
| | `parseEducation_doctorLevel_returnsDoctor` | "博士研究生" | "博士" |
| | `parseEducation_dazhuan_returnsDazhuan` | "大专及以上" | "大专" |
| | `parseEducation_nullInput_returnsNull` | null | null |
| | `parseEducation_emptyString_returnsNull` | "" | null |
| **政治面貌解析** | `parsePolitical_partyMember_returnsParty` | "中共党员" | "中共党员" |
| | `parsePolitical_noLimit_returnsNoLimit` | "不限" | "不限" |
| | `parsePolitical_multiRequirement_returnsLowest` | "中共党员或共青团员" | "共青团员" |
| | `parsePolitical_nullInput_returnsNull` | null | null |
| **职位校验** | `validate_allValid_returnsNull` | 完整合法数据 | null |
| | `validate_emptyDepartment_returnsError` | department="" | 错误消息 |
| | `validate_yearOutOfRange_returnsError` | year=1999 | 错误消息 |
| | `validate_invalidEducation_returnsError` | education="高中" | 错误消息 |
| | `validate_zeroRecruitment_returnsError` | num=0 | 错误消息 |
| **导入执行** | `execute_sessionNotFound_throwsApiException` | 无效sessionId | ApiException |
| | `execute_duplicateKey_skipped` | 重复year_dept_name | 跳过计数+1 |

### 2.5 UmsAdminServiceTest（中优先级）

**测试对象**: `UmsAdminServiceImpl` — 用户认证服务

**注解**: `@ExtendWith(MockitoExtension.class)`  
**Mock**: `UmsAdminMapper`, `PasswordEncoder`, `JwtTokenUtil`, `UmsAdminCacheService`, `UmsAdminLoginLogMapper`

#### 测试用例

| 测试方法 | 场景 | 预期 |
|---------|------|------|
| `register_newUser_success` | 用户名不存在 | 返回注册用户，密码已编码 |
| `register_duplicateUsername_returnsNull` | 用户名已存在 | 返回 null |
| `login_validCredentials_returnsToken` | 正确密码 | 返回 JWT token |
| `login_wrongPassword_throwsException` | 错误密码 | 抛出异常 |
| `login_disabledAccount_throwsException` | 账户禁用(status=0) | 抛出异常 |
| `updatePassword_validOldPwd_success` | 旧密码正确 | 返回成功标记 |
| `updatePassword_wrongOldPwd_returnsNeg3` | 旧密码错误 | 返回 -3 |
| `updatePassword_userNotFound_returnsNeg2` | 用户不存在 | 返回 -2 |
| `updateRole_withRoles_deletesAndInserts` | roleIds=[1,2] | 先删后增 |
| `updateRole_emptyRoles_onlyDeletes` | roleIds=[] | 仅删除 |

---

## 三、API 集成测试方案

### 3.1 策略分层

| 层级 | 注解 | 用途 | 数量 |
|------|------|------|------|
| **Slice 测试** | `@WebMvcTest` | 验证路由、参数校验、权限、响应格式 | ~15 个 |
| **完整链路** | `@SpringBootTest` + H2 | 验证核心业务完整流程（仅导入流程） | ~3 个 |

### 3.2 新增依赖

```xml
<!-- pom.xml 新增 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### 3.3 测试配置文件

**新增文件**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  redis:
    host: localhost
    port: 6379
jwt:
  tokenHeader: Authorization
  secret: test-secret-key-for-unit-tests
  expiration: 604800
  tokenHead: 'Bearer '
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

### 3.4 UmsAdminControllerTest（@WebMvcTest）

| 测试方法 | HTTP | Endpoint | 场景 | 预期 |
|---------|------|----------|------|------|
| `login_valid_returns200` | POST | `/admin/login` | Mock 返回 token | 200 + token |
| `login_emptyBody_returns400` | POST | `/admin/login` | 无请求体 | 400 |
| `login_invalidJson_returns400` | POST | `/admin/login` | 畸形 JSON | 400 |
| `register_success_returns200` | POST | `/admin/register` | 注册成功 | 200 |
| `getInfo_noAuth_returns401` | GET | `/admin/info` | 无 token | 401 |
| `updatePassword_valid_returns200` | POST | `/admin/updatePassword` | Mock 成功 | 200 |

### 3.5 MatchControllerTest（@WebMvcTest）

| 测试方法 | 场景 | 预期 |
|---------|------|------|
| `recommend_success_returnsMatchList` | Mock 返回匹配结果 | 200 + 列表 |
| `recommend_noArchive_returnsFailed` | Mock 抛出 ApiException | 500 + 错误消息 |
| `recommend_noAuth_returns401` | 无认证 | 401 |

### 3.6 AdminImportControllerTest（@WebMvcTest）

| 测试方法 | 场景 | 预期 |
|---------|------|------|
| `upload_validExcel_returns200` | MockMultipartFile(.xlsx) | 200 + 预览数据 |
| `upload_emptyFile_returnsFailed` | 空文件 | 500 + 错误 |
| `execute_validSession_returns200` | Mock 成功 | 200 + 导入结果 |
| `execute_expiredSession_returnsFailed` | Mock 抛出异常 | 对应错误码 |

### 3.7 GlobalExceptionHandlerTest

使用 `@WebMvcTest` + 临时测试 Controller（内部类），验证每种异常的映射是否正确：

| 测试方法 | 触发异常 | 预期响应 |
|---------|---------|---------|
| `apiException_returnsMappedCode` | ApiException | 对应 code+message |
| `validationException_returns400` | @Valid 校验失败 | code=400 |
| `messageNotReadable_returns400` | 畸形 JSON | code=400, 固定消息 |
| `methodNotSupported_returns405` | GET 访问 POST 端点 | code=405 |
| `genericException_returns500Generic` | NullPointerException | code=500, "系统繁忙" |

---

## 四、构建配置修改

**文件**: `pom.xml`

1. `<skipTests>true</skipTests>` → `<skipTests>false</skipTests>`
2. 新增 H2 依赖（test scope）

---

## 五、实施顺序

### Phase 1: 异常处理增强（不破坏现有功能）
1. 修改 `ResultCode.java` — VALIDATE_FAILED 改 400，新增业务错误码
2. 增强 `GlobalExceptionHandler.java` — 新增 handler + 日志
3. 重构 `AdminImportServiceImpl` — RuntimeException → Asserts.fail()
4. 清理 Controller 中冗余 try/catch

### Phase 2: 单元测试（纯逻辑，无 Spring 上下文）
1. `MatchServiceTest.java` — 匹配算法测试（~20 用例）
2. `AdminImportServiceTest.java` — 解析校验测试（~18 用例）
3. `UmsAdminServiceTest.java` — 认证逻辑测试（~10 用例）

### Phase 3: API 集成测试
1. 创建 `application-test.yml`
2. `GlobalExceptionHandlerTest.java` — 异常映射验证
3. `UmsAdminControllerTest.java` — 登录注册 API
4. `MatchControllerTest.java` — 匹配 API
5. `AdminImportControllerTest.java` — 导入 API

### Phase 4: 构建配置
1. 修改 pom.xml（skipTests + H2 依赖）
2. 本地运行 `mvn test` 确认全部通过

---

## 六、验收标准

| 维度 | 标准 |
|------|------|
| 单元测试 | ~48 个用例，覆盖正常流程、边界值、异常分支 |
| 集成测试 | ~18 个用例，覆盖关键 API 路由和异常响应 |
| 异常处理 | GlobalExceptionHandler 覆盖 8+ 种异常类型 |
| 信息安全 | 兜底 handler 不泄露堆栈/SQL/类名 |
| 日志 | 所有 handler 记录日志（WARN/ERROR） |
| 构建 | `mvn test` 全部通过 |

---

## 七、关键文件清单

### 修改文件
- `src/main/java/com/macro/mall/tiny/common/api/ResultCode.java`
- `src/main/java/com/macro/mall/tiny/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/macro/mall/tiny/modules/ums/service/impl/AdminImportServiceImpl.java`
- `src/main/java/com/macro/mall/tiny/modules/ums/controller/AdminImportController.java`（清理 try/catch）
- `pom.xml`

### 新增文件
- `src/test/java/com/macro/mall/tiny/modules/ums/service/MatchServiceTest.java`
- `src/test/java/com/macro/mall/tiny/modules/ums/service/AdminImportServiceTest.java`
- `src/test/java/com/macro/mall/tiny/modules/ums/service/UmsAdminServiceTest.java`
- `src/test/java/com/macro/mall/tiny/modules/ums/controller/UmsAdminControllerTest.java`
- `src/test/java/com/macro/mall/tiny/modules/ums/controller/MatchControllerTest.java`
- `src/test/java/com/macro/mall/tiny/modules/ums/controller/AdminImportControllerTest.java`
- `src/test/java/com/macro/mall/tiny/common/exception/GlobalExceptionHandlerTest.java`
- `src/test/resources/application-test.yml`

---

## 八、验证方式

```bash
# 运行全部测试
mvn test

# 仅运行单元测试
mvn test -Dtest="*ServiceTest"

# 仅运行集成测试
mvn test -Dtest="*ControllerTest,*HandlerTest"

# 验证异常处理
# 启动应用后，发送畸形请求验证返回格式：
# curl -X POST http://localhost:8080/admin/login -d "invalid json"
# 预期: {"code":400,"message":"请求体格式错误","data":null}
```
