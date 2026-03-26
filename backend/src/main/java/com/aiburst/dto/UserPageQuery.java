package com.aiburst.dto;


import lombok.Data;

@Data
public class UserPageQuery {
    private String username;
    private Integer status;
    private int pageNum = 1;
    private int pageSize = 10;
}
