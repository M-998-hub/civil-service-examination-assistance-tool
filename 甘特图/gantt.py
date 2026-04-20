import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

# 配置中文显示
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'PingFang SC']
plt.rcParams['axes.unicode_minus'] = False

# 定义任务（任务名称，开始周，持续周数）
tasks = [
    ("项目管理", 1, 8),
    ("需求与设计", 1, 2),
    ("用户系统", 2, 2),
    ("数据管理", 2, 2),
    ("智能匹配", 3, 2),
    ("统计分析", 4, 2),
    ("决策辅助", 5, 2),
    ("互动与工具", 5, 2),
    ("测试", 6, 2),
    ("部署与上线", 8, 1),
]

# 创建画布
fig, ax = plt.subplots(figsize=(14, 6))

# 为每个任务绘制条形
for i, (name, start, duration) in enumerate(tasks):
    ax.barh(y=i, width=duration, left=start, height=0.5, 
            color='#2E86AB', edgecolor='white', linewidth=1)
    ax.text(start + duration/2, i, name, 
            ha='center', va='center', color='white', fontsize=9, fontweight='bold')

# 设置坐标轴
ax.set_xlim(0, 9)
ax.set_ylim(-0.5, len(tasks)-0.5)
ax.set_xlabel('项目周数', fontsize=11)
ax.set_title('考公选岗预测系统开发甘特图', fontsize=14, fontweight='bold', pad=20)

# 设置周刻度
ax.set_xticks(range(1, 9))
ax.set_xticklabels([f'W{i}' for i in range(1, 9)])

# 隐藏y轴刻度线，只保留任务名称
ax.set_yticks(range(len(tasks)))
ax.set_yticklabels([t[0] for t in tasks])
ax.invert_yaxis()

# 添加网格线
ax.grid(axis='x', alpha=0.3, linestyle='--')

# 添加图例
legend_patch = mpatches.Patch(color='#2E86AB', label='任务周期')
ax.legend(handles=[legend_patch], loc='lower right')

# 添加里程碑标记
milestones = [(2, "设计完成"), (4, "核心功能完成"), (7, "测试完成"), (8.5, "项目上线")]
for x, label in milestones:
    ax.scatter(x, -0.8, color='red', s=100, zorder=5, marker='v')
    ax.annotate(label, (x, -0.5), ha='center', fontsize=8, color='red')

plt.tight_layout()
plt.savefig('gantt_chart.png', dpi=150, bbox_inches='tight')
plt.show()
print("甘特图已保存为 gantt_chart.png")