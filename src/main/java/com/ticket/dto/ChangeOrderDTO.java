package com.ticket.dto;

import lombok.Data;

@Data
public class ChangeOrderDTO {

    private String orderNumber;

    private Long newTrainId;

    private Integer newSeatType;
}
