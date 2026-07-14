CREATE TABLE IF NOT EXISTS `bot_evaluation_report` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `bot_id` int NOT NULL,
  `bot_name` varchar(100) NOT NULL,
  `mode` varchar(32) NOT NULL,
  `report_json` longtext NOT NULL,
  `analysis_status` varchar(32) NOT NULL DEFAULT 'pending',
  `analysis_findings` text,
  `analysis_weaknesses` text,
  `analysis_suggestions` text,
  `optimized_code` longtext,
  `createtime` datetime NOT NULL,
  `modifytime` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `createtime`),
  KEY `idx_user_bot_time` (`user_id`, `bot_id`, `createtime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
