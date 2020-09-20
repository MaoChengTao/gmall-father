package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

public interface SearchService {
    /**
     * 上架：将商品对应的 skuId 保存到 es 中
     *
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 下架：将商品对应的 skuId 在 es 中删除
     *
     * @param skuId
     */
    void lowerGoods(Long skuId);

    /**
     * 更新商品的热度值
     *
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 根据用户输入的条件查询数据
     * @param searchParam 查询条件对象
     * @return 返回搜索结果集实体
     * @throws IOException
     */
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
