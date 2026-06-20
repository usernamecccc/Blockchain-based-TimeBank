package org.example.timecoinweb.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.pojo.Notice;
import org.example.pojo.Result;
import org.example.timecoinweb.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员发布公告（路径须含 administrator 以通过登录与角色校验）
 */
@Slf4j
@RestController
@RequestMapping("/administrator/notices")
public class NoticeAdminController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping
    public Result list() {
        List<Notice> list = noticeService.listAll();
        return Result.success(list);
    }

    @PostMapping
    public Result add(@RequestBody Notice notice) {
        if (notice.getContent() == null || notice.getContent().trim().isEmpty()) {
            return Result.error("公告正文不能为空");
        }
        if (notice.getTitle() == null) {
            notice.setTitle("");
        }
        noticeService.add(notice);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Notice notice) {
        if (notice.getId() == null) {
            return Result.error("缺少公告 id");
        }
        if (notice.getContent() == null || notice.getContent().trim().isEmpty()) {
            return Result.error("公告正文不能为空");
        }
        if (notice.getTitle() == null) {
            notice.setTitle("");
        }
        noticeService.update(notice);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable int id) {
        noticeService.deleteById(id);
        return Result.success();
    }
}
