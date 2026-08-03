package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNumber;

    private Long userId;

    private Long trainId;

    private String trainNumber;

    private String fromStation;

    private String toStation;

    private LocalDateTime departTime;

    private LocalDateTime arriveTime;

    private Integer seatType;

    private String seatNumber;

    private Long passengerId;

    private String passengerName;

    private String passengerIdCard;

    private BigDecimal totalAmount;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime refundTime;

    private BigDecimal refundAmount;

    private BigDecimal refundFee;

    private LocalDateTime cancelTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}