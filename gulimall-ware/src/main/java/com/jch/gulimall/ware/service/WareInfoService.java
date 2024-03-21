package com.jch.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.ware.entity.WareInfoEntity;
import com.jch.gulimall.ware.vo.FareVO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 16:22:11
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 实现仓库模糊查询功能
     * @param params
     * @return
     */
    PageUtils queryPageByCondition(Map<String, Object> params);

    /**
     * 根据用户的收获地址计算运费
     * @param addrId
     * @return
     */
    FareVO getFare(Long addrId);
}

