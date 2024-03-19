package com.jch.gulimall.cart.vo;


import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class UserInfoTo {
    private Long userId;
    private String userKey;

    private boolean hasUserKey = false;   // 判断是否有userKey
}
