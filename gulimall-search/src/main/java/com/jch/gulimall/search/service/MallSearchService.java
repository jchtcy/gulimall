package com.jch.gulimall.search.service;

import com.jch.gulimall.search.vo.SearchParam;
import com.jch.gulimall.search.vo.SearchResult;

import java.io.IOException;

public interface MallSearchService {

    /**
     * 实现搜索功能
     *
     * @param searchParam 检索的所有参数
     * @return 返回检索的结果
     */
    SearchResult search(SearchParam searchParam);
}
