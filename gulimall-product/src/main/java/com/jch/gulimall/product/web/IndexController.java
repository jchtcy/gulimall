package com.jch.gulimall.product.web;

import com.jch.gulimall.product.entity.CategoryEntity;
import com.jch.gulimall.product.service.CategoryService;
import com.jch.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    /**
     * 首页
     * @param model
     * @return
     */
    @GetMapping({"/", "index.html"})
    public String indexPage(Model model) {
        List<CategoryEntity> categoryEntityList = categoryService.getLevelOneCateGoryList();
        model.addAttribute("catagories", categoryEntityList);
        return "index";
    }

    /**
     * 三级分类菜单
     * @return
     */
    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2Vo>> getCatalogJson() {

        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }
}
