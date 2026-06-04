-- 为 import_template 表添加 import_type 字段
ALTER TABLE import_template
    ADD COLUMN import_type VARCHAR(50) DEFAULT 'position' COMMENT '导入类型（position/position_stats）'
    AFTER template_name;

-- 将已有模板的 import_type 设为默认值 position
UPDATE import_template SET import_type = 'position' WHERE import_type IS NULL;
