package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @RequestMapping("addCart.html")
    public String addCart(@RequestParam(name = "skuId") Long skuId,
                          @RequestParam(name = "skuNum") Integer skuNum,
                          HttpServletRequest request) {

        cartFeignClient.addToCart(skuId, skuNum);

        // 页面需要 skuInfo 和  skuNum
        SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);

        return "cart/addCart";
    }

    /**
     * 查看购物车
     * <a href="/cart.html" class="sui-btn btn-xlarge btn-danger " target="_blank">去购物车结算 > </a>
     * cart/index 页面使用 v-for="item in data" 渲染页面
     * data 就是 CartApiController.cartList() ===>>> return Result.ok(cartList);
     *
     * @param request
     * @return
     */
    @RequestMapping("cart.html")
    public String addCart(HttpServletRequest request) {
        return "cart/index";
    }
}
