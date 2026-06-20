package org.example.timecoinweb.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.pojo.CompensationRecord;
import org.example.pojo.Result;
import org.example.timecoinweb.mapper.CompensationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/administrator/compensation")
public class CompensationController {

    @Autowired
    private CompensationMapper compensationMapper;

    @GetMapping("/list")
    public Result getList(@RequestParam(required = false) Short status) {
        try {
            List<CompensationRecord> list;
            if (status != null) {
                if (status == 0) {
                    list = compensationMapper.selectPending();
                } else {
                    list = compensationMapper.selectAll().stream()
                            .filter(r -> r.getStatus() != null && r.getStatus().equals(status))
                            .collect(Collectors.toList());
                }
            } else {
                list = compensationMapper.selectAll();
            }
            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            return Result.success(data);
        } catch (Exception e) {
            log.warn("查询追讨记录列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/process/{id}")
    public Result processRecord(@PathVariable Integer id, @RequestBody Map<String, Object> request) {
        try {
            Short status = Short.valueOf(request.get("status").toString());
            if (status != 1 && status != 2) {
                return Result.error("状态值无效：1 已追讨，2 平台核销");
            }
            compensationMapper.updateStatus(id, status, LocalDateTime.now());
            return Result.success(null);
        } catch (Exception e) {
            log.warn("处理追讨记录失败 id={}", id, e);
            return Result.error("处理失败: " + e.getMessage());
        }
    }
}
