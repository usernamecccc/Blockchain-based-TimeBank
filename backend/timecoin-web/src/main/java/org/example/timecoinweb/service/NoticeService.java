package org.example.timecoinweb.service;

import org.example.pojo.Notice;

import java.util.List;

public interface NoticeService {

    List<Notice> listRecent(int limit);

    List<Notice> listAll();

    void add(Notice notice);

    void update(Notice notice);

    void deleteById(int id);
}
