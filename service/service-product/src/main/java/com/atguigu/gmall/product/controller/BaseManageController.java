package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@CrossOrigin
@Api(tags = "平台属性管理")
@RestController //  @ResponseBody + @Controller
@RequestMapping("admin/product")
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    // http://api.gmall.com/admin/product/getCategory1
    @ApiOperation(value = "获取一级分类信息")
    @GetMapping("getCategory1")
    public Result getCategory1() {
        List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        return Result.ok(baseCategory1List);
    }

    // http://api.gmall.com/admin/product/getCategory2/{category1Id}
    @ApiOperation(value = "获取二级分类信息(根据category1Id)")
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id) {
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    // http://api.gmall.com/admin/product/getCategory3/{category2Id}
    @ApiOperation(value = "获取三级分类信息(category2Id)")
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id) {
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    //http://api.gmall.com/admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
    @ApiOperation(value = "根据分类id获取平台属性")
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result attrInfoList(@PathVariable Long category1Id,
                               @PathVariable Long category2Id,
                               @PathVariable Long category3Id) {

        List<BaseAttrInfo> attrInfoList = manageService.getBaseAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(attrInfoList);
    }

    // http://api.gmall.com/admin/product/saveAttrInfo
    // 需要前端传递 平台属性 和 平台属性值，前端传递的是Json字符串，使用@RequestBody转换为实体类
    @ApiOperation(value = "添加or修改平台属性")
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        // 调用 service 层保存数据
        manageService.saveAttrInfo(baseAttrInfo);

        return Result.ok();
    }

    // http://api.gmall.com/admin/product/getAttrValueList/{attrId}
    @ApiOperation(value = "根据平台属性id获取平台属性值集合")
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId) {
        // 1 注意：执行修改操作时，要先确定当前【平台属性】 是否存在。存在的话再从 【平台属性】 中取出 【平台属性值集合】
        BaseAttrInfo baseAttrInfo = manageService.getBaseValueInfo(attrId);

        if (baseAttrInfo != null) {
            // 2 从【平台属性】中自动获取到【平台属性值集合】
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            return Result.ok(attrValueList);
        }
        return Result.fail();
        // select * from base_attr_value where attr_id = attrId
        /*List<BaseAttrValue> baseAttrValueList = manageService.getAttrValueList(attrId);
        return Result.ok(baseAttrValueList);*/
    }

    // http://api.gmall.com/admin/product/baseSaleAttrList
    @ApiOperation(value = "获取销售属性")
    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){
        // 调用 service
        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }
}
