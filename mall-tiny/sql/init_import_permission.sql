-- 初始化数据导入权限
-- 需要在 ums_resource 表中添加导入相关权限，并分配给管理员角色

-- 1. 添加导入权限资源
INSERT INTO ums_resource (id, name, url, description, category_id, create_time)
VALUES 
(1001, 'ums:import:upload', '/admin/import/upload', 'Excel导入上传权限', 1, NOW()),
(1002, 'ums:import:execute', '/admin/import/execute', 'Excel导入执行权限', 1, NOW()),
(1003, 'ums:import:template', '/admin/import/templates', '导入模板管理权限', 1, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), url = VALUES(url), description = VALUES(description);

-- 2. 将权限分配给管理员角色（假设管理员角色ID为1）
-- 先删除已存在的关系，避免重复
DELETE FROM ums_role_resource_relation WHERE role_id = 1 AND resource_id IN (1001, 1002, 1003);

-- 插入新的关系
INSERT INTO ums_role_resource_relation (role_id, resource_id)
VALUES 
(1, 1001),
(1, 1002),
(1, 1003);

-- 3. 如果管理员角色不存在，创建一个默认的管理员角色
INSERT INTO ums_role (id, name, description, admin_count, create_time, status, sort)
VALUES (1, '管理员', '拥有所有权限', 0, NOW(), 1, 0)
ON DUPLICATE KEY UPDATE name = '管理员', description = '拥有所有权限';

-- 4. 给用户分配管理员角色（假设用户ID为1）
-- 先删除已存在的关系，避免重复
DELETE FROM ums_admin_role_relation WHERE admin_id = 1 AND role_id = 1;

-- 插入新的关系
INSERT INTO ums_admin_role_relation (admin_id, role_id) VALUES (1, 1);
