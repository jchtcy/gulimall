package com.jch.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jch.gulimall.product.service.CategoryBrandRelationService;
import com.jch.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;
import com.jch.gulimall.product.dao.CategoryDao;
import com.jch.gulimall.product.entity.CategoryEntity;
import com.jch.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查出所有分类以及子分类, 以树形结构组装
     *
     * @return
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        // 1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        // 2、组装成树形结构
        // 找到一级分类
        List<CategoryEntity> level1Menus = entities.stream()
                .filter(categoryEntity -> Long.valueOf("0").equals(categoryEntity.getParentCid()))
                .map(menu -> {
                    menu.setChildrenList(getChildrenList(menu, entities));
                    return menu;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort())
                            - (menu2.getSort() == null ? 0 : menu2.getSort());
                })
                .collect(Collectors.toList());

        return level1Menus;
    }

    /**
     * 递归查找所有菜单的子菜单
     *
     * @param root 当前菜单
     * @param all  所有菜单
     * @return
     */
    private List<CategoryEntity> getChildrenList(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> childrenList = all.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid().equals(root.getCatId()))
                .map(categoryEntity -> {
                    categoryEntity.setChildrenList(getChildrenList(categoryEntity, all));
                    return categoryEntity;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort())
                            - (menu2.getSort() == null ? 0 : menu2.getSort());
                })
                .collect(Collectors.toList());
        return childrenList;
    }

    /**
     * 删除（逻辑删除）
     *
     * @param list
     */
    @Override
    public void removeMenuByIds(List<Long> list) {
        // todo 检查当前删除的菜单, 是否被别的地方引用
        Object catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        // 逻辑删除
        baseMapper.deleteBatchIds(list);
    }

    /**
     * 根据分类id查询完整路径
     *
     * @param catelogId 分类id
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 递归查找父节点
     *
     * @param catelogId
     * @param paths
     * @return
     */
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        paths.add(catelogId);
        return paths;
    }

    /**
     * 修改分类表和品牌分类关联表
     *
     * @param category
     */
    // 失效模式, 同时删除多个缓存
//    @Caching(evict = {
//            @CacheEvict(value = {"category"}, key = "'getLevelOneCateGoryList'"),
//            @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
//    })
    // 删除改分区下的缓存
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateDetail(CategoryEntity category) {
        this.updateById(category);
        if (!StringUtils.isEmpty(category.getName())) {
            categoryBrandRelationService.updateCategory(category);
        }
    }

    /**
     * 查出所有1级分类
     *
     * @return
     */
    // category是缓存分区的名字, #root.methodName(当前方法名)是key
    // 当前方法的结果需要缓存, 如果缓存中有, 方法不用调用。如果没有, 会调用方法, 并将方法的返回值写入缓存
    @Cacheable(value = {"category"}, key = "#root.method.name", sync = true)
    @Override
    public List<CategoryEntity> getLevelOneCateGoryList() {
        return this.baseMapper.selectList(
                new QueryWrapper<CategoryEntity>()
                        .eq("parent_cid", 0)
        );
    }

    /**
     * 三级分类菜单
     *
     * @return
     */
    @Cacheable(value = {"category"}, key = "#root.method.name")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        // 将数据库多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);
        // 查询所有一级分类
        List<CategoryEntity> levelOneList = getParentCid(selectList, 0L);
        // 封装一级分类数据
        Map<String, List<Catelog2Vo>> levelOneMap = levelOneList.stream().collect(Collectors.toMap(
                k -> k.getCatId().toString(),
                v -> {
                    // 查询每一个一级分类的二级分类
                    List<CategoryEntity> levelTwoList = getParentCid(selectList, v.getCatId());
                    // 封装二级分类数据
                    List<Catelog2Vo> levelTwoCateGoryList = null;
                    if (levelTwoList != null) {
                        levelTwoCateGoryList = levelTwoList.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            List<CategoryEntity> levelThreeList = getParentCid(selectList, l2.getCatId());
                            if (levelThreeList != null) {
                                List<Catelog2Vo.Catelog3Vo> levelThreeCateGoryList = levelThreeList.stream().map(levelThree -> {
                                    Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), levelThree.getCatId().toString(), levelThree.getName());
                                    return catelog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(levelThreeCateGoryList);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return levelTwoCateGoryList;
                }
        ));
        return levelOneMap;
    }
//    public Map<String, List<Catelog2Vo>> getCatalogJson() {
//        // 先尝试从缓存中获取
//        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
//        Map<String, List<Catelog2Vo>> catalogJsonForDb = new HashMap<>();
//        if (StringUtils.isEmpty(catalogJSON)) {
//            // 缓存中没有, 查询数据库
//            System.out.println("缓存中没有, 查询数据库");
//            catalogJsonForDb = getCatalogJsonFromDbWithRedissonLock();
//        }
//        System.out.println("缓存中命中");
//        // 转为指定的对象
//        catalogJsonForDb = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
//        });
//
//        return catalogJsonForDb;
//    }

    /**
     * 获取分布式锁
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock(){
        // 获取分布式锁
        // 锁的粒度: 具体缓存某个数据, 11号商品, product-11-lock
        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();
        System.out.println("获取分布式锁");

        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getCatalogJsonForDb();
        } finally {
            lock.unlock();
        }
        return dataFromDb;
    }

    /**
     * 从数据库查询并封装分类数据
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonForDb() {
        // 得到锁后再去缓存中查一次, 此时没有数据再查数据库
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON-lock");
        if (!StringUtils.isEmpty(catalogJSON)) {
            return JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
        }
        System.out.println("查询了数据库");
        // 将数据库多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);
        // 查询所有一级分类
        List<CategoryEntity> levelOneList = getParentCid(selectList, 0L);
        // 封装一级分类数据
        Map<String, List<Catelog2Vo>> levelOneMap = levelOneList.stream().collect(Collectors.toMap(
                k -> k.getCatId().toString(),
                v -> {
                    // 查询每一个一级分类的二级分类
                    List<CategoryEntity> levelTwoList = getParentCid(selectList, v.getCatId());
                    // 封装二级分类数据
                    List<Catelog2Vo> levelTwoCateGoryList = null;
                    if (levelTwoList != null) {
                        levelTwoCateGoryList = levelTwoList.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            List<CategoryEntity> levelThreeList = getParentCid(selectList, l2.getCatId());
                            if (levelThreeList != null) {
                                List<Catelog2Vo.Catelog3Vo> levelThreeCateGoryList = levelThreeList.stream().map(levelThree -> {
                                    Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), levelThree.getCatId().toString(), levelThree.getName());
                                    return catelog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(levelThreeCateGoryList);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return levelTwoCateGoryList;
                }
        ));
        // 查到的数据再放入缓存
        redisTemplate.opsForValue().set("catalogJSON-lock", JSON.toJSONString(levelOneMap), 1, TimeUnit.DAYS);
        return levelOneMap;
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parentCid) {
        List<CategoryEntity> collect = selectList.stream().filter(c -> c.getParentCid().equals(parentCid)).collect(Collectors.toList());
        return collect;
    }
}
