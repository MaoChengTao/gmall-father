package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {

    /**
     * 获取一级分类信息
     *
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类id获取二级分类信息
     *
     * @param categoryId1
     * @return
     */
    List<BaseCategory2> getCategory2(Long categoryId1);

    /**
     * 根据二级分类id获取三级分类信息
     *
     * @param categoryId2
     * @return
     */
    List<BaseCategory3> getCategory3(Long categoryId2);

    /**
     * 根据分类id获取平台属性数据
     * @param categoryId1
     * @param categoryId2
     * @param categoryId3
     * @return
     */
    List<BaseAttrInfo> getBaseAttrInfoList(Long categoryId1, Long categoryId2, Long categoryId3);

    /**
     * 添加or修改平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性id获取平台属性值集合
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);

    /**
     * 根据平台属性id获取平台属性值集合
     * @param attrId
     * @return
     */
    BaseAttrInfo getBaseValueInfo(Long attrId);


    //http://api.gmall.com/admin/product/ {page}/{limit}?category3Id=61
    /**
     * 根据三级分类id获取spu分页列表
     * 分页要确定：显示页码和显示数据条数 封装到 Page
     * 查询条件：封装到 SpuInfo
     * @param pageParam 分页数据
     * @param spuInfo 查询条件
     * @return
     */
    IPage<SpuInfo> getSpuInfoPageList(Page<SpuInfo> pageParam,SpuInfo spuInfo);

    /**
     * 获取销售属性
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存spu
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId获取图片列表
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 根据spuId获取销售属性和销售属性值
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 保存 sku
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 获取sku分页列表
     * @param pageParam
     * @return
     */
    IPage<SkuInfo> getSkuList(Page<SkuInfo> pageParam);

    /**
     * 上架
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 下架
     * @param skuId
     */
    void cancelSale(Long skuId);

    /**
     * 根据 skuId 获取sku基本信息与图片信息
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 通过 category3Id 查询分类信息
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    /**
     * 根据 skuId 获取商品价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据 skuId、spuId 查询销售属性集合
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 根据 spuId 获取其 所有销售属性值 组合成的skuId的Map集合
     *如果我们查询的sql语句得出的结果，结果中的字段，没有所对应的实体类。
     *      那么我们可以自己创建 vo 或者 DTO（字段相对多的时候）。
     *      如果查询出来的字段很少，那么我们可以使用 map 来代替实体类。
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);

    /**
     * 获取全部分类数据 - 以 JSON对象封装数据
     * @return 返回 json 对象集合
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 通过 品牌Id 来查询品牌数据
     * @param TmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long TmId);

    /**
     * 根据 skuId 获取平台属性值集合
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
}
