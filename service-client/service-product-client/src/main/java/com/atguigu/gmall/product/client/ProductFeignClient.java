package com.atguigu.gmall.product.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductFeignClientImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// value 表示服务名
// fallback 表示如果调用服务过程中发生错误，则降级走fallback配置类
@FeignClient(value = "service-product", fallback = ProductFeignClientImpl.class)
public interface ProductFeignClient {

    // 根据 skuId 获取sku基本信息与图片信息
    @GetMapping("api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getAttrValueList(@PathVariable("skuId") Long skuId);

    // 通过 category3Id 查询分类信息
    @GetMapping("api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id);

    // 根据 skuId 获取商品价格
    @GetMapping("api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable("skuId") Long skuId);

    // 根据skuId、spuId查询销售属性集合
    @GetMapping("api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,
                                                   @PathVariable("spuId") Long spuId);

    // 根据spuId获取其所有销售属性值组合成的skuId的Map集合
    @GetMapping("api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable("spuId") Long spuId);

    // 获取所有分类数据
    @GetMapping("api/product/getBaseCategoryList")
    Result getBaseCategoryList();

    // 通过品牌Id来查询品牌数据
    @GetMapping("api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademark(@PathVariable("tmId") Long tmId);

    // 根据skuId获取平台属性值集合
    @GetMapping("api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId);
}
