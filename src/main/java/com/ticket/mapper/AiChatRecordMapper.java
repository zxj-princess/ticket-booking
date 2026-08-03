package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.AiChatRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiChatRecordMapper extends BaseMapper<AiChatRecord> {

    @Select("SELECT * FROM ai_chat_record WHERE session_id = #{sessionId} AND deleted = 0 ORDER BY create_time ASC")
    List<AiChatRecord> selectBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT intent, COUNT(*) AS count " +
            "FROM ai_chat_record " +
            "WHERE intent IS NOT NULL AND deleted = 0 " +
            "GROUP BY intent " +
            "ORDER BY count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTopIntents(@Param("limit") int limit);
}