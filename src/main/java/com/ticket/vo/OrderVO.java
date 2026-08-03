package com.ticket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;

    private String orderNumber;

    /** 前端别名：订单号 */
    private String orderNo;

    private Long userId;

    private Long trainId;

    private String trainNumber;

    /** 前端别名：车次号 */
    private String trainNo;

    private String fromStation;

    private String toStation;

    private LocalDateTime departTime;

    private LocalDateTime arriveTime;

    /** 前端别名：发车时间(HH:mm) */
    private String departTimeStr;

    /** 前端别名：到达时间(HH:mm) */
    private String arriveTimeStr;

    private Integer seatType;

    /** 前端别名：座位文案 */
    private String seatLabel;

    private String seatNumber;

    private Long passengerId;

    private String passengerName;

    private String passengerIdCard;

    private String passengerPhone;

    private BigDecimal totalAmount;

    /** 前端别名：订单金额 */
    private BigDecimal totalPrice;

    /** 前端别名：价格 */
    private BigDecimal price;

    private Integer status;

    /** 前端别名：状态字符串 */
    private String statusStr;

    /** 前端别名：出发日期 */
    private String date;

    /** 前端别名：历时文案 */
    private String durationText;

    private LocalDateTime payTime;

    private LocalDateTime createTime;
}
