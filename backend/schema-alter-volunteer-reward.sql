-- 已部署的 TimeCoin 库升级：每人志愿者答谢与防重复支付标记
-- 在 USE timecoin; 后执行（列已存在时会报错，可忽略或先检查 information_schema）

ALTER TABLE activity
  ADD COLUMN volunteer_reward INT NOT NULL DEFAULT 0 COMMENT '完成后老人向每名志愿者答谢的时间币数量';

ALTER TABLE activity_volunteer
  ADD COLUMN reward_paid SMALLINT NOT NULL DEFAULT 0 COMMENT '0未付答谢 1已付';
