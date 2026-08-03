package com.ticket.dto;

import lombok.Data;

@Data
public class CreateOrderDTO {

    private Long trainId;

    private Integer seatType;

    private Long passengerId;
}
