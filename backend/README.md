# 后端服务

智能考公选岗系统 REST API。业务代码位于 `com.m998.civilservice`，按认证、档案、岗位、匹配、收藏、导入和 AI 七个领域组织。

```bash
mvn test
mvn spring-boot:run
```

配置统一通过根目录 `.env.example` 所列环境变量注入。数据库初始化脚本位于 `sql/`，接口文档在应用启动后可通过 Swagger 查看。
