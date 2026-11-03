-- ops70 20261103 卡004 复核：S3 DDL 落位状态
SELECT CONCAT('record-col: ', column_name, ' ', column_type) FROM information_schema.columns
 WHERE table_schema='likeadmin_java' AND table_name='la_reading_record'
   AND column_name IN ('paused_at','paused_total','auto_closed');
SELECT CONCAT('record-idx: ', index_name) FROM information_schema.statistics
 WHERE table_schema='likeadmin_java' AND table_name='la_reading_record' AND index_name='idx_open';
SELECT CONCAT('bookshelf-pinned: ', column_type) FROM information_schema.columns
 WHERE table_schema='likeadmin_java' AND table_name='la_bookshelf' AND column_name='pinned';
SELECT CONCAT('bookshelf-idx: ', index_name) FROM information_schema.statistics
 WHERE table_schema='likeadmin_java' AND table_name='la_bookshelf' AND index_name='idx_user_status';
SELECT CONCAT('newtable: ', table_name, ' rows_desc=', table_rows) FROM information_schema.tables
 WHERE table_schema='likeadmin_java' AND table_name='la_user_reading_config';
SELECT CONCAT('comment-hex: ', HEX(SUBSTRING(column_comment,1,12))) FROM information_schema.columns
 WHERE table_schema='likeadmin_java' AND table_name='la_reading_record' AND column_name='paused_at';
