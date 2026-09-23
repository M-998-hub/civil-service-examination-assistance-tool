-- 考公选岗业务表（用于全新数据库初始化）

CREATE TABLE IF NOT EXISTS `user_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '关联 ums_admin.id',
  `major` varchar(255) DEFAULT NULL COMMENT '专业',
  `education` varchar(50) DEFAULT NULL COMMENT '学历',
  `political_status` varchar(50) DEFAULT NULL COMMENT '政治面貌',
  `is_fresh_graduate` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否应届',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_archive_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户档案表';

CREATE TABLE IF NOT EXISTS `position` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `department` varchar(255) NOT NULL COMMENT '招录部门',
  `position_name` varchar(255) NOT NULL COMMENT '职位名称',
  `major_required` varchar(500) DEFAULT NULL COMMENT '专业要求',
  `education_required` varchar(100) DEFAULT NULL COMMENT '学历要求',
  `political_status_required` varchar(100) DEFAULT NULL COMMENT '政治面貌要求',
  `is_fresh_only` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否限应届',
  `recruitment_number` int NOT NULL DEFAULT 0 COMMENT '招录人数',
  `registration_deadline` datetime DEFAULT NULL COMMENT '报名截止时间',
  `year` int NOT NULL COMMENT '招录年份',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0正常 1删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_position_year` (`year`),
  KEY `idx_position_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

CREATE TABLE IF NOT EXISTS `position_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `year` int NOT NULL,
  `department` varchar(255) NOT NULL,
  `position_name` varchar(255) NOT NULL,
  `registration_count` int DEFAULT NULL COMMENT '报名人数',
  `min_entry_score` decimal(6,2) DEFAULT NULL COMMENT '最低进面分',
  `max_entry_score` decimal(6,2) DEFAULT NULL COMMENT '最高进面分',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_year_dept_position` (`year`, `department`, `position_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报录比与分数线表';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `position_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_position` (`user_id`, `position_id`),
  KEY `idx_favorite_position` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位收藏表';

CREATE TABLE IF NOT EXISTS `prediction_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `position_id` bigint NOT NULL,
  `simulate_score` decimal(6,2) DEFAULT NULL,
  `probability` decimal(6,4) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_prediction_user` (`user_id`),
  KEY `idx_prediction_position` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预测记录表';

CREATE TABLE IF NOT EXISTS `import_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_name` varchar(100) NOT NULL,
  `import_type` varchar(50) NOT NULL DEFAULT 'position',
  `description` varchar(500) DEFAULT NULL,
  `column_mapping` text NOT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入模板表';
