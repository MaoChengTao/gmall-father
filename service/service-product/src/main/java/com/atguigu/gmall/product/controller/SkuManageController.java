package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "SKU管理接口")
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    // http://api.gmall.com/admin/product/spuImageList/{spuId}
    @ApiOperation(value = "根据spuId获取图片列表")
    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId) {
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }

    //http://api.gmall.com/admin/product/spuSaleAttrList/{spuId}
    @ApiOperation(value = "根据spuId获取销售属性和销售属性值")
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId) {
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }

    // http://api.gmall.com/admin/product/saveSkuInfo
    @ApiOperation(value = "添加sku")
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo) {// 前端传递的 json字符串 对应 SkuInfo实体类
        // 调用 service
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    // http://api.gmall.com/admin/product/list/{page}/{limit}
    @ApiOperation(value = "获取sku分页列表")
    @GetMapping("list/{page}/{limit}")
    public Result getSkuList(@PathVariable Long page, @PathVariable Long limit) {
        // 创建分页对象
        Page<SkuInfo> pageParam = new Page<>(page, limit);
        // 调用service
        IPage<SkuInfo> skuInfoList = manageService.getSkuList(pageParam);

        return Result.ok(skuInfoList);
    }

    // http://api.gmall.com/admin/product/onSale/{skuId}
    @ApiOperation(value = "上架")
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        manageService.onSale(skuId);
        return Result.ok();
    }

    // http://api.gmall.com/admin/product/cancelSale/{skuId}
    @ApiOperation(value = "下架")
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}
