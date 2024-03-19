package com.jch.authserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.jch.authserver.feign.MemberFeignService;
import com.jch.authserver.feign.ThirdPartFeignService;
import com.jch.authserver.vo.UserLoginVo;
import com.jch.authserver.vo.UserRegistVo;
import com.jch.common.constant.auth.AuthConstant;
import com.jch.common.exception.BizCodeEnum;
import com.jch.common.utils.R;
import com.jch.common.vo.auth.MemberResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.util.StringUtils;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 登录
 *
 * @Author: wanzenghui
 * @Date: 2021/11/26 22:26
 */
@Controller
public class LoginController {

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/sms/sendCode")
    @ResponseBody
    public R sendCode(@RequestParam("mail") String mail) {
        // 1、接口防刷
        String redisCode = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + mail);
        if (!StringUtils.isEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60 * 1000) {
                // 60秒内不能再发
                return R.error(BizCodeEnum.SMS_DOCE_EXCEPTION.getCode(), BizCodeEnum.SMS_DOCE_EXCEPTION.getMsg());
            }
        }
        // 2、验证码存入缓存
        String code = UUID.randomUUID().toString().substring(0, 6);
        String codeTime = code + "_" + System.currentTimeMillis();
        // redis缓存验证码 key-mail value-code  sms:code:test@qq.com 123456_time
        redisTemplate.opsForValue().set(AuthConstant.SMS_CODE_CACHE_PREFIX + mail, codeTime,
                10, TimeUnit.MINUTES);
        // 3、发送验证码
        thirdPartFeignService.sendCode(mail, code);
        return R.ok();
    }

    /**
     * 注册
     * @param vo
     * @param result
     * @param redirectAttributes 重定向携带的数据
     * @return
     */
    @PostMapping("/register")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes) {
        // 参数校验
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            redirectAttributes.addFlashAttribute("errors", errors);
            // 校验失败, 转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        // 校验验证码
        String code = vo.getCode();
        String mail = vo.getMail();
        String s = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + mail);
        if (!StringUtils.isEmpty(s)) {
            if (code.equals(s.split("_")[0])) {
                // 删除验证码, 令牌机制
                redisTemplate.delete(AuthConstant.SMS_CODE_CACHE_PREFIX + mail);
                // 验证码通过。 真正注册, 调用远程服务进行注册
                R r = memberFeignService.regist(vo);
                if (r.getCode() == 0) {
                    // 成功
                    return "redirect:/reg.html";
                } else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    // 注册成功回到登录页
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                // 校验失败, 转发到注册页
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            // 校验失败, 转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @GetMapping({"/login.html", "/", "/index", "index.html"}) // auth
    public String loginPage(HttpSession session) {
        // 从会话获取loginUser
        Object attribute = session.getAttribute(AuthConstant.LOGIN_USER);

        if (attribute == null) {
            return "login";
        }

        // 已登陆过，重定向到首页
        return "redirect:http://gulimall.com";
    }

    /**
     * 登录
     * @param vo
     * @param redirectAttributes
     * @param session
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session) {
        // 远程登录
        R login = memberFeignService.login(vo);
        if (login.getCode() == 0) {
            // 2.登录成功，设置session值
            MemberResponseVO data = login.getData("data", new TypeReference<MemberResponseVO>() {
            });
            session.setAttribute(AuthConstant.LOGIN_USER, data);
            // 登录成功返回商城首页
            return "redirect:http://gulimall.com";
        } else {
            // 登录失败, 返回登录页
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
