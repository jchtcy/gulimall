package com.jch.gulimall.order.web;

import com.alipay.api.AlipayApiException;
import com.jch.common.utils.PageUtils;
import com.jch.common.vo.order.PayVO;
import com.jch.gulimall.order.config.AliPayConfig;
import com.jch.gulimall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OrderPayController {

    @Autowired
    private AliPayConfig aliPayConfig;

    @Autowired
    private OrderService orderService;

    /**
     * 支付宝支付
     * @param orderSn
     * @return
     */
    @GetMapping(value = "/payOrder",produces = "text/html")
    @ResponseBody
    public String alipay(@RequestParam String orderSn) throws AlipayApiException {
        PayVO payVO = orderService.getOrderPayInfo(orderSn);
        String routePage = aliPayConfig.pay(payVO);
        return routePage;
    }

    @GetMapping("/orderList.html")
    public String orderListPage(@RequestParam(value = "pageNum",required = false,defaultValue = "0") Integer pageNum,
                                Model model, HttpServletRequest request) {
        Map<String,Object> params = new HashMap<>();
        params.put("page",pageNum.toString());
        PageUtils data = orderService.queryPageWithItem(params);
        model.addAttribute("orders",data);
        return "orderList";
    }
}
