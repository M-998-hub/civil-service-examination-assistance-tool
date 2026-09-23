# scripts/generate_prompt.py
from jinja2 import Environment, FileSystemLoader
import os

def main():
    # 1. 获取项目根目录（假设脚本在 scripts/ 下）
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    
    # 2. 配置 Jinja2 模板加载器
    template_dir = os.path.join(project_root, "src", "main", "resources", "templates")
    env = Environment(loader=FileSystemLoader(template_dir))

    # 3. 加载模板
    template = env.get_template("recommend_prompt.j2")

    # 定义三类 Service 的参数
    services = [
        {
            "feature": "推荐",
            "feature_desc": "基于物品协同过滤的岗位推荐",
            "interface_name": "RecommendService",
            "impl_class": "RecommendServiceImpl",
            "util_class": "SimilarityCalculator",
            "algorithm_desc": """基于相同收藏的"相似岗位"推荐 + 冷启动规则推荐
- 核心算法：基于物品的协同过滤（ItemCF）
- 相似度计算：基于用户收藏行为的 Jaccard 相似度
- 冷启动策略：新用户用"地区+学历"匹配热门岗位
- 缓存策略：Redis 缓存 Top-N 推荐结果（TTL 6小时）""",
            "constraints": [
                "算法实现写在工具类里，Service 只做编排",
                "Redis Key 必须有项目前缀和过期时间",
                "不要引入新的 Maven 依赖"
            ]
        },
        {
            "feature": "搜索",
            "feature_desc": "基于 Elasticsearch 的岗位搜索",
            "interface_name": "SearchService",
            "impl_class": "SearchServiceImpl",
            "util_class": "QueryBuilder",
            "algorithm_desc": """基于 Elasticsearch 的全文检索
- 核心功能：岗位标题、岗位描述、招录单位的多字段搜索
- 排序策略：相关性评分 + 发布时间降序
- 高亮显示：搜索关键词高亮
- 缓存策略：热门搜索词缓存（TTL 1小时）""",
            "constraints": [
                "搜索逻辑写在工具类里",
                "支持分页和排序",
                "空搜索词返回空结果"
            ]
        },
        {
            "feature": "画像",
            "feature_desc": "用户画像标签计算",
            "interface_name": "ProfileService",
            "impl_class": "ProfileServiceImpl",
            "util_class": "TagCalculator",
            "algorithm_desc": """基于用户行为的标签计算
- 核心标签：偏好地区、偏好岗位类型、活跃度等级
- 计算方式：用户收藏 + 搜索历史 + 点击行为
- 更新策略：实时更新 + 离线批量计算
- 存储：Redis Hash 存储用户标签""",
            "constraints": [
                "标签计算写在工具类里",
                "支持多维度标签组合",
                "过期标签自动清理"
            ]
        }
    ]

    # 4. 批量生成
    for service in services:
        params = {
            "tech_domain": "后端",
            "tech_stack": "Spring Boot 2.7.5 + MyBatis-Plus + Redis",
            "experience": "推荐系统落地",
            "project_name": "考公预测系统",
            "feature": service["feature"],
            "feature_desc": service["feature_desc"],
            "code_layer": "核心服务层代码",
            "algorithm_desc": service["algorithm_desc"],
            "project_desc": "智能考公选岗系统",
            "tech_stack_list": "Spring Boot 2.7.5 + MyBatis-Plus + Redis 5",
            "existing_services": "UserService、PositionService、FavoriteService",
            "jdk_version": "11",
            "database": "MySQL 8.0",
            "interface_name": service["interface_name"],
            "impl_class": service["impl_class"],
            "util_class": service["util_class"],
            "config_item": f"{service['feature']}模块配置",
            "config_format": "列表",
            "special_notes": [
                f"{service['feature']}结果需要带来源标记",
                f"{service['feature']}模块有兜底降级机制"
            ]
        }

        # 渲染模板
        prompt = template.render(**params)

        # 保存到文件
        output_dir = os.path.join(project_root, "output")
        os.makedirs(output_dir, exist_ok=True)
        output_file = os.path.join(output_dir, f"prompt_{service['feature']}.txt")

        with open(output_file, "w", encoding="utf-8") as f:
            f.write(prompt)

        print(f"✅ 已生成：{output_file}，长度：{len(prompt)} 字符")

    print("🎉 批量生成完成！")

if __name__ == "__main__":
    main()
