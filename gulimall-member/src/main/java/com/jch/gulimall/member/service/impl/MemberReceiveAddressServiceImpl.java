package com.jch.gulimall.member.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;

import com.jch.gulimall.member.dao.MemberReceiveAddressDao;
import com.jch.gulimall.member.entity.MemberReceiveAddressEntity;
import com.jch.gulimall.member.service.MemberReceiveAddressService;


@Service("memberReceiveAddressService")
public class MemberReceiveAddressServiceImpl extends ServiceImpl<MemberReceiveAddressDao, MemberReceiveAddressEntity> implements MemberReceiveAddressService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberReceiveAddressEntity> page = this.page(
                new Query<MemberReceiveAddressEntity>().getPage(params),
                new QueryWrapper<MemberReceiveAddressEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取会员收获地址列表
     * @param memberId
     * @return
     */
    @Override
    public List<MemberReceiveAddressEntity> getAddress(Long memberId) {
        List<MemberReceiveAddressEntity> address = this.list(
                new QueryWrapper<MemberReceiveAddressEntity>().eq("member_id", memberId)
        );
        return address;
    }
}
