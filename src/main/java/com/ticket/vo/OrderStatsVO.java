package com.ticket.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatsVO {

    private Long totalOrders;

    private BigDecimal totalAmount;

    private Long completedOrders;

    private Long refundOrders;
}
