package com.jch.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.jch.common.utils.R;
import com.jch.gulimall.ware.feign.MemberFeignService;
import com.jch.gulimall.ware.vo.FareVO;
import com.jch.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;

import com.jch.gulimall.ware.dao.WareInfoDao;
import com.jch.gulimall.ware.entity.WareInfoEntity;
import com.jch.gulimall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                new QueryWrapper<WareInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 实现仓库模糊查询功能
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.eq("id", key).or().like("name", key).or().like("areacode", key);
        }
        IPage page = this.page(new Query<WareInfoEntity>().getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    /**
     * 根据用户的收获地址计算运费
     * @param addrId
     * @return
     */
    @Override
    public FareVO getFare(Long addrId) {
        FareVO fareVO = new FareVO();
        // 收获地址详细信息
        R r = memberFeignService.info(addrId);
        MemberAddressVo address = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {});

        if (address != null) {
            // 简单处理：截取手机号最后一位作为邮费
            String phone = address.getPhone();
            String fare = phone.substring(phone.length() - 1);
            BigDecimal bigDecimal = new BigDecimal(fare);
            fareVO.setFare(bigDecimal);
            fareVO.setAddress(address);
            return fareVO;
        }
        return null;
    }
}
