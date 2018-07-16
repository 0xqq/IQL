package cn.mc.report.iql.service;

import cn.mc.report.bean.IqlExcution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IqlExcutionRepository  extends JpaRepository<IqlExcution, Long> {

    @Query(value="select * from iql_excution a where a.iql like CONCAT('%',:search,'%')",nativeQuery=true)
    List<IqlExcution> findByIqlLike(@Param("search") String search);

}