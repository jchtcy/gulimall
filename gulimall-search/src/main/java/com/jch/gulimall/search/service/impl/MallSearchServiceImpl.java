package com.jch.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.jch.common.to.es.SkuEsModel;
import com.jch.gulimall.search.config.GulimallElasticSearchConfig;
import com.jch.gulimall.search.constant.EsConstant;
import com.jch.gulimall.search.service.MallSearchService;
import com.jch.gulimall.search.vo.SearchParam;
import com.jch.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 实现搜索功能(ES中)
     *
     * @param searchParam 检索的所有参数
     * @return 返回检索的结果
     */
    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult result = null;

        // 动态构建出查询需要的DSL语句
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        try {
            // 执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            // 分析响应格式封装成需要的格式
            result = buildSearchResult(response, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 动态构建出查询需要的DSL语句
     *
     * @param searchParam
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        // 构建DSL语句
        SearchSourceBuilder builder = new SearchSourceBuilder();


        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 1、查询
        // 模糊匹配
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        }

        // 2、过滤
        // 三级分类id查询
        if (searchParam.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        }
        // 品牌id查询
        if (searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", searchParam.getBrandId()));
        }
        // 按照所有指定的属性查询
        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {

            for (String attrStr : searchParam.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        // 按照是否有库存查询
        boolQuery.filter(QueryBuilders.termQuery("hasStock", searchParam.getHasStock() == 1));
        // 按照价格区间查询
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = searchParam.getSkuPrice().split("_");
            if (s.length == 2) {
                rangeQuery.gte(s[0]).lte(s[1]);
            } else {
                if (searchParam.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(s[0]);
                }
                if (searchParam.getSkuPrice().endsWith("_")) {
                    rangeQuery.gte(s[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }
        // 查询和过滤结束
        builder.query(boolQuery);

        // 3、排序
        if (!StringUtils.isEmpty(searchParam.getSort())) {
            String sort = searchParam.getSort();
            String[] s = sort.split("_");
            SortOrder sortOrder = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            builder.sort(s[0], sortOrder);
        }

        // 4、分页
        builder.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        builder.size(EsConstant.PRODUCT_PAGESIZE);

        // 5、高亮
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            builder.highlighter(highlightBuilder);
        }

        // 6、聚合分析
        // 品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        // 品牌聚合的子聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        builder.aggregation(brandAgg);
        // 分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        builder.aggregation(catalogAgg);
        // 属性聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
        // 聚合出当前所有属性id
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // 聚合出属性对应的名字
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 聚合出属性对应的所有属性值
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attrAgg.subAggregation(attrIdAgg);
        builder.aggregation(attrAgg);

        //System.out.println(builder.toString());

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, builder);
        return searchRequest;
    }

    /**
     * 分析响应格式封装成需要的格式
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam searchParam) {
        SearchResult result = new SearchResult();
        // 命中的记录
        SearchHits hits = response.getHits();
        // 1、返回的所有查询到的商品
        SearchHit[] data = hits.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (data != null && data.length > 0) {
            for (SearchHit hit : data) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                esModels.add(skuEsModel);
            }
        }
        result.setProducts(esModels);
        // 获得聚合信息
        Aggregations aggregations = response.getAggregations();
        // 2、当前商品涉及到所有属性信息
        List<SearchResult.AttrVo> attrVoList = new ArrayList<>();
        ParsedNested attrAgg = aggregations.get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
        // 面包屑map数据源
        Map<Long, String> attrMap = new HashMap<>();
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();

            attrVo.setAttrId(Long.parseLong(bucket.getKeyAsString()));

            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            attrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());

            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            attrVo.setAttrValue(attrValueAgg.getBuckets().stream().map(b ->  b.getKeyAsString()).collect(Collectors.toList()));
            attrVoList.add(attrVo);

            // 构建面包屑数据源
            if (!CollectionUtils.isEmpty(searchParam.getAttrs()) && !attrMap.containsKey(attrVo.getAttrId())) {
                attrMap.put(attrVo.getAttrId(), attrVo.getAttrName());
            }
        }
        result.setAttrs(attrVoList);
        // 3、当前商品涉及到所有品牌信息
        List<SearchResult.BrandVo> brandVoList = new ArrayList<>();
        ParsedLongTerms brandAgg = aggregations.get("brand_agg");
        Map<Long, String> brandMap = new HashMap<>();// 面包屑map数据源【品牌】
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();

            brandVo.setBrandId(Long.parseLong(bucket.getKeyAsString()));

            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
            brandVo.setBrandName(brandNameAgg.getBuckets().get(0).getKeyAsString());

            ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
            brandVo.setBrandImg(brandImgAgg.getBuckets().get(0).getKeyAsString());

            brandVoList.add(brandVo);

            // 构建面包屑数据源
            if (!CollectionUtils.isEmpty(searchParam.getBrandId()) ) {
                brandMap.put(brandVo.getBrandId(), brandVo.getBrandName());
            }
        }
        result.setBrands(brandVoList);
        // 4、当前商品涉及到所有分类
        ParsedLongTerms catalogAgg = aggregations.get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVoList = new ArrayList<>();
        String catalogName = null;// 面包屑map数据源【分类】
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();

            catalogVo.setCatalogId(Long.parseLong(bucket.getKeyAsString()));

            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            catalogVo.setCatalogName(catalogNameAgg.getBuckets().get(0).getKeyAsString());

            catalogVoList.add(catalogVo);

            // 构建面包屑数据源
            if (catalogVo.getCatalogId().equals(searchParam.getCatalog3Id())) {
                catalogName = catalogVo.getCatalogName();
            }
        }
        result.setCatalogs(catalogVoList);
        // 5、分页信息
        result.setPageNum(searchParam.getPageNum());// 页码
        long total = hits.getTotalHits().value;
        result.setTotal(total);// 总记录数
        result.setTotalPages((int) Math.ceil((double)total / EsConstant.PRODUCT_PAGESIZE));// 总页码
        // 导航页码
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= result.getTotalPages(); i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        // 6、构建面包屑导航
        if (!CollectionUtils.isEmpty(searchParam.getAttrs())) {
            List<SearchResult.NavVo> navVoList = searchParam.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                // 分析attrs传递过来的查询参数值
                String[] s = attr.split("_");
                // 封装筛选属性ID集合【给前端判断哪些属性是筛选条件，从而隐藏显示属性栏，显示在面包屑中】
                result.getAttrIds().add(Long.parseLong(s[0]));
                // 面包屑名字：属性名
                navVo.setNavValue(s[1]);
                // 面包屑值：属性值
                navVo.setNavName(attrMap.get(Long.parseLong(s[0])));
                // 设置跳转地址（将属性条件置空）【当取消面包屑上的条件时，跳转地址】
                String replace = replaceQueryString(searchParam, "attrs", attr);
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);// 每一个属性都有自己对应的回退地址
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVoList);
        }
        // 7.构建面包屑导航数据_品牌
        if (!CollectionUtils.isEmpty(searchParam.getBrandId())) {
            List<SearchResult.NavVo> navs = result.getNavs();
            // 多个品牌ID封装成一级面包屑，所以这里只需要一个NavVo
            SearchResult.NavVo nav = new SearchResult.NavVo();
            // 面包屑名称直接使用品牌
            nav.setNavName("品牌");
            StringBuffer buffer = new StringBuffer();
            String replace = "";
            for (Long brandId : searchParam.getBrandId()) {
                // 多个brandId筛选条件汇总为一级面包屑，所以navValue拼接所有品牌名
                buffer.append(brandMap.get(brandId)).append(";");
                // 因为多个brandId汇总为一级面包屑，所以每一个brandId筛选条件都要删除
                replace = replaceQueryString(searchParam, "brandId", brandId.toString());
            }
            nav.setNavValue(buffer.toString());// 品牌拼接值
            nav.setLink("http://search.gulimall.com/list.html?" + replace);// 回退品牌面包屑等于删除所有品牌条件
            navs.add(nav);
        }
        // 8、构建面包屑导航数据_分类
        if (searchParam.getCatalog3Id() != null) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo nav = new SearchResult.NavVo();
            nav.setNavName("分类");
            nav.setNavValue(catalogName);// 分类名
//            String replace = replaceQueryString(param, "catalog3Id", param.getCatalog3Id().toString());
//            nav.setLink("http://search.gulimall.com/list.html?" + replace);
            navs.add(nav);
        }
        return result;
    }

    private String replaceQueryString(SearchParam param, String key, String value) {
        // 解决编码问题，前端参数使用UTF-8编码了
        String encode = null;
        encode = UriEncoder.encode(value);
//                try {
//                    encode = URLEncoder.encode(attr, "UTF-8");// java将空格转义成了+号
//                    encode = encode.replace("+", "%20");// 浏览器将空格转义成了%20，差异化处理，否则_queryString与encode匹配失败
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
        // 替换掉当前查询条件，剩下的查询条件即是回退地址
        String replace = "";
        if (!StringUtils.isEmpty(param.get_queryString())) {
            replace = param.get_queryString().replace("&" + key + "=" + encode, "");
        }
        return replace;
    }
}
