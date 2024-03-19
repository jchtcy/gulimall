package com.jch.gulimall.thirdparty.service;

import java.security.GeneralSecurityException;

/**
 * 短信服务
 * @Author: wanzenghui
 * @Date: 2021/11/27 22:58
 */
public interface SmsService {

    /**
     * 发送短信验证码
     * @param to 邮箱
     * @param vcode  验证码
     */
    public Boolean sendCode(String to, String vcode) throws GeneralSecurityException;

}
