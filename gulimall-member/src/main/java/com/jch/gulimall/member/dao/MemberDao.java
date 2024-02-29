package com.jch.gulimall.member.dao;

import com.jch.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 15:54:55
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
