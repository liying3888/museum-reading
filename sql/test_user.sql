-- 创建测试用户
-- 密码: test123, salt: abcde, 校验算法 md5(password+salt)
-- MD5('test123' + 'abcde') = c97871bee35d41abf34924f15f0cfb9e
-- 注：原值 5f4dcc3b... 是 MD5('password')，任何密码均无法登录，2026-11-02 S2 地基修复
INSERT INTO la_user (sn, avatar, real_name, nickname, username, password, mobile, money, salt, sex, channel, is_disable, is_delete, is_new, last_login_ip, last_login_time, create_time, update_time, delete_time)
VALUES (10001, '/api/static/default_avatar.png', '测试用户', '测试用户10001', 'testuser', 'c97871bee35d41abf34924f15f0cfb9e', '13800138000', 0.00, 'abcde', 0, 1, 0, 0, 1, '127.0.0.1', UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), 0)
ON DUPLICATE KEY UPDATE password='c97871bee35d41abf34924f15f0cfb9e', salt='abcde';

SELECT id, sn, nickname, username FROM la_user WHERE username='testuser';
