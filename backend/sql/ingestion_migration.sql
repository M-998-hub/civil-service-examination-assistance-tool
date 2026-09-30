-- Existing deployments: execute this migration once before starting the new backend.
ALTER TABLE `position`
  ADD COLUMN `source_type` varchar(30) DEFAULT NULL,
  ADD COLUMN `source_position_code` varchar(100) DEFAULT NULL,
  ADD COLUMN `source_document_id` bigint DEFAULT NULL,
  ADD COLUMN `recruitment_status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  ADD UNIQUE KEY `uk_position_source_code` (`year`, `source_type`, `source_position_code`);

CREATE TABLE IF NOT EXISTS `ingestion_document` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_url` varchar(2048) NOT NULL,
  `sha256` char(64) NOT NULL,
  `file_path` varchar(1024) NOT NULL,
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ingestion_document_hash` (`sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ingestion_run` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `year` int NOT NULL,
  `source_url` varchar(2048) DEFAULT NULL,
  `document_id` bigint DEFAULT NULL,
  `state` varchar(30) NOT NULL,
  `message` varchar(1000) DEFAULT NULL,
  `candidate_count` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ingestion_run_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ingestion_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `run_id` bigint NOT NULL,
  `position_code` varchar(100) NOT NULL,
  `change_type` varchar(20) NOT NULL,
  `state` varchar(20) NOT NULL DEFAULT 'PENDING',
  `data_json` text NOT NULL,
  `previous_json` text DEFAULT NULL,
  `validation_error` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ingestion_candidate_code` (`run_id`, `position_code`),
  KEY `idx_ingestion_candidate_run` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `position_revision` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `position_id` bigint NOT NULL,
  `run_id` bigint NOT NULL,
  `change_type` varchar(20) NOT NULL,
  `before_json` text DEFAULT NULL,
  `after_json` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_position_revision_position` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
