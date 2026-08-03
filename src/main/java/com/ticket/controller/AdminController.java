package com.ticket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.result.R;
import com.ticket.entity.AiChatRecord;
import com.ticket.service.AiChatService;
import com.ticket.service.OrderService;
import com.ticket.vo.OrderStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AiChatService aiChatService;

    @GetMapping("/stats")
    public R<OrderStatsVO> getStats() {
        Map<String, Object> stats = orderService.getOrderStats();
        OrderStatsVO vo = new OrderStatsVO();
        vo.setTotalOrders(toLong(stats.get("totalOrders")));
        vo.setTotalAmount(toBigDecimal(stats.get("totalAmount")));
        vo.setCompletedOrders(toLong(stats.get("completedOrders")));
        vo.setRefundOrders(toLong(stats.get("refundOrders")));
        return R.success(vo);
    }

    @GetMapping("/chat/records")
    public R<Page<AiChatRecord>> listChatRecords(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Page<AiChatRecord> records = aiChatService.listAllSessions(page, size);
        return R.success(records);
    }

    @GetMapping("/chat/intents")
    public R<List<Map<String, Object>>> getTopIntents() {
        List<Map<String, Object>> intents = aiChatService.getTopIntents();
        return R.success(intents);
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return Long.parseLong(obj.toString());
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        return new BigDecimal(obj.toString());
    }
}
