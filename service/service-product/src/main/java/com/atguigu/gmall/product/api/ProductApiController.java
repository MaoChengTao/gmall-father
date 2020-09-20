package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 该类为所有功能的内部接口
 */
@Api(tags = "内部接口-提供数据")
@RestController
@RequestMapping("api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;

    // 根据 skuId 获取sku基本信息与图片信息
    @ApiOperation("根据skuId获取sku基本信息与图片信息")
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getAttrValueList(@PathVariable Long skuId) {
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        return skuInfo;
    }

    // 通过 category3Id 查询分类信息
    @ApiOperation("通过category3Id查询分类信息")
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        BaseCategoryView baseCategoryView = manageService.getCategoryViewByCategory3Id(category3Id);
        return baseCategoryView;
    }

    // 根据 skuId 获取商品价格
    @ApiOperation("根据skuId获取商品价格")
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId) {
        BigDecimal bigDecimal = manageService.getSkuPrice(skuId);
        return bigDecimal;
    }

    // 根据skuId、spuId查询销售属性集合
    @ApiOperation("根据 skuId、spuId 查询销售属性集合")
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId) {
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuId, spuId);
        return spuSaleAttrList;
    }

    // 根据spuId获取其所有销售属性值组合成的skuId的Map集合
    @ApiOperation("根据 spuId 获取其 所有销售属性值 组合成的skuId的Map集合")
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId) {
        return manageService.getSkuValueIdsMap(spuId);
    }

    // 获取所有分类数据
    @ApiOperation("获取所有分类数据")
    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList() {
        List<JSONObject> list = manageService.getBaseCategoryList();
        return Result.ok(list);
    }

    // 通过品牌Id来查询品牌数据
    @ApiOperation(value = "通过品牌Id来查询品牌数据")
    @GetMapping("inner/getTrademark/{tmId}")
    public Result getTrademark(@PathVariable Long tmId) {
        BaseTrademark baseTrademark = manageService.getTrademarkByTmId(tmId);
        return Result.ok(baseTrademark);
    }

    // 根据 skuId 获取平台属性值集合
    @ApiOperation(value = "根据 skuId 获取平台属性值集合")
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId) {
        return manageService.getAttrList(skuId);
    }
}
