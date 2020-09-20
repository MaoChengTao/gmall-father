package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncServiceImpl implements CartAsyncService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Async // 表示异步操作 会自动去找线程池
    @Override
    public void updateCartInfo(CartInfo cartInfo) {
        System.out.println("异步操作数据库 ===>>> 修改 CartInfo");
        cartInfoMapper.updateById(cartInfo);
    }

    @Async // 表示异步操作 会自动去找线程池
    @Override
    public void saveCartInfo(CartInfo cartInfo) {
        System.out.println("异步操作数据库 ===>>> 保存 CartInfo");
        cartInfoMapper.insert(cartInfo);
    }
    @Async // 表示异步操作 会自动去找线程池
    @Override
    public void deleteCartInfo(String userId) {
        System.out.println("异步操作数据库 ===>>> 删除 CartInfo");
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        cartInfoMapper.delete(wrapper);
    }
    @Async // 表示异步操作 会自动去找线程池
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        System.out.println("异步操作数据库 ===>>> 选择状态更新");

        CartInfo cartInfo = new CartInfo();

        // 更新选择状态的字段
        cartInfo.setIsChecked(isChecked);

        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("sku_id", skuId);


        cartInfoMapper.update(cartInfo, wrapper);

    }
    @Async // 表示异步操作 会自动去找线程池
    @Override
    public void deleteCartInfo(String userId, Long skuId) {
        System.out.println("异步===>>>删除购物车数据");
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<CartInfo>();
        wrapper.eq("user_id", userId);
        wrapper.eq("sku_id", skuId);
        cartInfoMapper.delete(wrapper);
    }

}
