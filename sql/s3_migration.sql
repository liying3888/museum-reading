-- S3 批 2 migration —— docs/S3/01-接口契约与DDL草案.md §1（leader 2026-11-04 过闸版）
-- 全部 DEFAULT 加列/建新表：存量行自动取缺省，零回填；幂等性注意 ADD COLUMN 重复执行会报错，
-- 236 由 ops70 init 流程拼合执行，本机由 museum 手工执行一次。

-- 1) la_reading_record +3 列（真暂停+自动结束承载，02 §0.3/§5.1）
ALTER TABLE la_reading_record
  ADD COLUMN paused_at INT NOT NULL DEFAULT 0 COMMENT '当前暂停起点时间戳(0=非暂停态)' AFTER end_time,
  ADD COLUMN paused_total INT NOT NULL DEFAULT 0 COMMENT '已累计暂停秒数' AFTER paused_at,
  ADD COLUMN auto_closed TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动结束: 0=正常, 1=240分钟自动结束' AFTER paused_total,
  ADD INDEX idx_open (end_time, start_time);

-- 2) la_bookshelf +置顶列（02 §4.1：长按置顶，排序=置顶优先+update_time 倒序）
ALTER TABLE la_bookshelf
  ADD COLUMN pinned TINYINT NOT NULL DEFAULT 0 COMMENT '置顶: 0=否, 1=是' AFTER progress,
  ADD INDEX idx_user_status (user_id, status, update_time);

-- 3) 新表 la_user_reading_config（每日目标单一数据源 10-240 分钟默认 30，02 §0.1；不动底座 la_user）
CREATE TABLE IF NOT EXISTS la_user_reading_config (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL COMMENT '用户ID',
  daily_goal INT NOT NULL DEFAULT 30 COMMENT '每日目标分钟数(10-240)',
  create_time INT,
  update_time INT,
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
