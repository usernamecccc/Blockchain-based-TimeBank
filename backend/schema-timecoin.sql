-- TimeCoin 后端 MySQL 初始化脚本（与 MyBatis 映射一致）
-- 使用方式：mysql -u root -p < schema-timecoin.sql
-- 或先 CREATE DATABASE timecoin; USE timecoin; 再执行下方语句

CREATE DATABASE IF NOT EXISTS timecoin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE timecoin;

DROP TABLE IF EXISTS activity_volunteer;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS volunteer;
DROP TABLE IF EXISTS old;
DROP TABLE IF EXISTS administrator;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) DEFAULT NULL,
  password VARCHAR(255) NOT NULL,
  role SMALLINT NOT NULL COMMENT '1老人 2志愿者 3管理员',
  email VARCHAR(128) DEFAULT NULL,
  age SMALLINT DEFAULT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  image VARCHAR(512) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE old (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  CONSTRAINT fk_old_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE administrator (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  CONSTRAINT fk_admi_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE volunteer (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  CONSTRAINT fk_vol_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE activity (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  quota SMALLINT NOT NULL,
  deadline DATETIME NOT NULL,
  date DATE NOT NULL,
  begin TIME NOT NULL,
  end TIME NOT NULL,
  address VARCHAR(255) NOT NULL,
  old_id INT NOT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  description TEXT,
  status SMALLINT NOT NULL DEFAULT 1,
  administrator_id INT NOT NULL,
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  message VARCHAR(512) DEFAULT NULL,
  remain SMALLINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_act_old FOREIGN KEY (old_id) REFERENCES old(id),
  CONSTRAINT fk_act_admi FOREIGN KEY (administrator_id) REFERENCES administrator(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notice (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL DEFAULT '',
  content TEXT NOT NULL,
  create_time DATETIME DEFAULT NULL,
  INDEX idx_notice_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE activity_volunteer (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  activity_id INT NOT NULL,
  volunteer_id INT NOT NULL,
  status SMALLINT DEFAULT NULL,
  sign SMALLINT DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  UNIQUE KEY uk_act_vol (activity_id, volunteer_id),
  CONSTRAINT fk_av_act FOREIGN KEY (activity_id) REFERENCES activity(id) ON DELETE CASCADE,
  CONSTRAINT fk_av_vol FOREIGN KEY (volunteer_id) REFERENCES volunteer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认管理员（老人发布活动需要 administrator 外键；PC 端可用 admin / 123456 登录）
INSERT INTO user (username, name, password, role, email, age, phone, create_time, update_time)
VALUES ('admin', '系统管理员', '123456', 3, 'admin@local', 30, '13800000000', NOW(), NOW());
INSERT INTO administrator (user_id) SELECT id FROM user WHERE username = 'admin';
