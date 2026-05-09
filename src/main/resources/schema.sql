CREATE TABLE IF NOT EXISTS video_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    file_path VARCHAR(512) NOT NULL COMMENT '文件路径',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型(image/video)',
    text_content TEXT COMMENT '文字内容',
    third_party_service VARCHAR(64) COMMENT '第三方服务',
    status VARCHAR(32) DEFAULT 'pending' COMMENT '状态(pending/processing/completed/failed)',
    result_url VARCHAR(512) COMMENT '结果URL',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频任务表';