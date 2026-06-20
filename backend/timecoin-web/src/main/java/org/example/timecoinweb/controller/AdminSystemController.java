package org.example.timecoinweb.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.pojo.Result;
import org.example.timecoinweb.service.SystemMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统管理：真实主机指标（路径须含 administrator 以通过管理员校验）。
 */
@Slf4j
@RestController
@RequestMapping("/administrator/system")
public class AdminSystemController {

    @Autowired
    private SystemMetricsService systemMetricsService;

    @GetMapping("/metrics")
    public Result metrics() {
        try {
            Map<String, Object> data = systemMetricsService.snapshot();
            return Result.success(data);
        } catch (Exception e) {
            log.warn("采集系统指标失败", e);
            return Result.error("采集系统指标失败: " + e.getMessage());
        }
    }
}
