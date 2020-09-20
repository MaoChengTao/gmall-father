package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {
    // 注入远程调用的接口
    @Autowired
    private ProductFeignClient productFeignClient;

    // 注入线程池
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    // 注入远程调用的接口
    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {

        Map<String, Object> result = new HashMap<>();

        // 使用  CompletableFuture异步编排 多线程远程调用 获取数据

        /**
         *  1 根据 skuId 查询 skuInfo
         *  需要返回 skuInfo，使用 supplyAsync 创建异步对象。(supplyAsync可以支持返回值)
         */
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);
            // 保存skuInfo
            result.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);

        /**
         *  2 通过 category3Id 查询分类信息
         *  需要使用 skuCompletableFuture 创建 CompletableFuture，使用 skuCompletableFuture 的返回结果 skuInfo 获取 category3Id;
         *  消费 skuCompletableFuture 的 skuInfo。查询出来的结果直接保存到 map，无返回结果，使用 thenAcceptAsync;
         *  (thenAcceptAsync 方法：消费处理结果。接收任务的处理结果，并消费处理，无返回结果。)
         */
        CompletableFuture<Void> categoryCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            // 保存商品分类数据
            result.put("categoryView", categoryView);
        }, threadPoolExecutor);


        /**
         *  3 根据 skuId 获取 sku价格
         *  直接使用传递的参数 skuId 查询价格 不依赖其他线程 故直接创建 CompletableFuture
         *  并且没有返回值 使用 runAsync 方法(runAsync方法不支持返回值)
         */
        CompletableFuture priceCompletableFuture = CompletableFuture.runAsync(() ->{
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            // 保存价格
            result.put("price", skuPrice);
        },threadPoolExecutor);

        /**
         *  4 根据 spuId、skuId 查询销售属性集合
         *  需要使用 skuCompletableFuture 创建 CompletableFuture，使用 skuCompletableFuture 的返回结果 skuInfo 获取 spuId;
         *  消费 skuCompletableFuture 的 skuInfo。查询出来的结果直接保存到 map，无返回结果，使用 thenAcceptAsync;
         * (thenAcceptAsync 方法：消费处理结果。接收任务的处理结果，并消费处理，无返回结果。)
         */
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            // 保存销售属性等数据
            result.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);

        /**
         *  5 根据 spuId 查询 map 集合属性
         *  需要使用 skuCompletableFuture 创建 CompletableFuture，使用 skuCompletableFuture 的返回结果 skuInfo 获取 spuId;
         *  消费 skuCompletableFuture 的 skuInfo。查询出来的结果直接保存到 map，无返回结果，使用 thenAcceptAsync;
         *  (thenAcceptAsync 方法：消费处理结果。接收任务的处理结果，并消费处理，无返回结果。)
         */
        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            // map 转换为 json字符串
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            // 保存销售属性和销售属性值组成的jason数据 ，需要将 map 变为 jason valuesSkuJson
            result.put("valuesSkuJson", valuesSkuJson);
        }, threadPoolExecutor);

        // *** 远程调用 更新商品的热度值 (使用CompletableFuture异步编排)***
        CompletableFuture<Void> hotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);

        // 6 多任务组合
        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                categoryCompletableFuture,
                priceCompletableFuture,
                spuSaleAttrCompletableFuture,
                skuValueIdsMapCompletableFuture,
                hotScoreCompletableFuture).join();

        // 7 保存后返回 map
        return result;

        /** 未使用 CompletableFuture异步编排
        // 1 根据 skuId 获取sku基本信息与图片信息
        SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);

        // 2 通过 category3Id 查询分类信息
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());

        // 3 根据 skuId 获取商品价格
        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);

        // 4 根据skuId、spuId查询销售属性集合
        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());

        // 5 根据spuId获取其所有销售属性值组合成的skuId的Map集合
        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
        // map 转换为 json字符串
        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);

        // 6 为 map 赋值
        // 赋值需要注意：这个 key 跟商品详情页面获取的 key 有直接关系！要保持一致

        // 保存skuInfo
        result.put("skuInfo", skuInfo);
        // 保存商品分类数据
        result.put("categoryView", categoryView);
        // 保存价格
        result.put("price", skuPrice);
        // 保存销售属性等数据
        result.put("spuSaleAttrList", spuSaleAttrList);
        // 保存销售属性和销售属性值组成的jason数据 ，需要将 map 变为 jason valuesSkuJson
        result.put("valuesSkuJson", valuesSkuJson);*/

    }
}
