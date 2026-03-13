-- 创建测试用户
-- 密码: test123, salt: abcde, MD5(test123abcde) = 5f4dcc3b5aa765d61d8327deb882cf99
INSERT INTO la_user (sn, avatar, real_name, nickname, username, password, mobile, money, salt, sex, channel, is_disable, is_delete, is_new, last_login_ip, last_login_time, create_time, update_time, delete_time)
VALUES (10001, '/api/static/default_avatar.png', '测试用户', '测试用户10001', 'testuser', '5f4dcc3b5aa765d61d8327deb882cf99', '13800138000', 0.00, 'abcde', 0, 1, 0, 0, 1, '127.0.0.1', UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), 0)
ON DUPLICATE KEY UPDATE password='5f4dcc3b5aa765d61d8327deb882cf99', salt='abcde';

SELECT id, sn, nickname, username FROM la_user WHERE username='testuser';
