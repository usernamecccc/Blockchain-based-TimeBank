package org.example.timecoinweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.pojo.Notice;

import java.util.List;

@Mapper
public interface NoticeMapper {

    @Insert("INSERT INTO notice(title, content, create_time) VALUES (#{title}, #{content}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Notice notice);

    @Update("UPDATE notice SET title = #{title}, content = #{content} WHERE id = #{id}")
    void update(Notice notice);

    @Delete("DELETE FROM notice WHERE id = #{id}")
    void deleteById(@Param("id") int id);

    @Select("SELECT id, title, content, create_time FROM notice ORDER BY create_time DESC LIMIT #{limit}")
    List<Notice> listRecent(@Param("limit") int limit);

    @Select("SELECT id, title, content, create_time FROM notice ORDER BY create_time DESC")
    List<Notice> listAll();
}
