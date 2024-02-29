package com.jch.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.member.entity.MemberEntity;

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
}

