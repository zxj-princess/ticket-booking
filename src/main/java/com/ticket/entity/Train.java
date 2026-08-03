package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("train")
public class Train {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String trainNumber;

    private Integer trainType;

    private Long fromStationId;

    private Long toStationId;

    private String departTime;

    private String arriveTime;

    private Integer duration;

    private Integer businessSeats;

    private BigDecimal businessPrice;

    private Integer firstSeats;

    private BigDecimal firstPrice;

    private Integer secondSeats;

    private BigDecimal secondPrice;

    private Integer softSleepers;

    private BigDecimal softPrice;

    private Integer hardSleepers;

    private BigDecimal hardPrice;

    private Integer hardSeats;

    private BigDecimal seatPrice;

    private String runningDays;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}