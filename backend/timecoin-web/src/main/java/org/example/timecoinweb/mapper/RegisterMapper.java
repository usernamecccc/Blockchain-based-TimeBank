package org.example.timecoinweb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.pojo.User;
import org.springframework.dao.DuplicateKeyException;

@Mapper
public interface RegisterMapper {

    /**
     * 新增加用户，注册
     */
    @Insert("insert into user(username,name,password,role,email,age,phone,address,create_time,update_time)" +
            "values(#{username},#{name},#{password},#{role},#{email},#{age},#{phone},#{address},#{createTime},#{updateTime})")
    void register(User user) throws DuplicateKeyException;

    @Select("select * from user where username=#{username}")
    User getByUsername(@Param("username") String username);

    @Update("update user set password=#{password}, update_time=now() where id=#{id}")
    void updatePasswordById(@Param("id") Integer id, @Param("password") String password);

    /**
     * 根据id查询用户自身
     * @param id
     * @return
     */
    @Select("select * from user where id=#{id}")
    User getById(Integer id);
}
