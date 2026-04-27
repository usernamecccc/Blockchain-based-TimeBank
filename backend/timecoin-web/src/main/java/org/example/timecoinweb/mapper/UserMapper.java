package org.example.timecoinweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.pojo.Activity;
import org.example.pojo.User;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 根据id删除用户
     * @param ids
     */
    void deleteById(@Param("ids") List<Integer> ids);

    /**
     * 更新用户数据
     * @param user
     */
    void update(User user) throws DuplicateKeyException;

    /**
     *新增用户信息
     * @param user
     */
    @Insert("insert into user(username, password, role, email, age, phone, address, name, create_time, update_time) " +
            "VALUES(#{username},#{password},#{role},#{email},#{age},#{phone},#{address},#{name},#{createTime},#{updateTime}) ")
    void insert(User user);

    /**
     * 管理员查询用户
     * @param role
     * @param address
     * @param username
     * @param minAge
     * @param maxAge
     * @return
     */
    List<User> selectUsers(@Param("role") Short role,@Param("address") String address,@Param("username") String username,@Param("minAge") Short minAge,@Param("maxAge") Short maxAge);

    //更新图片本地地址
    @Update("update user set image=#{local},update_time=now() where id=#{id}")
    void updateImage(@Param("local") String local,@Param("id") Integer id);

    /**
     * 统计各角色人数
     * @return 包含 role 和 count 的 map 列表
     */
    @Select("select role, count(*) as count from user group by role")
    List<java.util.Map<String, Object>> countUsersByRole();
}
