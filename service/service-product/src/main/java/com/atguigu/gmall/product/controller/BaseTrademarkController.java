package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "品牌接口管理")
@RestController //  @ResponseBody + @Controller
@RequestMapping("admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    // http://api.gmall.com/admin/product/baseTrademark/{page}/{limit}
    @ApiOperation(value = "获取品牌分页列表")
    @GetMapping("{page}/{limit}")
    public Result getBaseTradeMarkPage(@PathVariable Long page,
                                       @PathVariable Long limit) {

        // 创建分页查询对象
        Page<BaseTrademark> pageParam = new Page<>(page, limit);

        // 调用service
        IPage<BaseTrademark> baseTradeMarkPage = baseTrademarkService.getBaseTradeMarkPage(pageParam);

        return Result.ok(baseTradeMarkPage);
    }

    // http://api.gmall.com/admin/product/baseTrademark/save
    @ApiOperation(value = "添加品牌")
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark) {
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    // http://api.gmall.com/admin/product/baseTrademark/get/{id}
    @ApiOperation(value = "根据Id获取品牌")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    // http://api.gmall.com/admin/product/baseTrademark/update
    @ApiOperation(value = "修改品牌")
    @PutMapping("update")
    public Result update(@RequestBody BaseTrademark baseTrademark) {
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    // http://api.gmall.com/admin/product/baseTrademark/remove/{id}
    @ApiOperation(value = "删除品牌")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    // http://api.gmall.com/admin/product/baseTrademark/getTrademarkList
    @ApiOperation(value = "获取品牌属性")
    @GetMapping("getTrademarkList")
    public Result getTrademarkList(){
        // 方式一：通过自己写的接口获取 品牌属性列表
        // List<BaseTrademark> baseTrademarkList = baseTrademarkService.getTrademarkList();

        // 方式二：通过IService自带的方法获取 品牌属性列表
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.list(null);

        return Result.ok(baseTrademarkList);
    }
}
