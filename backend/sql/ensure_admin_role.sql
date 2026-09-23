-- 确保管理员角色存在
-- 如果不存在则插入，存在则忽略

-- 1. 确保管理员角色存在
INSERT INTO ums_role (id, name, description, admin_count, create_time, status, sort)
SELECT 1, '管理员', '拥有所有权限', 0, NOW(), 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ums_role WHERE id = 1);

-- 2. 查看现有用户
SELECT a.id, a.username, r.name as role_name 
FROM ums_admin a 
LEFT JOIN ums_admin_role_relation arr ON a.id = arr.admin_id 
LEFT JOIN ums_role r ON arr.role_id = r.id;

-- 3. 给指定用户分配管理员角色（替换 admin_id 为实际用户ID）
-- 先删除该用户的旧角色关系
-- DELETE FROM ums_admin_role_relation WHERE admin_id = 1;

-- 插入管理员角色关系
-- INSERT INTO ums_admin_role_relation (admin_id, role_id) VALUES (1, 1);
