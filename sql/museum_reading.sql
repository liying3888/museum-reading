-- 博物馆读书小程序数据库表

-- 1. 书籍表
CREATE TABLE IF NOT EXISTS la_book (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(100),
  publisher VARCHAR(100),
  isbn VARCHAR(20),
  cover VARCHAR(500),
  intro TEXT,
  pages INT DEFAULT 0,
  rating DECIMAL(2,1) DEFAULT 0,
  rating_count INT DEFAULT 0,
  category_id INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time INT,
  update_time INT,
  INDEX idx_category (category_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 书架表
CREATE TABLE IF NOT EXISTS la_bookshelf (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  book_id INT NOT NULL,
  status TINYINT DEFAULT 0,
  current_page INT DEFAULT 0,
  progress INT DEFAULT 0,
  start_time INT,
  finish_time INT,
  create_time INT,
  update_time INT,
  UNIQUE KEY uk_user_book (user_id, book_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 阅读记录表
CREATE TABLE IF NOT EXISTS la_reading_record (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  book_id INT,
  start_time INT NOT NULL,
  end_time INT,
  duration INT DEFAULT 0,
  note VARCHAR(500),
  create_time INT,
  INDEX idx_user_time (user_id, create_time),
  INDEX idx_book (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
