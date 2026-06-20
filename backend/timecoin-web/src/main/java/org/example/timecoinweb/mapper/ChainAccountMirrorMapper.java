package org.example.timecoinweb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.pojo.ChainAccountMirror;

@Mapper
public interface ChainAccountMirrorMapper {

    @Select("SELECT account_id AS accountId, coin_balance AS coinBalance, balance_sync_time AS balanceSyncTime, update_time AS updateTime " +
            "FROM chain_account_mirror WHERE account_id = #{accountId}")
    ChainAccountMirror selectById(@Param("accountId") String accountId);

    @Insert("INSERT INTO chain_account_mirror(account_id, coin_balance, balance_sync_time, update_time) " +
            "VALUES(#{accountId}, #{coinBalance}, NOW(), NOW())")
    void insert(ChainAccountMirror mirror);

    @Update("UPDATE chain_account_mirror SET coin_balance = coin_balance + #{delta}, update_time = NOW() WHERE account_id = #{accountId}")
    int addBalance(@Param("accountId") String accountId, @Param("delta") long delta);

    @Update("UPDATE chain_account_mirror SET coin_balance = coin_balance - #{amount}, update_time = NOW() " +
            "WHERE account_id = #{accountId} AND coin_balance >= #{amount}")
    int deductBalance(@Param("accountId") String accountId, @Param("amount") long amount);

    @Update("UPDATE chain_account_mirror SET coin_balance = #{balance}, balance_sync_time = NOW(), update_time = NOW() " +
            "WHERE account_id = #{accountId}")
    void setBalance(@Param("accountId") String accountId, @Param("balance") long balance);
}
