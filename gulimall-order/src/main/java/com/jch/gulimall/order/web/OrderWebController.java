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

        SubmitOrderResponseVo orderVo = null;
        try {
            orderVo = orderService.submitOrder(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:http://order.gulimall.com/toTrade";
        }
        if (orderVo.getCode() == 0) {
            // 下单成功来到支付选择页
            model.addAttribute("submitOrderResp", orderVo);
            return "pay";
        } else {
            String msg = "下单失败: ";
            switch (orderVo.getCode()) {
                case 1:
                    msg += "订单信息过期, 请刷新再次提交";
                    break;
                case 2:
                    msg += "订单商品价格发生变化, 请确认后再次提交";
                    break;
                case 3:
                    msg += "锁库存失败, 库存商品不足";
                    break;
            }
            attributes.addFlashAttribute("msg", msg);
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
