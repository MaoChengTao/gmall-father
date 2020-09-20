package com.atguigu.gmall.item.service;

import java.util.Map;

public interface ItemService {
    /**
     * 接口返回值类型定义为一个map
     * 例如：map.put（“skuInfo”，skuInfo）; map.put（“price”，price）…
     * 获取sku详情信息
     * @param skuId
     * @return
     */
    Map<String,Object> getBySkuId(Long skuId);
}
