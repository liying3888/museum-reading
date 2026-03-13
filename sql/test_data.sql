-- Day 3 测试数据

-- 插入测试书籍
INSERT INTO la_book (title, author, publisher, cover, intro, pages, rating, rating_count, category_id, status, create_time, update_time) VALUES
('百年孤独', '加西亚·马尔克斯', '南海出版公司', 'https://example.com/book1.jpg', '魔幻现实主义文学代表作', 360, 9.3, 12580, 1, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('三体', '刘慈欣', '重庆出版社', 'https://example.com/book2.jpg', '中国科幻小说巅峰之作', 302, 9.5, 25680, 1, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('人类简史', '尤瓦尔·赫拉利', '中信出版社', 'https://example.com/book3.jpg', '从动物到上帝的人类历史', 440, 9.1, 18920, 1, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('活着', '余华', '作家出版社', 'https://example.com/book4.jpg', '中国当代文学的杰作', 191, 9.4, 32010, 1, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('明朝那些事儿', '当年明月', '中国海关出版社', 'https://example.com/book5.jpg', '以史料为基础的历史小说', 1700, 9.0, 28560, 1, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- 插入测试书架数据
INSERT INTO la_bookshelf (user_id, book_id, status, current_page, progress, start_time, finish_time, create_time, update_time) VALUES
(1, 1, 1, 120, 33, UNIX_TIMESTAMP() - 86400*7, NULL, UNIX_TIMESTAMP() - 86400*7, UNIX_TIMESTAMP()),
(1, 2, 2, 302, 100, UNIX_TIMESTAMP() - 86400*5, UNIX_TIMESTAMP(), UNIX_TIMESTAMP() - 86400*5, UNIX_TIMESTAMP()),
(1, 3, 1, 220, 50, UNIX_TIMESTAMP() - 86400*3, NULL, UNIX_TIMESTAMP() - 86400*3, UNIX_TIMESTAMP()),
(1, 4, 0, 0, 0, NULL, NULL, UNIX_TIMESTAMP() - 86400*2, UNIX_TIMESTAMP() - 86400*2);

-- 插入测试阅读记录（最近7天）
-- 今天
INSERT INTO la_reading_record (user_id, book_id, start_time, end_time, duration, note, create_time) VALUES
(1, 1, UNIX_TIMESTAMP() - 3600, UNIX_TIMESTAMP(), 60, '今天继续读百年孤独', UNIX_TIMESTAMP()),
(1, 3, UNIX_TIMESTAMP() - 7200, UNIX_TIMESTAMP() - 3600, 60, '人类简史第三章', UNIX_TIMESTAMP());

-- 昨天
INSERT INTO la_reading_record (user_id, book_id, start_time, end_time, duration, note, create_time) VALUES
(1, 1, UNIX_TIMESTAMP() - 86400 - 3600, UNIX_TIMESTAMP() - 86400, 60, '继续阅读', UNIX_TIMESTAMP() - 86400),
(1, 2, UNIX_TIMESTAMP() - 86400 - 7200, UNIX_TIMESTAMP() - 86400 - 5400, 30, '三体大结局', UNIX_TIMESTAMP() - 86400);

-- 2天前
INSERT INTO la_reading_record (user_id, book_id, start_time, end_time, duration, note, create_time) VALUES
(1, 1, UNIX_TIMESTAMP() - 86400*2 - 5400, UNIX_TIMESTAMP() - 86400*2 - 1800, 60, '马孔多的故事', UNIX_TIMESTAMP() - 86400*2);

-- 3天前
INSERT INTO la_reading_record (user_id, book_id, start_time, end_time, duration, note, create_time) VALUES
(1, 3, UNIX_TIMESTAMP() - 86400*3 - 7200, UNIX_TIMESTAMP() - 86400*3 - 3600, 60, '认知革命', UNIX_TIMESTAMP() - 86400*3);

-- 4天前
INSERT INTO la_reading_record (user_id, book_id, start_time, end_time, duration, note, create_time) VALUES
(1, 2, UNIX_TIMESTAMP() - 86400*4 - 5400, UNIX_TIMESTAMP() - 86400*4, 90, '黑暗森林', UNIX_TIMESTAMP() - 86400*4);

-- 5天前
INSERT INTO la_reading_record (user_id, book_id, start_time, end_time, duration, note, create_time) VALUES
(1, 2, UNIX_TIMESTAMP() - 86400*5 - 3600, UNIX_TIMESTAMP() - 86400*5, 60, '三体第二部', UNIX_TIMESTAMP() - 86400*5);

-- 6天前
INSERT INTO la_reading_record (user_id, book_id, start_time, end_time, duration, note, create_time) VALUES
(1, 1, UNIX_TIMESTAMP() - 86400*6 - 7200, UNIX_TIMESTAMP() - 86400*6 - 5400, 30, '布恩迪亚家族', UNIX_TIMESTAMP() - 86400*6);

-- 7天前
INSERT INTO la_reading_record (user_id, book_id, start_time, end_time, duration, note, create_time) VALUES
(1, 1, UNIX_TIMESTAMP() - 86400*7 - 5400, UNIX_TIMESTAMP() - 86400*7 - 1800, 60, '开始读百年孤独', UNIX_TIMESTAMP() - 86400*7);
