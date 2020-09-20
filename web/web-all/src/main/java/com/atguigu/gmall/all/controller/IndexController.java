package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    //可以访问 www.gmall.com 或者 www.gmall.com/index.html
    @GetMapping({"/", "index.html"})
    public String index(HttpServletRequest request){
        // 远程调用 - 获取全部分类数据
        Result result = productFeignClient.getBaseCategoryList();

        // 全部分类数据 List<JSONObject> list 存储在 Result.data
        // 由页面可知 后台需要存储一个 list 对象
        // 将数据保存进 request 域对象
        request.setAttribute("list",result.getData());

        // 返回页面
        return "index/index";
    }
}
