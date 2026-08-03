package com.ticket.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.entity.AiChatRecord;

import java.util.List;
import java.util.Map;

public interface AiChatService {

    AiChatRecord chat(Long userId, String sessionId, String message);

    List<AiChatRecord> listBySessionId(String sessionId);

    List<AiChatRecord> listByUserId(Long userId);

    void deleteRecord(Long id);

    List<Map<String, Object>> getTopIntents();

    Page<AiChatRecord> listAllSessions(int page, int size);
}
