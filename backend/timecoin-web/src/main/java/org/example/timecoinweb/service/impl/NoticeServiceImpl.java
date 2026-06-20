package org.example.timecoinweb.service.impl;

import org.example.pojo.Notice;
import org.example.timecoinweb.mapper.NoticeMapper;
import org.example.timecoinweb.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    @Autowired
    private NoticeMapper noticeMapper;

    @Override
    public List<Notice> listRecent(int limit) {
        int n = Math.min(Math.max(limit, 1), 100);
        return noticeMapper.listRecent(n);
    }

    @Override
    public List<Notice> listAll() {
        return noticeMapper.listAll();
    }

    @Override
    public void add(Notice notice) {
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    @Override
    public void update(Notice notice) {
        noticeMapper.update(notice);
    }

    @Override
    public void deleteById(int id) {
        noticeMapper.deleteById(id);
    }
}
