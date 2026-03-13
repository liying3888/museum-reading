-- 书籍分类表
CREATE TABLE IF NOT EXISTS la_book_category (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '分类名称',
  icon VARCHAR(500) COMMENT '分类图标',
  sort INT DEFAULT 0 COMMENT '排序',
  status TINYINT DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
  create_time INT COMMENT '创建时间',
  update_time INT COMMENT '更新时间',
  INDEX idx_status (status),
  INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书籍分类表';

-- 插入一些测试数据
INSERT INTO la_book_category (name, icon, sort, status, create_time, update_time) VALUES
('文学', '', 1, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('历史', '', 2, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('科学', '', 3, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('艺术', '', 4, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('哲学', '', 5, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());
