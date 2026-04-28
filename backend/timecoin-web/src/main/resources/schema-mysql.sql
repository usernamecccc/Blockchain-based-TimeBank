-- MySQL 幂等脚本：与 backend/schema-timecoin.sql 结构一致，可重复执行
-- 由 application.properties 在「未使用 dev 配置」时自动执行

CREATE TABLE IF NOT EXISTS `user` (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  name VARCHAR(64) DEFAULT NULL,
  password VARCHAR(255) NOT NULL,
  role SMALLINT NOT NULL,
  email VARCHAR(128) DEFAULT NULL,
  age SMALLINT DEFAULT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  image VARCHAR(512) DEFAULT NULL,
  UNIQUE KEY ux_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS old (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  CONSTRAINT fk_old_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS administrator (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  CONSTRAINT fk_admi_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS volunteer (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  CONSTRAINT fk_vol_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS activity (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  quota SMALLINT NOT NULL,
  deadline DATETIME NOT NULL,
  date DATE NOT NULL,
  `begin` TIME NOT NULL,
  `end` TIME NOT NULL,
  address VARCHAR(255) NOT NULL,
  old_id INT NOT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  description TEXT,
  -- status: 1待审核 2审核通过 3进行中 4拒绝 5活动过期
  status SMALLINT NOT NULL DEFAULT 1,
  administrator_id INT NOT NULL,
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  message VARCHAR(512) DEFAULT NULL,
  remain SMALLINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_act_old FOREIGN KEY (old_id) REFERENCES old(id),
  CONSTRAINT fk_act_admi FOREIGN KEY (administrator_id) REFERENCES administrator(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notice (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL DEFAULT '',
  content TEXT NOT NULL,
  create_time DATETIME DEFAULT NULL,
  INDEX idx_notice_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS activity_volunteer (
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

INSERT INTO `user` (username, name, password, role, email, age, phone, create_time, update_time)
SELECT 'admin', '系统管理员', '123456', 3, 'admin@local', 30, '13800000000', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'admin');
INSERT INTO administrator (user_id)
SELECT u.id FROM `user` u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM administrator a WHERE a.user_id = u.id);
