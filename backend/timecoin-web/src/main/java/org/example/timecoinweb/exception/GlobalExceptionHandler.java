package org.example.timecoinweb.exception;

import org.example.pojo.Result;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public Result handleIllegalState(IllegalStateException ex) {
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result handleDataIntegrityViolationException(DataIntegrityViolationException ex){
        ex.printStackTrace();
        Throwable cause = ex.getMostSpecificCause();
        String msg = cause != null ? cause.getMessage() : "";
        if (msg != null && (msg.contains("UX_USER_USERNAME") || msg.contains("ux_user_username")
                || (msg.contains("Duplicate entry") && msg.toLowerCase().contains("user")))) {
            return Result.error("用户名已存在");
        }
        return Result.error("数据约束错误（如外键或必填字段），请检查是否已配置管理员或数据是否合法");
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public Result handleNoJdbc(CannotGetJdbcConnectionException ex) {
        return Result.error(resolveDbMessage(ex));
    }

    /** 常见于 POST Body 编码错误（如 Windows PowerShell 5 默认用 UTF-16 发 JSON） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleUnreadable(HttpMessageNotReadableException ex) {
        Throwable c = ex.getMostSpecificCause();
        String detail = c != null && c.getMessage() != null ? c.getMessage() : ex.getMessage();
        return Result.error("请求体无法解析为 JSON（请使用 UTF-8 发送 Body，或换用 curl/Postman/PowerShell 7）。详情: " + detail);
    }

    @ExceptionHandler(Exception.class)//捕获所有异常
    public Result ex(Exception ex){
        String db = resolveDbMessage(ex);
        if (db != null) {
            ex.printStackTrace();
            return Result.error(db);
        }
        ex.printStackTrace();
        String detail = ex.getClass().getSimpleName();
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            detail = detail + " — " + ex.getMessage();
        }
        return Result.error("操作失败（详情便于排查，生产环境可改回统一提示）：" + detail);
    }

    /** 从异常链中识别常见数据库问题，返回中文说明；无法识别时返回 null */
    private static String resolveDbMessage(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            String m = t.getMessage();
            if (m != null) {
                if (m.contains("Access denied")) {
                    return "数据库账号或密码错误：请修改 backend/timecoin-web/src/main/resources/application.properties 中的 spring.datasource.username 与 spring.datasource.password，使其与本机 MySQL 一致。";
                }
                if (m.contains("Unknown database")) {
                    return "数据库尚未创建：请在 MySQL 中执行项目内 backend/schema-timecoin.sql（会创建 timecoin 库及表）。";
                }
                if (m.contains("Communications link failure") || m.contains("Connection refused")) {
                    return "无法连接 MySQL：请确认本机 MySQL 服务已启动，且地址端口与 spring.datasource.url 一致（默认 localhost:3306）。";
                }
            }
            t = t.getCause();
        }
        return null;
    }

    /** 从异常链中识别常见数据库问题，返回中文说明；无法识别时返回 null */
    private static String resolveDbMessage(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            String m = t.getMessage();
            if (m != null) {
                if (m.contains("Access denied")) {
                    return "数据库账号或密码错误：请修改 backend/timecoin-web/src/main/resources/application.properties 中的 spring.datasource.username 与 spring.datasource.password，使其与本机 MySQL 一致。";
                }
                if (m.contains("Unknown database")) {
                    return "数据库尚未创建：请在 MySQL 中执行项目内 backend/schema-timecoin.sql（会创建 timecoin 库及表）。";
                }
                if (m.contains("Communications link failure") || m.contains("Connection refused")) {
                    return "无法连接 MySQL：请确认本机 MySQL 服务已启动，且地址端口与 spring.datasource.url 一致（默认 localhost:3306）。";
                }
            }
            t = t.getCause();
        }
        return null;
    }
}
