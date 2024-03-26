package com.jch.gulimall.coupon.service.impl;

import com.jch.common.utils.DateUtils;
import com.jch.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.jch.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;
import com.jch.gulimall.coupon.dao.SeckillSessionDao;
import com.jch.gulimall.coupon.entity.SeckillSessionEntity;
import com.jch.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取最近三天的活动
     * @return
     */
    @Override
    public List<SeckillSessionEntity> getSeckillSkuLatest3Days() {
        //查出这三天参与秒杀活动的商品
        List<SeckillSessionEntity> list = this.list(new QueryWrapper<SeckillSessionEntity>()
                .between("start_time", DateUtils.startTime(), DateUtils.endTime())
        );

        if (list != null && list.size() > 0) {
            list = list.stream().map(session -> {
                Long id = session.getId();
                List<SeckillSkuRelationEntity> skus = seckillSkuRelationService.list(new QueryWrapper<SeckillSkuRelationEntity>()
                        .eq("promotion_session_id", id)
                );
                session.setRelationSkus(skus);
                return session;
            }).collect(Collectors.toList());
        }

        return list;
    }
}
