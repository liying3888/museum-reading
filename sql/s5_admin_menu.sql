-- S5 批 5：admin 内容管理菜单树（板 09 侧栏：数据看板/书籍管理/分类管理）
-- 权限码口径：跟 article 域惯例（xxx:list/detail/add/edit/del）；接口层底座无注解校验（登录态+前端 v-perms）
-- 幂等：按 perms 判存在跳过，重复执行安全

INSERT INTO la_system_auth_menu
    (id, pid, menu_type, menu_name, menu_icon, menu_sort, perms, paths, component, selected, params, is_cache, is_show, is_disable, create_time, update_time)
SELECT t.id, t.pid, t.menu_type, t.menu_name, t.menu_icon, t.menu_sort, t.perms, t.paths, t.component, t.selected, t.params, t.is_cache, t.is_show, t.is_disable, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()
FROM (
    SELECT 810 AS id, 0   AS pid, 'M' AS menu_type, '内容管理' AS menu_name, '' AS menu_icon, 45 AS menu_sort, '' AS perms, 'book' AS paths, '' AS component, '' AS selected, '' AS params, 0 AS is_cache, 1 AS is_show, 0 AS is_disable UNION ALL
    SELECT 811, 810, 'C', '数据看板', '', 0, 'statistics:board',  'board',    'book/board/index',    '', '', 0, 1, 0 UNION ALL
    SELECT 812, 810, 'C', '书籍管理', '', 1, 'book:list',         'books',    'book/books/index',    '', '', 0, 1, 0 UNION ALL
    SELECT 813, 810, 'C', '分类管理', '', 2, 'book:cate:list',    'category', 'book/category/index', '', '', 0, 1, 0 UNION ALL
    SELECT 814, 812, 'A', '书籍详情', '', 0, 'book:detail',    '', '', '', '', 0, 1, 0 UNION ALL
    SELECT 815, 812, 'A', '书籍新增', '', 0, 'book:add',       '', '', '', '', 0, 1, 0 UNION ALL
    SELECT 816, 812, 'A', '书籍编辑', '', 0, 'book:edit',      '', '', '', '', 0, 1, 0 UNION ALL
    SELECT 817, 812, 'A', '书籍删除', '', 0, 'book:delete',    '', '', '', '', 0, 1, 0 UNION ALL
    SELECT 818, 813, 'A', '分类详情', '', 0, 'book:cate:detail', '', '', '', '', 0, 1, 0 UNION ALL
    SELECT 819, 813, 'A', '分类新增', '', 0, 'book:cate:add',    '', '', '', '', 0, 1, 0 UNION ALL
    SELECT 820, 813, 'A', '分类编辑', '', 0, 'book:cate:edit',   '', '', '', '', 0, 1, 0 UNION ALL
    SELECT 821, 813, 'A', '分类停用', '', 0, 'book:cate:del',    '', '', '', '', 0, 1, 0
) AS t
WHERE NOT EXISTS (SELECT 1 FROM la_system_auth_menu m WHERE m.perms = t.perms AND t.perms != '');

-- 超管角色挂全菜单（likeadmin 惯例：role_id=0 为超管，通常全量可见；若库中另有角色绑定表，按需补 la_system_auth_role_menu）
