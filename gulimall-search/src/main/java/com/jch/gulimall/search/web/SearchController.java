package com.jch.gulimall.search.web;

import com.jch.gulimall.search.service.MallSearchService;
import com.jch.gulimall.search.vo.SearchParam;
import com.jch.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class SearchController {

    @Autowired
    private MallSearchService mallSearchService;

    /**
     *
     * @param param
     * @param model
     * @return
     */
    @GetMapping({ "/list.html"})
    public String indexPage(SearchParam param, Model model){
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result", result);
        return "list";
    }
}
