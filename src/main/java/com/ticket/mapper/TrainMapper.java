package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.Train;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainMapper extends BaseMapper<Train> {

    @Select("SELECT * FROM train " +
            "WHERE from_station_id = #{fromStationId} " +
            "AND to_station_id = #{toStationId} " +
            "AND running_days LIKE CONCAT('%', #{dayOfWeek}, '%') " +
            "AND status = 1 " +
            "AND deleted = 0")
    List<Train> selectByStationAndDate(@Param("fromStationId") Long fromStationId,
                                       @Param("toStationId") Long toStationId,
                                       @Param("dayOfWeek") int dayOfWeek);

    /**
     * 跨站点查询：支持城市级查询（多个出发站/到达站）
     */
    @Select("<script>" +
            "SELECT * FROM train " +
            "WHERE from_station_id IN " +
            "<foreach item='id' collection='fromStationIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND to_station_id IN " +
            "<foreach item='id' collection='toStationIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND running_days LIKE CONCAT('%', #{dayOfWeek}, '%') " +
            "AND status = 1 " +
            "AND deleted = 0 " +
            "ORDER BY depart_time ASC" +
            "</script>")
    List<Train> selectByStationIdsAndDate(@Param("fromStationIds") List<Long> fromStationIds,
                                          @Param("toStationIds") List<Long> toStationIds,
                                          @Param("dayOfWeek") int dayOfWeek);
}
