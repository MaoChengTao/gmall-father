package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemFeignClientImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-item", fallback = ItemFeignClientImpl.class)
public interface ItemFeignClient {

    /**
     * 根据skuId获取商品详情信息
     */
    @GetMapping("api/item/{skuId}")
    Result getItem(@PathVariable("skuId") Long skuId);
}
