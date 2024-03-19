package com.jch.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.member.entity.MemberEntity;
import com.jch.gulimall.member.exception.EmailExistException;
import com.jch.gulimall.member.exception.UsernameExistException;
import com.jch.gulimall.member.vo.MemberLoginVo;
import com.jch.gulimall.member.vo.MemberRegistVo;

import java.util.Map;

/**
 * 会员
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 15:54:55
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 注册进会员
     * @param vo
     */
    void regist(MemberRegistVo vo);

    /**
     * 检查邮箱是否唯一
     * @param mail
     * @return
     */
    void checkUniqueEmail(String mail) throws EmailExistException;

    /**
     * 检查用户名是否唯一
     * @param userName
     * @return
     */
    void checkUniqueUserName(String userName) throws UsernameExistException;

    /**
     * 会员登录
     * @param vo
     * @return
     */
    MemberEntity login(MemberLoginVo vo);
}

