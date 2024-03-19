package com.jch.gulimall.cart.interceptor;

import com.jch.common.constant.auth.AuthConstant;
import com.jch.common.constant.cart.CartConstant;
import com.jch.common.vo.auth.MemberResponseVO;
import com.jch.gulimall.cart.controller.CartController;
import com.jch.gulimall.cart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 1、在执行目标方法之前，检测cookie里的userKey，如果没有则新建用户传输对象，userKey设为随机uuid
 * 2、将用户传输对象封装进ThreadLocal。
 * 3、在执行目标方法之后，创建cookie并，设置作用域和过期时间，让浏览器保存
 */
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 目标方法执行之前执行
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();

        HttpSession session = request.getSession();
        MemberResponseVO member = (MemberResponseVO) session.getAttribute(AuthConstant.LOGIN_USER);
        if (member != null) {
            // 用户登录
            userInfoTo.setUserId(member.getId());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                // user-key
                String name = cookie.getName();
                if (CartConstant.TEMP_USER_COOKIE_NAME.equals(name)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setHasUserKey(true);
                }
            }
        }

        // 如果没有登录, 并且没有临时用户
        if (member == null && StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }

        // 目标方法执行之前
        threadLocal.set(userInfoTo);
        return true;
    }

    /**
     * 业务执行之后
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();

        if (!userInfoTo.isHasUserKey()) {
            // 持续延长临死用户过期时间
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
