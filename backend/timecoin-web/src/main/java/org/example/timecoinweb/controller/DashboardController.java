package org.example.timecoinweb.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.pojo.Result;
import org.example.timecoinweb.service.ActivityService;
import org.example.timecoinweb.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard 统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    /**
     * 获取首页统计数据
     * @return 汇总后的统计结果
     */
    @GetMapping("/stats")
    public Result getHomeStats() {
        log.info("获取首页统计数据");
        Map<String, Object> responseData = new HashMap<>();
        
        // 获取用户统计
        responseData.put("userStats", userService.getUserStats());
        
        // 获取活动统计
        responseData.put("activityStats", activityService.getActivityStats());
        
        return Result.success(responseData);
    }
}
