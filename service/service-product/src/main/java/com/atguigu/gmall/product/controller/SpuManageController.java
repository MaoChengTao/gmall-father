package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "SPU接口管理")
@RestController //  @ResponseBody + @Controller
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    // http://api.gmall.com/admin/product/{page}/{limit}?category3Id=61
    @ApiOperation(value = "获取spu分页列表(根据三级分类id)")
    @GetMapping("{page}/{limit}")
    public Result getSpuInfoPageList(@PathVariable Long page,
                                     @PathVariable Long limit,
                                     SpuInfo spuInfo) {

        // 创建分页对象
        Page<SpuInfo> pageParam = new Page<>(page, limit);

        // 调用service
        IPage<SpuInfo> spuInfoPageList = manageService.getSpuInfoPageList(pageParam, spuInfo);

        return Result.ok(spuInfoPageList);
    }

    // http://api.gmall.com/admin/product/saveSpuInfo
    @ApiOperation(value = "添加spu")
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {// 前台传递的json字符串刚好对应 SpuInfo 实体类
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }


}
