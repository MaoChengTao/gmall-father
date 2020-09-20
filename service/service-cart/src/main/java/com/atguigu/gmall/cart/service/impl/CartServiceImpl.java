package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CartAsyncService cartAsyncService;

    /**
     * 加入购物车
     *
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
         /*
         添加购物车的思路：
            1 如果购物车中【无】当前添加的商品 则数据直接插入
            2 如果购物车中【有】当前添加的商品 则商品数量累加
            3 mysql 与 redis 做数据同步
         */
        // 获取缓存中购物车的 key
        String cartKey = getCartKey(userId);

        // 先把数据加载进缓存
        if (!redisTemplate.hasKey(cartKey)) {
            loadCartCache(userId);
        }

        // 查询数据库中前加入购物车的商品
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("sku_id", skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(wrapper);

        // 判断数据库中是否有该商品
        if (cartInfoExist != null) {// 【不为空】说明购物车中有当前添加的商品
            // 购物车中商品数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);

            // 获取最新价格【商品价格在skuInfo表 远程调用 productFeignClient 的接口获取】
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);

            // 设置最新价格
            cartInfoExist.setSkuPrice(skuPrice);

            // 更新数据库数据
            //cartInfoMapper.updateById(cartInfoExist);
            // 异步操作 更新数据库数据
            cartAsyncService.updateCartInfo(cartInfoExist);

            // TODO 已更新缓存中的数据
            // redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);
        } else {// 【为空】说明购物车中没有当前添加的商品
            // 创建购物车实例
            CartInfo cartInfo = new CartInfo();

            // 为购物车赋值，购物车数据来自：购物车 --- 商品详情 --- 商品后台
            SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);

            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuNum(skuNum);

            cartInfo.setSkuPrice(skuInfo.getPrice());// 第一次添加时：实时价格 == skuInfo.price
            cartInfo.setCartPrice(skuInfo.getPrice());// 添加购物车时的价格：默认是最新价格
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            //cartInfo.setIsChecked();// 默认值为值 表示被选中

            // 插入数据库
            //cartInfoMapper.insert(cartInfo);
            // 异步操作 插入数据库
            cartAsyncService.saveCartInfo(cartInfo);

            // 废物利用
            cartInfoExist = cartInfo;
        }
        // 设置缓存同步

        // 使用 hash 存储数据 [废物利用cartInfoExist]
        redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfoExist);

        // 设置过期时间
        setCartKeyExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {

        List<CartInfo> cartInfoList = new ArrayList<>();

        // 未登录：临时用户Id 获取未登录的购物车数据
        if (StringUtils.isEmpty(userId)) {
            cartInfoList = getCartList(userTempId);
        }

        /*
         1. 准备合并购物车
         2. 获取未登录的购物车数据
         3. 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同 则数量相加，合并完成之后，删除未登录的数据！
         4. 如果未登录购物车没有数据，则直接显示已登录的数据
        */


        if (!StringUtils.isEmpty(userId)) {
            // 获取未登录情况下购物车的数据
            List<CartInfo> cartInfoNoLoginList = getCartList(userTempId);

            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)) {

                // 未登录购物车有数据 则合并 合并的条件 skuId 相同
                cartInfoList = mergeToCartList(cartInfoNoLoginList, userId);

                // 删除未登录购物车的数据
                deleteCartList(userTempId);
            }
            // 细节：如果未登录情况下购物车数据为空 或者 临时用户Id为空 都会直接查询数据库
            if (StringUtils.isEmpty(userTempId) || CollectionUtils.isEmpty(cartInfoNoLoginList)) {
                cartInfoList = getCartList(userId);
            }
        }
        return cartInfoList;
    }

    /**
     * 选中状态变更
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        // 异步操作数据库
        cartAsyncService.checkCart(userId, isChecked, skuId);

        // 更新redis缓存
    /*
        hset(key,field,value)

        获取所有数据：
        BoundHashOperations<String, String, CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);

        通过 key,field 获取 value： hget(key,field)
        CartInfo cartInfoUpd = boundHashOperations.get(skuId.toString());

        存放数据：hput(field,value)
        boundHashOperations.put(skuId.toString(), cartInfoUpd);
     */

        // 定义key user:userId:cart
        String cartKey = this.getCartKey(userId);

        // 通过 cartKey 获取所有数据
        BoundHashOperations<String, String, CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);

        // 判断这个 hash 中是否有对应的 key
        if (boundHashOperations.hasKey(skuId.toString())) {
            // 准备更新数据
            CartInfo cartInfoUpd = boundHashOperations.get(skuId.toString());
            // 设置选中状态
            cartInfoUpd.setIsChecked(isChecked);
            // 将更新完成的对象写入缓存
            boundHashOperations.put(skuId.toString(), cartInfoUpd);
            // 设置过期时间
            setCartKeyExpire(cartKey);
        }
    }


    /**
     * 删除购物车
     *
     * @param userId
     * @param skuId
     */
    @Override
    public void deleteCartInfo(String userId, Long skuId) {
        // 异步操作 删除数据库中的数据
        cartAsyncService.deleteCartInfo(userId, skuId);

        // 定义缓存中的数据
        String cartKey = getCartKey(userId);

        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);

        if (boundHashOperations.hasKey(skuId.toString())) {
            // 删除缓存中的数据
            boundHashOperations.delete(skuId.toString());
        }
    }

    /**
     * 根据 用户Id 查询购物车中选择的商品列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        // 定义一个购物车集合
        List<CartInfo> cartInfoList = new ArrayList<>();

        // 注意：从购物车列表页面点击去结算，缓存一定会有数据，故从缓存中获取数据

        // 获取缓存中的 key
        String cartKey = getCartKey(userId);

        // 获取 key 对应所有的数据集合
        List<CartInfo> cartInfoInRedisList = redisTemplate.opsForHash().values(cartKey);

        // 判断
        if (!CollectionUtils.isEmpty(cartInfoInRedisList) && cartInfoInRedisList.size() > 0) {
            // 循环遍历获取 cartInfo
            for (CartInfo cartInfo : cartInfoInRedisList) {
                // 判断商品的选中状态
                if (cartInfo.getIsChecked().intValue() == 1) {
                    // 添加被选中的商品进 cartInfoList
                    cartInfoList.add(cartInfo);
                }
            }

        }
        return cartInfoList;
    }

    /**
     * 合并后删除未登录购物车数据
     *
     * @param userTempId
     */
    private void deleteCartList(String userTempId) {
        // 需要删除数据库中的数据 和 缓存中的数据

        // 删除数据库中的数据
        /*QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userTempId);
        cartInfoMapper.delete(wrapper);*/
        cartAsyncService.deleteCartInfo(userTempId);

        // 获取缓存中的key 删除对应的缓存
        String cartKey = getCartKey(userTempId);

        // 判断缓存中是否有对应的key
        Boolean flag = redisTemplate.hasKey(cartKey);

        if (flag) {
            redisTemplate.delete(cartKey);
        }
    }

    /**
     * 合并购物车数据
     *
     * @param cartInfoNoLoginList
     * @param userId
     * @return
     */
    private List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
          /*
        demo1:
            登录：
                37 1
                38 1
            未登录：
                37 1
                38 1
                39 1
            合并之后的数据
                37 2
                38 2
                39 1
         demo2:
             未登录：
                37 1
                38 1
                39 1
                40 1
              合并之后的数据
                37 1
                38 1
                39 1
                40 1
          */
        // 合并有两个集合：登录购物车集合、未登录购物车集合

        // 获取登录购物车的集合
        List<CartInfo> cartInfoLoginList = getCartList(userId);

        // 将 cartInfoLoginList 换还为 map key=skuId value=CartInfo
        Map<Long, CartInfo> cartInfoLoginMap = cartInfoLoginList.stream().collect(Collectors.toMap((cartInfo) -> {
            return cartInfo.getSkuId();
        }, (cartInfo) -> {
            return cartInfo;
        }));

        // 循环遍历未登录购物车集合
        for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {

            // 获取未登录购物车对象中的skuId
            Long skuId = cartInfoNoLogin.getSkuId();

            // 判断 cartInfoLoginMap 是否包含相同的skuId
            if (cartInfoLoginMap.containsKey(skuId)) {// 包含相同skuId的数据

                // 获取登录购物车中相同 skuId 的对象
                CartInfo cartInfoLogin = cartInfoLoginMap.get(skuId);

                // 商品数量相加
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum() + cartInfoNoLogin.getSkuNum());

                // 细节：合并选中
                // 未登录购物车处于状态选中的商品 则合并后数据库中数据也为选中状态
                if (cartInfoNoLogin.getIsChecked().intValue() == 1) {
                    cartInfoLogin.setIsChecked(1);
                }

                // 异步操作 更新数据库数据
                cartAsyncService.updateCartInfo(cartInfoLogin);

            } else {// 未包含相同skuId的数据 说明没有相同的商品

                // 将未登录的临时用户Id 设置为已登录的用户Id
                cartInfoNoLogin.setUserId(userId);

                //异步操作 保存数据到数据库
                cartAsyncService.saveCartInfo(cartInfoNoLogin);
            }
        }
        // 获取合并之后所有的数据 统一汇总 查询最新购物车数据
        List<CartInfo> cartInfoList = loadCartCache(userId);

        return cartInfoList;
    }

    /**
     * 根据 userId | userTempId 获取购物车列表数据
     *
     * @param userId
     * @return
     */
    private List<CartInfo> getCartList(String userId) {

        List<CartInfo> cartInfoList = new ArrayList<>();

        // *判断用户Id是否为空【实际是判断临时用户Id是否为空，因为在getCartList()里 临时用户id 也能进入getCartList(userTempId)】*
        if (StringUtils.isEmpty(userId)) {
            return cartInfoList;
        }

        // 根据用户 Id 查询 {先查询缓存，缓存没有，再查询数据库}

        // 获取缓存中的 key
        String cartKey = getCartKey(userId);

        // 获取缓存中的所有数据{ 获取 hash 中所有 value 的数据 这个value存储的是cartInfo.toString  }
        cartInfoList = redisTemplate.opsForHash().values(cartKey);

        // 判断
        if (!CollectionUtils.isEmpty(cartInfoList)) { // 缓存中有数据

            // 购物车列表显示有顺序：按照商品的更新时间 降序{但我们数据库没有设置时间 故选 Id 排序}
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });

            // 返回排序好的集合数据
            return cartInfoList;

        } else { // 缓存中无数据

            // 通过 userId 查询数据库中购物车数据并放入缓存
            cartInfoList = loadCartCache(userId);

            return cartInfoList;
        }
    }

    /**
     * 通过 userId 查询数据库中购物车数据并放入缓存
     *
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        // 一个用户可能有多个商品 使用 selectList()
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(wrapper);

        // 数据库没有数据 返回空对象
        if (CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList;
        }

        // 数据库有数据 将数据库中的数据放入缓存并返回数据

        // 定义 map 保存从 cartInfoList 取出的 CartInfo
        Map<String, CartInfo> map = new HashMap<>();

        for (CartInfo cartInfo : cartInfoList) {
            // *细节：数据库的表中没有 skuPrice 字段 为了保证缓存中 cartInfo.skuPrice 不为空 需要单独设置 cartInfo 的实时价格*
            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);
            // 将 cartInfo 放进 map
            map.put(cartInfo.getSkuId().toString(), cartInfo);
        }
        // 获取缓存中的 key
        String cartKey = getCartKey(userId);

        // 将数据存入缓存
        redisTemplate.boundHashOps(cartKey).putAll(map);

        // 设置过期时间
        setCartKeyExpire(cartKey);

        // 返回从数据库查询到的数据
        return cartInfoList;
    }

    /**
     * 设置过期时间
     *
     * @param cartKey
     */
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存中购物车的 key
     *
     * @param userId
     * @return
     */
    private String getCartKey(String userId) {

        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
