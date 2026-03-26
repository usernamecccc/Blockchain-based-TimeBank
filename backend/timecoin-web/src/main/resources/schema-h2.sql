-- H2（MySQL 模式）；保留字用反引号（与 MySQL 习惯一致）
DROP TABLE IF EXISTS activity_volunteer;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS volunteer;
DROP TABLE IF EXISTS old;
DROP TABLE IF EXISTS administrator;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  name VARCHAR(64) DEFAULT NULL,
  password VARCHAR(255) NOT NULL,
  role SMALLINT NOT NULL,
  email VARCHAR(128) DEFAULT NULL,
  age SMALLINT DEFAULT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  image VARCHAR(512) DEFAULT NULL
);
CREATE UNIQUE INDEX ux_user_username ON `user`(username);

CREATE TABLE old (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  CONSTRAINT fk_old_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_old_user ON old(user_id);

CREATE TABLE administrator (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  CONSTRAINT fk_admi_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_admi_user ON administrator(user_id);

CREATE TABLE volunteer (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  CONSTRAINT fk_vol_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_vol_user ON volunteer(user_id);

CREATE TABLE activity (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  quota SMALLINT NOT NULL,
  deadline TIMESTAMP NOT NULL,
  date DATE NOT NULL,
  `begin` TIME NOT NULL,
  `end` TIME NOT NULL,
  address VARCHAR(255) NOT NULL,
  old_id INT NOT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  description CLOB,
  status SMALLINT NOT NULL DEFAULT 1,
  administrator_id INT NOT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  message VARCHAR(512) DEFAULT NULL,
  remain SMALLINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_act_old FOREIGN KEY (old_id) REFERENCES old(id),
  CONSTRAINT fk_act_admi FOREIGN KEY (administrator_id) REFERENCES administrator(id)
);

CREATE TABLE activity_volunteer (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  activity_id INT NOT NULL,
  volunteer_id INT NOT NULL,
  status SMALLINT DEFAULT NULL,
  sign SMALLINT DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  CONSTRAINT uk_act_vol UNIQUE (activity_id, volunteer_id),
  CONSTRAINT fk_av_act FOREIGN KEY (activity_id) REFERENCES activity(id) ON DELETE CASCADE,
  CONSTRAINT fk_av_vol FOREIGN KEY (volunteer_id) REFERENCES volunteer(id) ON DELETE CASCADE
);
