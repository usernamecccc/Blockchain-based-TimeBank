-- 一次性迁移：在备份库后手工执行。
-- ⚠ 仅当库内仍是「旧版」语义时执行：1待审 2混用 3拒绝 4过期 5草稿。
-- 若当前已是新版（5=活动过期），执行本脚本会误把过期活动改成待审核。

START TRANSACTION;

-- 历史遗留：曾用 status=6 表示草稿时，并入待审核
UPDATE activity SET status = 1 WHERE status = 6;

-- 旧版「5=草稿」→ 待审核（必须在「4→5」之前执行）
UPDATE activity SET status = 1 WHERE status = 5;

UPDATE activity SET status = 5 WHERE status = 4;

UPDATE activity SET status = 4 WHERE status = 3;

UPDATE activity SET status = CASE
  WHEN CONCAT(`date`, ' ', `end`) < NOW() THEN 5
  WHEN CONCAT(`date`, ' ', `begin`) <= NOW() THEN 3
  ELSE 2
END
WHERE status = 2;

COMMIT;
