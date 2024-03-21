package com.jch.gulimall.order.web;

import com.jch.gulimall.order.service.OrderService;
import com.jch.gulimall.order.vo.OrderConfirmVo;
import com.jch.gulimall.order.vo.OrderSubmitVo;
import com.jch.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    /**
     * 订单确认页
     * @param model
     * @return
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.orderConfirm();
        model.addAttribute("confirmOrderData", confirmVo);
        return "confirm";
    }

    /**
     * 下订单
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes attributes) {
        SubmitOrderResponseVo orderVo = orderService.submitOrder(vo);
        if (orderVo.getCode() == 0) {
            // 下单成功来到支付选择页
            return "pay";
        } else {
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
