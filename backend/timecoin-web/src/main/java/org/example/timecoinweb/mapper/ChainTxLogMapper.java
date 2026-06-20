package org.example.timecoinweb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.pojo.ChainTxLog;

import java.util.List;

@Mapper
public interface ChainTxLogMapper {

    @Insert("INSERT INTO chain_tx_log(tx_type, from_account, to_account, amount, tx_hash, biz_type, biz_ref, create_time) " +
            "VALUES(#{txType}, #{fromAccount}, #{toAccount}, #{amount}, #{txHash}, #{bizType}, #{bizRef}, NOW())")
    void insert(ChainTxLog log);

    @Select("SELECT id, tx_type AS txType, from_account AS fromAccount, to_account AS toAccount, amount, tx_hash AS txHash, " +
            "biz_type AS bizType, biz_ref AS bizRef, create_time AS createTime " +
            "FROM chain_tx_log ORDER BY id DESC LIMIT #{limit}")
    List<ChainTxLog> listRecent(@Param("limit") int limit);
}
