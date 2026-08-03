package com.ticket.dto;

import lombok.Data;

@Data
public class TrainQueryDTO {

    private Long fromStationId;

    private Long toStationId;

    private String date;
}
