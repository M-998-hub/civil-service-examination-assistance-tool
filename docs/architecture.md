# 业务架构与主要流程

## 模块边界

| 模块 | 责任 |
| --- | --- |
| `auth` | 登录、注册、JWT、用户、角色、菜单、资源与动态授权 |
| `profile` | 考生学历、专业、政治面貌等个人档案 |
| `position` | 岗位检索、详情、统计与后台维护 |
| `match` | 硬性条件过滤、软性评分、匹配结果与预测记录 |
| `favorite` | 岗位收藏与用户侧清单 |
| `importdata` | Excel 上传、模板映射、校验及批量写入 |
| `ai` | 匹配解释和档案问答，负责对接 DeepSeek |

## 用户选岗流程

```mermaid
flowchart TD
    LOGIN[登录并取得 JWT] --> PROFILE[完善个人档案]
    PROFILE --> SEARCH[筛选或搜索岗位]
    PROFILE --> ENGINE[运行匹配引擎]
    ENGINE --> FILTER[学历/专业/政治面貌等硬性过滤]
    FILTER --> SCORE[地区/层级/招录特征等软性评分]
    SCORE --> RESULT[排序后的匹配结果]
    SEARCH --> DETAIL[查看岗位详情]
    RESULT --> DETAIL
    DETAIL --> FAVORITE[收藏]
    DETAIL --> COMPARE[加入对比]
    DETAIL --> ANALYZE[AI 深度分析]
    ANALYZE --> DEEPSEEK[后端转发至 DeepSeek]
```

## 管理员导入流程

```mermaid
sequenceDiagram
    actor Admin as 管理员
    participant Web as Vue 管理端
    participant Security as JWT + 动态 RBAC
    participant Import as 导入服务
    participant Strategy as 业务导入策略
    participant DB as MySQL

    Admin->>Web: 上传 Excel 并选择模板
    Web->>Security: 携带 JWT 请求导入接口
    Security-->>Web: 校验身份与资源权限
    Security->>Import: 放行请求
    Import->>Import: EasyExcel 逐行解析
    Import->>Strategy: 字段映射、规范化与校验
    Strategy->>DB: 分批写入事务
    DB-->>Web: 成功数、失败行与错误原因
```

## 权限更新流程

管理员修改用户—角色或角色—资源关系后，服务清理对应 Redis 权限缓存；后续请求由动态安全元数据重新读取授权关系，因此不需要重启应用。
