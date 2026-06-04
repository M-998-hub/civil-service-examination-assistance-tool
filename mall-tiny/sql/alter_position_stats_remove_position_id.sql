-- position_stats 表结构调整：去掉 position_id，新增 department 和 position_name

-- 删除旧字段
ALTER TABLE position_stats DROP COLUMN IF EXISTS position_id;

-- 新增字段
ALTER TABLE position_stats
    ADD COLUMN department   VARCHAR(255) NOT NULL DEFAULT '' COMMENT '招录部门' AFTER year,
    ADD COLUMN position_name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '职位名称' AFTER department;

-- 新增唯一索引（年份+部门+职位名称去重）
ALTER TABLE position_stats
    ADD UNIQUE INDEX uk_year_dept_position (year, department, position_name);
