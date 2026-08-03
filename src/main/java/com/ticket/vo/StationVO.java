package com.ticket.vo;

import lombok.Data;

@Data
public class StationVO {

    private Long id;

    private String name;

    private String code;

    private String pinyin;

    private String city;

    private String province;
}
