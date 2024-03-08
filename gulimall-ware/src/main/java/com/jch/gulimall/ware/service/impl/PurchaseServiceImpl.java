package com.jch.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jch.common.constant.ware.PurchaseConstant;
import com.jch.common.exception.RRException;
import com.jch.gulimall.ware.dao.PurchaseDetailDao;
import com.jch.gulimall.ware.entity.PurchaseDetailEntity;
import com.jch.gulimall.ware.service.PurchaseDetailService;
import com.jch.gulimall.ware.service.WareSkuService;
import com.jch.gulimall.ware.vo.MergeVo;
import com.jch.gulimall.ware.vo.PurchaseDoneVo;
import com.jch.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;

import com.jch.gulimall.ware.dao.PurchaseDao;
import com.jch.gulimall.ware.entity.PurchaseEntity;
import com.jch.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private PurchaseDetailDao purchaseDetailDao;

    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查询未领取的采购单
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", PurchaseConstant.PurchaseStatusEnum.CREATED.getCode())
                        .or().eq("status", PurchaseConstant.PurchaseStatusEnum.ASSIGNED.getCode())
        );

        return new PageUtils(page);
    }

    /**
     * 合并采购需求
     *
     * @param mergeVo
     */
    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        // 采购需求id
        List<Long> items = mergeVo.getItems();
        // 检验采购需求状态
        List<PurchaseDetailEntity> verification = purchaseDetailDao.selectBatchIds(items);
        for (PurchaseDetailEntity v : verification) {
            // 采购需求状态必须为新建或已分配
            if (!v.getStatus().equals(PurchaseConstant.PurchaseDetailStatusEnum.CREATED.getCode())
                    && !v.getStatus().equals(PurchaseConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode())) {
                throw new RRException("采购需求状态必须为新建或已分配");
            }
        }

        Long purchaseId = mergeVo.getPurchaseId();
        // 新增采购单
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(PurchaseConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(purchaseEntity);

            purchaseId = purchaseEntity.getId();
        }
        // 整合采购需求
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> purchaseDetailEntityList = items.stream().map(item -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(item);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(PurchaseConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(purchaseDetailEntityList);

        // 修改更新时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    /**
     * 领取采购单
     *
     * @param ids 采购单id
     */
    @Transactional
    @Override
    public void receivedPurchase(List<Long> ids) {
        // 确认当前采购单是新建或已分配状态
        List<PurchaseEntity> purchaseEntityList = this.getBaseMapper().selectBatchIds(ids).stream()
                .filter(purchaseEntity -> {
                    // 确保采购单是新建或已分配状态
                    return purchaseEntity.getStatus().equals(PurchaseConstant.PurchaseStatusEnum.CREATED.getCode()) ||
                            purchaseEntity.getStatus().equals(PurchaseConstant.PurchaseStatusEnum.ASSIGNED.getCode());
                })
                //修改采购单的状态为已领取
                .map(purchaseEntity -> {
                    purchaseEntity.setStatus(PurchaseConstant.PurchaseStatusEnum.RECEIVE.getCode());
                    purchaseEntity.setUpdateTime(new Date());
                    return purchaseEntity;
                }).collect(Collectors.toList());
        // 改变采购单状态
        this.updateBatchById(purchaseEntityList);
        // 改变采购项状态
        PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
        purchaseDetailEntity.setStatus(PurchaseConstant.PurchaseDetailStatusEnum.BUYING.getCode());
        purchaseDetailService.update(
                purchaseDetailEntity,
                new UpdateWrapper<PurchaseDetailEntity>().in("purchase_id", ids)
        );
    }

    /**
     * 完成采购
     * @param purchaseDoneVo
     */
    @Transactional
    @Override
    public void donePurchase(PurchaseDoneVo purchaseDoneVo) {
        // 改变采购项状态
        Boolean flag = true;
        List<PurchaseItemDoneVo> items = purchaseDoneVo.getItems();
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            if (item.getStatus().equals(PurchaseConstant.PurchaseDetailStatusEnum.HASERROR.getCode())) {
                flag = false;
                purchaseDetailEntity.setStatus(PurchaseConstant.PurchaseDetailStatusEnum.HASERROR.getCode());
            } else {
                purchaseDetailEntity.setStatus(PurchaseConstant.PurchaseDetailStatusEnum.FINISH.getCode());

                // 将成功采购入库
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum());
            }
            purchaseDetailEntity.setId(item.getItemId());
            updates.add(purchaseDetailEntity);
        }
        // 批量更新采购项状态
        purchaseDetailService.updateBatchById(updates);

        // 改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseDoneVo.getId());
        purchaseEntity.setStatus(flag ? PurchaseConstant.PurchaseDetailStatusEnum.FINISH.getCode():
                PurchaseConstant.PurchaseDetailStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }
}
