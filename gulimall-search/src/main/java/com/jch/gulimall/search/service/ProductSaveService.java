package com.jch.gulimall.search.service;

import com.jch.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {

    /**
     * 上架商品
     * @param skuEsModelList
     */
    boolean productStatusUp(List<SkuEsModel> skuEsModelList) throws IOException;
}
