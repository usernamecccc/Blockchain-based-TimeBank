package org.example.timecoinweb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.pojo.ChainReconcileLog;

import java.util.List;

@Mapper
public interface ChainReconcileLogMapper {

    @Insert("INSERT INTO chain_reconcile_log(run_time, total_checked, mismatch_count, fixed_count, chain_ready, detail_json, create_time) " +
            "VALUES(#{runTime}, #{totalChecked}, #{mismatchCount}, #{fixedCount}, #{chainReady}, #{detailJson}, NOW())")
    void insert(ChainReconcileLog log);

    @Select("SELECT id, run_time AS runTime, total_checked AS totalChecked, mismatch_count AS mismatchCount, " +
            "fixed_count AS fixedCount, chain_ready AS chainReady, detail_json AS detailJson, create_time AS createTime " +
            "FROM chain_reconcile_log ORDER BY id DESC LIMIT #{limit}")
    List<ChainReconcileLog> listRecent(@Param("limit") int limit);
}
