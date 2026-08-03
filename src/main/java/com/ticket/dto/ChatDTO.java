package com.ticket.dto;

import lombok.Data;

@Data
public class ChatDTO {

    private Long userId;

    private String sessionId;

    private String message;
}
