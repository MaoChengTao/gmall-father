package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;

    // 商品详情页面
    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model){
        
        // 远程调用接口
        Result<Map> result = itemFeignClient.getItem(skuId);

        // ItemApiController 里 将 汇总数据 封装到了 Result.data属性里了
        // result.getData() ===>>> 是我们需要的 map数据
        // 需要 将 Result 转为 Result<Map>
        model.addAllAttributes(result.getData());

        return "item/index";
    }
}
