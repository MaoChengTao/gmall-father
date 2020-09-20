package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    String WARE_URL;// 获取仓库系统的地址 http://localhost:9001

    @Autowired
    private RabbitService rabbitService;

    /**
     * 保存订单信息
     *
     * @param orderInfo
     * @return
     */
    @Override
    public Long saveOrderInfo(OrderInfo orderInfo) {
        /*
         保存订单信息 需要保存两张表：订单表 和 订单明细表

         页面没有传递，订单表目前需要赋值的数据：
         total_amount、order_status、userId、out_trade_no、trade_body、create_time、expire_time、process_status
         */

        // 计算总金额 有 orderInfo 且 orderDetailList 属性有值 直接调用方法计算总金额
        orderInfo.sumTotalAmount();

        // 设置订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());

        // 设置 userId【在实现类无法获得 但是可以在控制器获取 在控制器赋值 userId】

        // 设置 out_trade_no 订单交易编号（第三方支付用)
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);

        // 设置 trade_body 订单描述(第三方支付用)：拼接商品名称
        // 获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        // 定义变量 记录拼接的商品名称
        StringBuilder tradeBody = new StringBuilder();
        // 循环遍历获取每个商品明细的名称
        for (OrderDetail orderDetail : orderDetailList) {
            // 获取商品名称 进行拼接
            tradeBody.append(orderDetail.getSkuName());
        }
        // 注意拼接名称的长度不能超过数据库中字段的长度
        if (tradeBody.toString().length() > 200) {
            String tradeBodyAfterSub = tradeBody.toString().substring(0, 100);
            orderInfo.setTradeBody(tradeBodyAfterSub);
        } else {
            orderInfo.setTradeBody(tradeBody.toString());
        }

        // 设置 create_time 创建时间
        orderInfo.setCreateTime(new Date());

        // 设置 expire_time 失效时间 默认设置1天
        /*
            失效时间 一般考虑库存设置默认时间的长短
         */
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);

        orderInfo.setExpireTime(calendar.getTime());

        // 设置 process_status 进度状态【包含订单状态】
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        // 保存 订单表 到 数据库
        orderInfoMapper.insert(orderInfo);

        // 保存 订单明细表到数据库
        // 先获取订单明细进行赋值
        List<OrderDetail> orderDetailsList = orderInfo.getOrderDetailList();
        // 循环遍历为订单明细设置页面没有传递的 订单编号
        for (OrderDetail orderDetail : orderDetailsList) {
            /*
            需要赋值：OrderId
            但是我们也没有手动设置 orderInfo.Id，在此处  orderDetail.setOrderId(orderInfo.getId()) 会报空指针吗？
            不会报空指针，因为在 orderInfoMapper.insert(orderInfo) 的时候 id 是主键是自增的。当插入订单表成功后，就会自动生成id。
             */

            // 赋值：OrderId
            orderDetail.setOrderId(orderInfo.getId());

            // 保存 订单明细表 到 数据库
            orderDetailMapper.insert(orderDetail);
        }

        // 发送延迟消息 到时未支付取消订单 - 采用基于插件实现延迟消息的方式
        // 发送的内容：取消订单 - 本质是更新订单的状态，是根据订单Id来更新的。故发送的消息为：订单Id
        rabbitService.sendDelayMessage(
                MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,
                MqConst.ROUTING_ORDER_CANCEL,
                orderInfo.getId(),
                MqConst.DELAY_TIME);

        // 返回订单id
        return orderInfo.getId();
    }

    /**
     * 生成流水号【传递到页面和保存进缓存】
     *
     * @param userId 传递用户Id 用于将流水号存储到缓存
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        // 定义一个流水号
        String tradeNo = UUID.randomUUID().toString();

        // 定义缓存中的key
        String tradeNoKey = "user:" + userId + ":tradeCode";

        // 保存流水号到缓存
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);

        // 返回流水号
        return tradeNo;
    }

    /**
     * 比较流水号
     *
     * @param userId      获取缓存中的流水号
     * @param tradeCodeNo 页面传递过来的流水号
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 需要比较 页面传递过来的流水号 和 缓存中的流水号

        // 定义缓存中的key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 获取缓存中的流水号
        String tradeNoInRedis = (String) redisTemplate.opsForValue().get(tradeNoKey);
        // 返回比较结果
        return tradeCodeNo.equals(tradeNoInRedis);
    }

    /**
     * 删除流水号
     *
     * @param userId 获取缓存中的流水号
     */
    @Override
    public void deleteTradeNo(String userId) {
        // 定义缓存中的key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除缓存中的流水号
        redisTemplate.delete(tradeNoKey);
    }

    /**
     * 验证库存
     * 库存系统接口：http://localhost:9001/hasStock/hasStock?skuId=10221&num=2
     *
     * @param skuId  商品skuId
     * @param skuNum 商品数量
     * @return
     */
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 返回值 0：无库存  1：有库存
        String checkStockResult = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);

        return "1".equals(checkStockResult);
    }

    /**
     * 根据订单Id处理过期订单
     *
     * @param orderId
     */
    @Override
    public void execExpiredOrder(Long orderId) {
        // 方式一：
        /*OrderInfo orderInfo = new OrderInfo();

        orderInfo.setId(orderId);
        //orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
        orderInfo.setOrderStatus(ProcessStatus.CLOSED.getOrderStatus().name());
        orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());

        orderInfoMapper.updateById(orderInfo);*/

        // 方式二：根据订单Id、进程状态对象修改订单状态
        updateOrderStatus(orderId, ProcessStatus.CLOSED);

        // 发送消息 - 更新 paymentInfo 状态
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,
                MqConst.QUEUE_PAYMENT_CLOSE,
                orderId);
    }

    /**
     * 根据订单Id、进程状态对象修改订单状态
     *
     * @param orderId       订单号
     * @param processStatus 进程状态对象
     */
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();

        orderInfo.setId(orderId);

        // 订单状态根据进程状态更改
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());

        orderInfo.setProcessStatus(processStatus.name());

        orderInfoMapper.updateById(orderInfo);
    }

    /**
     * 根据订单Id 查询订单信息和订单明细信息
     *
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        // 获取订单信息
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);

        QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        // 获取订单明细集合
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(wrapper);

        // 赋值订单明细集合
        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;
    }

    /**
     * 通过订单Id获取订单明细 - 用于发送消息通知减库存
     *
     * @param orderId
     */
    @Override
    public void sendOrderStatus(Long orderId) {
        // 将订单的状态改为【已通知仓库】
        updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);

        // 需要发送消息的字符串
        String wareJson = initWareOrder(orderId);

        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_WARE_STOCK,
                MqConst.ROUTING_WARE_STOCK,
                wareJson);
    }

    /**
     * 将 map 转换为 json字符串
     *
     * @param orderId
     * @return
     */
    private String initWareOrder(Long orderId) {
        // 通过 orderId 获取 OrderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);

        // 调用方法获取 orderInfo 数据对应的 map
        Map map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);
    }

    /**
     * 将 orderInfo 部分数据转换为 Map
     *
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo) {

        Map<String, Object> map = new HashMap<>();

        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id 减库存拆单时需要使用！

         /*
         减库存接口 details 参数里数据格式为：k-v，多个数据使用list保存。故为：List<Map>。
            details:[{skuId:101,skuNum:1,skuName:’小米手64G’},
                     {skuId:201,skuNum:1,skuName:’索尼耳机’}]

         */

        // 获取 订单明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        // 声明一个集合存储 订单明细Map
        List<Map> orderDetailMapList = new ArrayList<>();

        for (OrderDetail orderDetail : orderDetailList) {
            // 创建 Map 封装 details 参数需要的数据
            Map<String, Object> orderDetailMap = new HashMap<>();

            orderDetailMap.put("skuId", orderDetail.getSkuId());
            orderDetailMap.put("skuNum", orderDetail.getSkuNum());
            orderDetailMap.put("skuName", orderDetail.getSkuName());

            // 将 orderDetailMap 保存进集合
            orderDetailMapList.add(orderDetailMap);
        }
        // 将数据存入map
        map.put("details", orderDetailMapList);

        // 返回 map
        return map;
    }

    /**
     * 拆单
     *
     * @param orderId    订单Id
     * @param wareSkuMap 仓库与sku的对应关系
     * @return
     */
    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        /*
            1 获取原始订单对象
            2 获取仓库与sku对象关系 转换为List<Map>
                 [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
            3 创建子订单对象
            4 为子订单对象赋值
                需要赋值 orderInfo 和 orderDetail 信息
            5 保存子订单数据
            6 把子订单数据放入集合
            7 更改原始订单的状态位 split
         */

        // 创建集合保存子订单数据
        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        // 获取原始订单对象
        OrderInfo orderInfoOriginal = getOrderInfo(Long.parseLong(orderId));

        // 获取仓库与sku对象关系 转换为List<Map>
        List<Map> wareSkuMapList = JSON.parseArray(wareSkuMap, Map.class);

        // 判读
        if (!CollectionUtils.isEmpty(wareSkuMapList)) {

            for (Map map : wareSkuMapList) {

                // 获取仓库Id
                String wareId = (String) map.get("wareId");

                // 获取所有子订单对象的skuId
                List<String> skuIds = (List<String>) map.get("skuIds");

                // 创建子订单对象
                OrderInfo subOrderInfo = new OrderInfo();

                // 赋值 orderInfo 基本信息 && 赋值 orderDetail 用于计算总金额
                BeanUtils.copyProperties(orderInfoOriginal, subOrderInfo);
                subOrderInfo.setId(null);
                subOrderInfo.setParentOrderId(orderInfoOriginal.getId());
                subOrderInfo.setWareId(wareId);

                // 获取原始订单的订单明细集合
                List<OrderDetail> orderDetailOriginalList = orderInfoOriginal.getOrderDetailList();

                // 定义集合保存订单明细
                List<OrderDetail> orderDetailList = new ArrayList<>();

                if (orderDetailOriginalList != null && orderDetailOriginalList.size() > 0) {
                    // 遍历订单明细
                    for (OrderDetail orderDetail : orderDetailOriginalList) {
                        // 遍历所有子订单对象的skuId
                        for (String skuId : skuIds) {
                            // 比较仓库中的skuId和订单明细中的skuId
                            if (orderDetail.getSkuId().equals(Long.parseLong(skuId))) {
                                // 保存订单明细到集合
                                orderDetailList.add(orderDetail);
                            }
                        }
                    }
                }
                // 赋值 orderDetail 用于计算总金额
                subOrderInfo.setOrderDetailList(orderDetailList);

                // 计算总金额
                subOrderInfo.sumTotalAmount();

                // 保存子订单数据到数据库
                saveOrderInfo(subOrderInfo);

                // 保存子订单数据
                subOrderInfoList.add(subOrderInfo);
            }
        }
        // 更改原始订单状态为 split
        updateOrderStatus(Long.parseLong(orderId), ProcessStatus.SPLIT);

        return subOrderInfoList;
    }
}
