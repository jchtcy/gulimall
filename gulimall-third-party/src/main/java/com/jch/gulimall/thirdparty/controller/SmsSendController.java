package com.jch.gulimall.thirdparty.controller;

import com.jch.common.utils.R;
import com.jch.gulimall.thirdparty.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @Autowired
    SmsService smsService;

    /**
     * 给其他服务调用使用
     * @param mail
     * @param code
     * @return
     */
    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("mail") String mail, @RequestParam("code") String code) {
        try {
            smsService.sendCode(mail, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok();
    }
}
