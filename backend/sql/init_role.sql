-- 初始化角色数据
-- 创建默认「管理员」角色

-- 1. 插入管理员角色（如果不存在）
INSERT INTO ums_role (name, description, admin_count, create_time, status, sort)
SELECT '管理员', '拥有所有权限，可管理系统所有功能', 0, NOW(), 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ums_role WHERE name = '管理员');

-- 2. 将管理员角色分配给 admin 用户（假设 admin 用户 id = 1）
-- 先获取管理员角色的 id
SET @role_id = (SELECT id FROM ums_role WHERE name = '管理员' LIMIT 1);

-- 插入关联关系（如果不存在）
INSERT INTO ums_admin_role_relation (admin_id, role_id)
SELECT 1, @role_id
FROM DUAL
WHERE @role_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ums_admin_role_relation WHERE admin_id = 1 AND role_id = @role_id);

-- 3. 可选：创建其他常用角色
INSERT INTO ums_role (name, description, admin_count, create_time, status, sort)
SELECT '普通用户', '普通用户，只能查看岗位和匹配', 0, NOW(), 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ums_role WHERE name = '普通用户');

INSERT INTO ums_role (name, description, admin_count, create_time, status, sort)
SELECT '数据录入员', '负责岗位数据录入和管理', 0, NOW(), 1, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ums_role WHERE name = '数据录入员');
