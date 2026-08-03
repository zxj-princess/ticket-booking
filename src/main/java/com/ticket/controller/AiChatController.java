package com.ticket.controller;

import com.ticket.common.result.R;
import com.ticket.dto.ChatDTO;
import com.ticket.entity.AiChatRecord;
import com.ticket.service.AiChatService;
import com.ticket.vo.ChatVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai/chat")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/send")
    public R<ChatVO> sendMessage(@RequestBody ChatDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AiChatRecord record = aiChatService.chat(userId, dto.getSessionId(), dto.getMessage());
        return R.success(convertToVO(record));
    }

    @GetMapping("/session/{sessionId}")
    public R<List<ChatVO>> getSessionRecords(@PathVariable String sessionId) {
        List<AiChatRecord> records = aiChatService.listBySessionId(sessionId);
        List<ChatVO> voList = records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return R.success(voList);
    }

    @GetMapping("/my")
    public R<List<ChatVO>> getMySessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<AiChatRecord> records = aiChatService.listByUserId(userId);
        List<ChatVO> voList = records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return R.success(voList);
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteRecord(@PathVariable Long id) {
        aiChatService.deleteRecord(id);
        return R.success();
    }

    private ChatVO convertToVO(AiChatRecord record) {
        ChatVO vo = new ChatVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
