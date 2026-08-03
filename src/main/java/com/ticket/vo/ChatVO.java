package com.ticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatVO {

    private String role;

    private String content;

    private LocalDateTime createTime;
}
