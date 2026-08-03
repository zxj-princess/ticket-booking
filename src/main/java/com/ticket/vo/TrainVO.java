package com.ticket.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrainVO {

    private Long id;

    private String trainNumber;

    /** 前端别名：车次号 */
    private String trainNo;

    private Integer trainType;

    /** 前端别名：车型字符串（G/D/Z/T/K） */
    private String trainTypeStr;

    private Long fromStationId;

    private Long toStationId;

    private String fromStationName;

    private String toStationName;

    /** 前端别名：出发站名 */
    private String fromStation;

    /** 前端别名：到达站名 */
    private String toStation;

    private String departTime;

    private String arriveTime;

    private Integer duration;

    /** 前端别名：历时文案 */
    private String durationText;

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
}
