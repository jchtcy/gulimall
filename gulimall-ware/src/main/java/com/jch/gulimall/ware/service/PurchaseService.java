package com.jch.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.ware.entity.PurchaseEntity;

import java.util.Map;

/**
 * 采购信息
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 16:22:11
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

