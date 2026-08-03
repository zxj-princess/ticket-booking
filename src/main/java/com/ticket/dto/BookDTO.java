package com.ticket.dto;

import lombok.Data;

import java.util.List;

/**
 * 订票请求DTO
 */
@Data
public class BookDTO {

    private Long trainId;

    private Integer seatType;

    private List<Long> passengerIds;

    private Integer seatCount;

    private String date;
}
