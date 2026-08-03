package com.ticket.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    private String avatar;

    private Integer role;

    private BigDecimal balance;
}
