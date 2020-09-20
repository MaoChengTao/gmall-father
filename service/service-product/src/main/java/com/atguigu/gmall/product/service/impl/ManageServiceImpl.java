package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {
    // 注入Mapper
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long categoryId1) {
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();

        wrapper.eq("category1_id", categoryId1);

        return baseCategory2Mapper.selectList(wrapper);
    }

    @Override
    public List<BaseCategory3> getCategory3(Long categoryId2) {
        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();

        wrapper.eq("category2_id", categoryId2);

        return baseCategory3Mapper.selectList(wrapper);
    }

    @Override
    public List<BaseAttrInfo> getBaseAttrInfoList(Long categoryId1, Long categoryId2, Long categoryId3) {
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.getBaseAttrInfoList(categoryId1, categoryId2, categoryId3);
        return baseAttrInfoList;
    }


    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 添加or修改平台属性

        // 1 保存 平台属性
        // 1.1 判断是否有 baseAttrInfo.id
        if (baseAttrInfo.getId() != null) {
            // id不为空说明为 更新操作
            baseAttrInfoMapper.updateById(baseAttrInfo);
        } else {
            // 无 id 说明为 添加操作
            baseAttrInfoMapper.insert(baseAttrInfo);
        }


        // 2 保存 平台属性值

        // *** 平台属性属性值保存在 attrValueList，里面有多个属性值对象，系统无法识别要修改哪个一个
        // *** 采用：根据平台属性id将对应的 平台属性值 删除，再新增
        baseAttrValueMapper.delete(new QueryWrapper<BaseAttrValue>().eq("attr_id", baseAttrInfo.getId()));


        // 2.1 从平台属性 取出 attrValueList
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

        // 2.2 判断取出来的 attrValueList 不为空
        if (!CollectionUtils.isEmpty(attrValueList)) {

            // 2.3 循环遍历 attrValueList ，取出一个个平台属性值 插入到表中
            for (BaseAttrValue baseAttrValue : attrValueList) {

                // 2.4 注意细节：设置 baseAttrValue 的 attrId == baseAttrInfo.id
                baseAttrValue.setAttrId(baseAttrInfo.getId());

                baseAttrValueMapper.insert(baseAttrValue);
            }
        }

    }

    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        // select * from base_attr_value where attr_id = attrId
        QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_id", attrId);

        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectList(wrapper);

        return baseAttrValueList;
    }


    @Override
    public BaseAttrInfo getBaseValueInfo(Long attrId) {
        // 1 查询 平台属性
        // select * from base_attr_info where id = attrId
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);

        if (baseAttrInfo != null) {
            // 2 设置 平台属性值(根据平台属性id获取平台属性值集合)
            baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        }
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> getSpuInfoPageList(Page<SpuInfo> pageParam, SpuInfo spuInfo) {
        // 创建查询条件对象
        QueryWrapper<SpuInfo> wrapper = new QueryWrapper<>();
        // 根据三级分类id查询
        wrapper.eq("category3_id", spuInfo.getCategory3Id());
        wrapper.orderByDesc("id");
        // 调用分页查询
        IPage<SpuInfo> spuInfoIPage = spuInfoMapper.selectPage(pageParam, wrapper);

        return spuInfoIPage;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrList = baseSaleAttrMapper.selectList(null);
        return baseSaleAttrList;
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        /**
         * 需要保存的bean
         *  spuInfo
         *  spuImage
         *  SpuSaleAttr
         *  SpuSaleAttrValue
         */

        // 1 spuInfo
        spuInfoMapper.insert(spuInfo);

        // 2 spuImage
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();

        if (!CollectionUtils.isEmpty(spuImageList)) {
            // 2.1 循环遍历取出 spuImage 插入数据库
            for (SpuImage spuImage : spuImageList) {

                // 2.2 设置 spuId 页面没有传递 spuId
                spuImage.setSpuId(spuInfo.getId());

                spuImageMapper.insert(spuImage);
            }
        }

        // 3 SpuSaleAttr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();

        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            // 3.1 循环遍历取出 spuSaleAttr 插入数据库
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {

                // 3.2 设置 spuId 页面没有传递 spuId
                spuSaleAttr.setSpuId(spuInfo.getId());

                spuSaleAttrMapper.insert(spuSaleAttr);

                // 4 SpuSaleAttrValue
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();

                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    // 4.1 循环遍历取出 spuSaleAttr 插入数据库
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {

                        // 4.2 设置 spuId 页面没有传递 spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());

                        // 4.3 设置 saleAttrName 页面没有传递 saleAttrName
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());

                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> wrapper = new QueryWrapper<>();

        wrapper.eq("spu_id", spuId);

        List<SpuImage> spuImageList = spuImageMapper.selectList(wrapper);

        return spuImageList;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        // 根据spuId获取销售属性和销售属性值
        // 多表关联查询 编写xml 配置sql
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        /**
         * 需要保持的数据
         *  skuInfo：库存单元表
         *  skuImage：库存单元图片表
         *  skuSaleAttrValue：sku与销售属性值表（sku与销售属性值的关系表）
         *  skuAttrValue：sku与平台属性值表（sku与平台属性值关系表）
         */

        // 1 skuInfo：库存单元表
        skuInfoMapper.insert(skuInfo);

        // 2 skuImage：库存单元图片表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();

        if (!CollectionUtils.isEmpty(skuImageList)) {

            for (SkuImage skuImage : skuImageList) {
                // 2.1 设置 skuId 页面没有传递
                skuImage.setSkuId(skuInfo.getId());

                skuImageMapper.insert(skuImage);
            }
        }

        // 3 skuSaleAttrValue：sku与销售属性值表（sku与销售属性值的关系表）
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();

        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {

            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                // 页面传递了 saleAttrValueId，还需 spuId 和 skuId
                // 3.1 设置 spuId
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                // 3.2 设置 skuId
                skuSaleAttrValue.setSkuId(skuInfo.getId());

                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }

        // 4 skuAttrValue：sku与平台属性值表（sku与平台属性值关系表）
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();

        if (!CollectionUtils.isEmpty(skuAttrValueList)) {

            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                // 4.1 设置 skuId 页面没有传递
                skuAttrValue.setSkuId(skuInfo.getId());

                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

        // 保存商品后 - 发送消息 - 商品上架
        // 上架的时候我们在 service-list 中 SearchServiceImpl.upperGoods(Long skuId)通过 skuId 获取已经上架的商品
        // 故参数三发送的消息为 skuId 来确定商品上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuInfo.getId());
    }

    @Override
    public IPage<SkuInfo> getSkuList(Page<SkuInfo> pageParam) {
        // 根据id倒排
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        // 调用分页查询
        IPage<SkuInfo> skuInfoList = skuInfoMapper.selectPage(pageParam, wrapper);

        return skuInfoList;
    }

    @Override
    public void onSale(Long skuId) {
        // 修改 skuInfo 字段  is_sale = 1
        SkuInfo skuInfo = new SkuInfo();

        skuInfo.setId(skuId);// 指定要修改的商品skuId

        skuInfo.setIsSale(1);

        // 更改状态 不影响其他字段
        // UPDATE sku_info SET is_sale=? WHERE id=?
        skuInfoMapper.updateById(skuInfo);

        // 发送消息 - 商品上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }

    @Override
    public void cancelSale(Long skuId) {
        // 修改 skuInfo 字段  is_sale = 0
        SkuInfo skuInfo = new SkuInfo();

        skuInfo.setId(skuId);// 指定要修改的商品skuId

        skuInfo.setIsSale(0);

        // 更改状态 不影响其他字段
        // UPDATE sku_info SET is_sale=? WHERE id=?
        skuInfoMapper.updateById(skuInfo);

        // 发送消息 - 商品下架
        // 注意路由键为 MqConst.ROUTING_GOODS_LOWER 下架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);
    }

    @GmallCache(prefix = "sku:")
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        return getSkuInfoDB(skuId);
    }

    /**
     * 使用 Redisson 实现缓存 + 分布式锁
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoByRedisson(Long skuId) {
        // 创建一个 SkuInfo 对象
        SkuInfo skuInfo = null;

        try {
            // 定义一个缓存的 key = sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;

            // 从缓存里查询数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);

            // 判断从缓存中获取的数据是否为空
            if (skuInfo == null) {// 缓存中获取的数据为空：查询 DB + 查询到的数据放入缓存

                // 创建一个锁的 key = sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;

                // 使用 redisson 获取锁
                RLock lock = redissonClient.getLock(lockKey);

                // 尝试上锁
                boolean isSuccess = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);

                // 判断上锁是否成功
                if (isSuccess) {// true 上锁成功
                    try {
                        // 查询 DB
                        skuInfo = getSkuInfoDB(skuId);

                        // 判断数据库中的数据是否存在【防止缓存穿透】
                        if (skuInfo == null) {
                            // 创建一个空对象
                            SkuInfo skuInfo1 = new SkuInfo();

                            // 将空对象放入缓存 并设置一个【短暂】的过期时间
                            redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);

                            // 返回空对象
                            return skuInfo1;
                        }

                        // 将查询到的真正数据存入缓存 设置一个【较长】的过期时间
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);

                        // 返回真正查询到的数据
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // 释放锁
                        lock.unlock();
                    }

                } else {// false 上锁失败
                    Thread.sleep(1000);
                    return getSkuInfoDB(skuId);
                }

            } else {// 缓存中获取的数据不为空：返回缓存中的数据
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 为了防止缓存宕机：从数据库中获取数据
        return getSkuInfoDB(skuId);
    }

    /**
     * 使用 Redis 的 Set命令 + Lua脚本 实现分布式锁
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoByRedisSetLua(Long skuId) {
        // 添加缓存！先查询缓存，如果缓存没有再查询数据库，并将数据放入缓存！！！

        // 创建 skuInfo 接收查询到的数据
        SkuInfo skuInfo = null;

        try {
            // 定义一个缓存的 key=sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;

            // 从缓存里获取数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);

            // 判断从缓存中获取的数据是否为空
            if (skuInfo == null) {// 缓存中获取的数据为空：查询 DB + 查询到的数据放入缓存

                // 设置 uuid
                String uuid = UUID.randomUUID().toString();

                // 设置 锁 lockKey = sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;

                // 使用 Redis 的 Set 执行上锁命令【防止缓存击穿】
                Boolean isSuccess = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);

                // 判断上锁是否成功
                if (isSuccess) {// true 上锁成功
                    // 查询 DB 获取数据
                    skuInfo = getSkuInfoDB(skuId);// 可能数据根本不存在，会出现【缓存穿透】

                    if (skuInfo == null) {
                        // 数据不存在 创建空对象
                        SkuInfo skuInfo1 = new SkuInfo();

                        // 空对象放进缓存 同时设置一个【较短的过期时间】
                        redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);

                        // 返回空对象
                        return skuInfo1;
                    }

                    // 将真正的数据放入缓存 商品详情页面数据应该有个较长的过期时间
                    redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);

                    // 获取完数据 使用 Lua脚本 释放锁
                    // 定义 Lua 脚本：这个脚本只在客户端传入的值和键的口令串相匹配时，才对键进行删除。
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

                    // 将  Lua 脚本 放进 defaultRedisScript
                    DefaultRedisScript defaultRedisScript = new DefaultRedisScript();
                    defaultRedisScript.setScriptText(script);

                    // 设置 Lua 脚本 返回值类型
                    // 因为删除判断的时候，返回的0,给其封装为数据类型。如果不封装那么默认返回String 类型，那么返回字符串与0 会有发生错误。
                    defaultRedisScript.setResultType(Long.class);

                    // 执行 Lua 脚本 ===>>> 删除锁
                    // 第一个要是 script 脚本 ，第二个需要判断的 锁的 key，第三个就是 key 所对应的值
                    redisTemplate.execute(defaultRedisScript, Arrays.asList(lockKey), uuid);

                    // 返回真正查询到的数据
                    return skuInfo;

                } else {// false 上锁失败
                    try {
                        // 等待其他线程执行完 同时获取 skuInfo
                        Thread.sleep(1000);
                        return getSkuInfoDB(skuId);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } else {// 缓存中获取的数据不为空：返回缓存中的数据
                return skuInfo;
            }
        } catch (Exception e) {
            // 如果有日志系统 这个地方应该记录日志
            e.printStackTrace();
        }
        // 如果Redis宕机了，redisTemplate和获取数据的时候 都会出现异常！！！
        // 如果Redis宕机了，使用查询数据库兜底！！！
        return getSkuInfoDB(skuId);
    }

    /**
     * 根据 skuId 查询 skuInfo 基本信息、图片列表集合(从数据库查询)
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoDB(Long skuId) {
        // 根据 skuId 获取sku基本信息与图片信息
        // 1  根据 skuId 获取sku基本信息
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        if (skuInfo != null) {// 不为空才执行以下操作
            // 2 根据 skuId 获取图片信息
            QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
            wrapper.eq("sku_id", skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(wrapper);

            // 3 为 skuInfo 设置 skuImageList
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;// 为空直接返回 null
    }

    @GmallCache(prefix = "categoryView:")
    @Override
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        // 表：base_category_view 主键id 就是 category3Id
        BaseCategoryView baseCategoryView = baseCategoryViewMapper.selectById(category3Id);

        return baseCategoryView;
    }

    @GmallCache(prefix = "price:")
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        // 商品价格在 skuInfo 里
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            BigDecimal price = skuInfo.getPrice();
            return price;
        }
        return new BigDecimal(0);
    }

    @GmallCache(prefix = "spu:")
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        // 调用 mapper 编写 xml
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    @GmallCache(prefix = "skuValueIdsMap:")
    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        // 1 返回的是一个Map 定义一个Map保存数据
        // json串{"86|89":"32","87|90":"33"}、map.put{"86|89","32"}。
        Map<Object, Object> map = new HashMap<>();

        // 2 没有实体类 使用map封装数据 多行数据使用 List<Map>
        List<Map> mapList = skuSaleAttrValueMapper.selectSkuSaleAttrValuesBySpu(spuId);

        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map skuMap : mapList) {
                // 3 从 skuMap 取出对应的key，存入 map，例如 map.put{"86|89","32"}。
                map.put(skuMap.get("value_ids"), skuMap.get("sku_id"));
            }
        }
        return map;
    }

    @GmallCache(prefix = "category:")
    @Override
    public List<JSONObject> getBaseCategoryList() {
        // 定义个 Json对象集合 封装数据
        List<JSONObject> allCategoryJsonObjectList = new ArrayList<>();

        // 先获取所有分类数据
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);

        // 按照 一级分类id 进行分组（根据分析的json数据格式 先获取一级分类数据 ）
        // Function R apply(T t);
        // key = 一级分类id | value = List<BaseCategoryView> 一级分类数据集合
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(baseCategoryView -> baseCategoryView.getCategory1Id()));

        // 定义确定一级分类个数的变量 初始值为1
        int index = 1;

        // 获取一级分类下所有数据：数据都在 category1Map 的 value 里
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            // 获取一级分类id
            Long category1Id = entry1.getKey();

            // 获取一级分类下的所有集合（因为是二级分类的数据 故取名 category2List）
            List<BaseCategoryView> category2List = entry1.getValue();

            // 获取一级分类名称 categoryName(因为进行分组了：同一个分类下所有名称一样 故从集合中取第一个即可)
            String category1Name = category2List.get(0).getCategory1Name();

            // 声明一个 JSONObject 存储 一级分类数据
            JSONObject category1JsonObject = new JSONObject();
            category1JsonObject.put("index", index);// 注意key保持和json格式一致
            category1JsonObject.put("categoryId", category1Id);
            category1JsonObject.put("categoryName", category1Name);
            //category1JsonObject.put("categoryChild",二级分类集合);

            // 变量迭代，更新一级分类个数
            index++;

            // 按照 二级分类id 进行分组（使用从一级分类取出的数据集合 category2List 获取二级分类数据）
            // key = 二级分类id | value = List<BaseCategoryView> 二级分类数据集合
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(baseCategoryView -> baseCategoryView.getCategory2Id()));

            // 声明二级分类对象集合 category2Child ：存储同一个一级分类下所有的二级分类数据
            List<JSONObject> category2Child = new ArrayList<>();

            // 循环遍历获取二级分类数据
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                // 获取二级分类id
                Long category2Id = entry2.getKey();

                // 获取二级分类下的所有数据集合(因为是三级分类的数据 故取名 category3List)
                List<BaseCategoryView> category3List = entry2.getValue();

                // 获取二级分类名称 categoryName(因为进行分组了：同一个分类下所有名称一样 故从集合中取第一个即可)
                String category2Name = category3List.get(0).getCategory2Name();

                // 声明一个 JSONObject 存储 二级分类数据
                JSONObject category2JsonObject = new JSONObject();
                category2JsonObject.put("index", index);// 注意key保持和json格式一致
                category2JsonObject.put("categoryId", category2Id);
                category2JsonObject.put("categoryName", category2Name);
                //category2JsonObject.put("categoryChild",三级分类集合);

                // 将每次循环的二级分类数据 放入 category2Child 集合
                category2Child.add(category2JsonObject);

                // 声明三级分类对象集合 category3Child ：存储同一个二级分类下所有的三级分类数据
                List<JSONObject> category3Child = new ArrayList<>();

                // 获取三级分类数据（三级分类数据没有重复的 不需要分组）
                category3List.forEach((category3View) -> {
                    // 获取三级分类id
                    Long category3Id = category3View.getCategory3Id();
                    // 获取三级分类名称
                    String category3Name = category3View.getCategory3Name();

                    // 声明一个 JSONObject 存储 三级分类数据
                    JSONObject category3JsonObject = new JSONObject();
                    category3JsonObject.put("categoryId", category3Id);
                    category3JsonObject.put("categoryName", category3Name);

                    //将每次循环的三级分类数据 放入 category3Child 集合
                    category3Child.add(category3JsonObject);
                });
                // 将 三级分类数据集合 放入 二级分类Json对象
                category2JsonObject.put("categoryChild", category3Child);
            }
            // 将 二级分类数据集合 放入 一级分类Json对象
            category1JsonObject.put("categoryChild", category2Child);

            // 将所有的 一级分类数据(一级分类Json对象) 放入到 allCategoryJsonObjectList 集合中
            allCategoryJsonObjectList.add(category1JsonObject);
        }
        // 返回 所有分类数据
        return allCategoryJsonObjectList;
    }

    @Override
    public BaseTrademark getTrademarkByTmId(Long TmId) {

        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(TmId);

        return baseTrademark;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        // 使用 xml 完成复制 sql
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);

    }
}
