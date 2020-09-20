package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseTrademarkService extends IService<BaseTrademark> {
    // 基本CRUD由 IService接口提供 只需自定义一个分页查询即可

    /**
     * 分页查询品牌列表
     * @param pageParam
     * @return
     */
    IPage<BaseTrademark> getBaseTradeMarkPage(Page<BaseTrademark> pageParam);

    /**
     * 获取品牌属性
     * @return
     */
    List<BaseTrademark> getTrademarkList();
}
