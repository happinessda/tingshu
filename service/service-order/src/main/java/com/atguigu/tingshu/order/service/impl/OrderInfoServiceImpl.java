package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private VipServiceConfigFeignClient vipServiceConfigFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;

    @Autowired
    private UserAccountFeignClient userAccountFeignClient;

    @Autowired
    private OrderDerateMapper orderDerateMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void orderPaySuccess(String orderNo) {
        //  修改订单状态，订单状态：1402-已支付 保存购买记录;
        OrderInfo orderInfo = this.getOrderInfoByOrderNo(orderNo);
        //        if (null == orderInfo){
        //            return;
        //        }
        if (null != orderInfo && orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)){
            //  设置订单支付状态为已付款
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
            //  执行更新操作
            this.updateById(orderInfo);

            //  记录用户购买记录
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            //  3.1 给属性赋值
            userPaidRecordVo.setUserId(orderInfo.getUserId());
            userPaidRecordVo.setOrderNo(orderNo);
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            //  获取到订单明细的 itemId; order_detail.item_id = 购买条目的Id;vip_service_config.id ;如果购买声音，它属于声音id
            List<Long> itemIdList = orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(itemIdList);
            //  远程调用;
            userInfoFeignClient.savePaidRecord(userPaidRecordVo);
        }

    }

    @Override
    public IPage<OrderInfo> findUserPage(Page<OrderInfo> orderInfoPage, Long userId, String orderStatus) {
        //  我的订单中，包含订单主表，订单明细表;
        IPage<OrderInfo> infoIPage = orderInfoMapper.selectUserPage(orderInfoPage, userId, orderStatus);
        infoIPage.getRecords().stream().forEach(orderInfo -> {
            orderInfo.setPayWayName(SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfo.getPayWay()) ? "余额支付" : "在线支付");
            orderInfo.setOrderStatusName(SystemConstant.ORDER_STATUS_PAID.equals(orderInfo.getOrderStatus()) ? "已支付" : SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus()) ? "未支付" : "已取消");
        });
        //  返回数据
        return infoIPage;
    }

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        //  查询数据
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        //  赋值支付方式
        orderInfo.setPayWayName(SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfo.getPayWay()) ? "余额支付" : "在线支付");
        //  获取订单明细
        List<OrderDetail> orderDetailList = this.orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderInfo.getId()));
        if (null != orderInfo){
            orderInfo.setOrderDetailList(orderDetailList);
        }
        //  返回数据
        return orderInfo;
    }

    @Override
    public void cancelOrder(String orderNo) {
        //  修改订单状态：
        /*
            case 1: orderInfo;
            case 2：orderInfo,paymentInfo 说明用户一定点击了微信支付：
            case 3: orderinfo,paymentInfo,wxPay
         */
        OrderInfo orderInfo = this.getOrderInfoByOrderNo(orderNo);
        if (null !=orderInfo && orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)){
            //  根据订单编号查询是否有paymentInfo 交易记录.
            /*
             PaymentInfo paymentInfo = paymentInfoFeignClient.getPaymentInfo(orderNo);
             if(null != paymentInfo && status()==ORDER_STATUS_UNPAID){ case 2:
                 查询是否有微信交易记录：queryPayStatus()
                 if(true){ 有交易记录
                    boolean result = closeWxPay();
                    if(true){
                        // 关闭成功; 不能支付了！
                         orderInfo + paymentInfo
                    }else{
                        //  关闭失败：在过期的一瞬间用户支付了！
                    }
                 }else{
                    orderInfo + paymentInfo
                 }
              } else {
                orderInfo
              }
             */
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
            //  this.update(orderInfo, new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
            this.updateById(orderInfo);

        }

    }

    @Override
    @GlobalTransactional
    public String submitOrder(OrderInfoVo orderInfoVo, Long userId) {
        //  1.  校验签名 : 签名格式：
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        map.put("payWay", "");
        SignHelper.checkSign(map);
        //  2.  校验tradeNo
        //  2.1 先获取到页面流水号;
        String tradeNo = orderInfoVo.getTradeNo();
        //  2.2 获取到redis 流水号;
        String tradeNoKey = "tradeNo:" + userId + ":" + tradeNo;
        //        String tradeNoStr = (String) this.redisTemplate.opsForValue().get(tradeNoKey);
        //        //  2.3 判断是否一致.
        //        if (!tradeNo.equals(tradeNoStr)){
        //            return "交易流水号不一致,重复提交订单.";
        //        }
        //        //  2.4 删除redis 流水号
        //        this.redisTemplate.delete(tradeNoKey);
        //  创建RedisScript 对象
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        //  定义lua 脚本;
        String scriptText = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        redisScript.setScriptText(scriptText);
        redisScript.setResultType(Long.class);
        //  判断是否删除成功.
        Long count = (Long) this.redisTemplate.execute(redisScript, Arrays.asList(tradeNoKey), tradeNo);
        if (count == 0) {
            throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
        }
        //  声明一个订单编号;
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        //  3.  判断支付方式：
        if (!SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay())) {
            //  微信支付; 保存订单信息，并发送一个消息，来实现取消订单功能
            this.saveOrderInfo(orderInfoVo, orderNo, userId);
            //  在线支付； 延迟消息实现方式： 1. 基于死信队列； 2. 基于延迟插件(具有顺序性) 有啥区别?
            rabbitService.sendDealyMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, orderNo, MqConst.CANCEL_ORDER_DELAY_TIME);
        } else {
            //  余额支付;
            //  1. 保存订单信息; order order_info order_detail order_derate
            this.saveOrderInfo(orderInfoVo, orderNo, userId);

            //  2. 判断是否有可用余额; account 创建扣减金额对象
            AccountDeductVo accountDeductVo = new AccountDeductVo();
            accountDeductVo.setOrderNo(orderNo);
            accountDeductVo.setUserId(userId);
            accountDeductVo.setAmount(orderInfoVo.getOrderAmount());
            accountDeductVo.setContent(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
            //  远程调用
            Result result = userAccountFeignClient.checkAndDeduct(accountDeductVo);
            if (200 != result.getCode()) {
                //  检查扣减失败，则抛出异常！
                throw new GuiguException(result.getCode(), result.getMessage());
            }
            //  修改订单状态！
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderInfo::getOrderNo, orderNo);
            this.orderInfoMapper.update(orderInfo, wrapper);

            //  3. 保存用户购买交易记录; user; user_paid_album user_paid_track user_vip_service
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            //  3.1 给属性赋值
            userPaidRecordVo.setUserId(userId);
            userPaidRecordVo.setOrderNo(orderNo);
            userPaidRecordVo.setItemType(orderInfoVo.getItemType());
            //  获取到订单明细的 itemId; order_detail.item_id = 购买条目的Id;vip_service_config.id ;如果购买声音，它属于声音id
            List<Long> itemIdList = orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(itemIdList);
            //  远程调用;
            Result userResult = userInfoFeignClient.savePaidRecord(userPaidRecordVo);
            //  判断
            if (200 != userResult.getCode()) {
                throw new GuiguException(211, "新增购买记录异常");
            }
        }
        //  返回订单编号
        return orderNo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOrderInfo(OrderInfoVo orderInfoVo, String orderNo, Long userId) {
        //  order_info
        //  属性拷贝：
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoVo, orderInfo);
        orderInfo.setUserId(userId);
        orderInfo.setOrderNo(orderNo);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        //  保存对象
        orderInfoMapper.insert(orderInfo);

        //  order_detail || order_detail.item_id = 购买条目的Id;vip_service_config.id ;如果购买声音，它属于声音id
        //  如果购买专辑 则它属于专辑id
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (!CollectionUtils.isEmpty(orderDetailVoList)) {
            for (OrderDetailVo orderDetailVo : orderDetailVoList) {
                //  创建订单对象
                OrderDetail orderDetail = new OrderDetail();
                //  属性拷贝：
                BeanUtils.copyProperties(orderDetailVo, orderDetail);
                //  赋值订单Id;
                orderDetail.setOrderId(orderInfo.getId());
                this.orderDetailMapper.insert(orderDetail);
            }
        }

        //  order_derate
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (!CollectionUtils.isEmpty(orderDerateVoList)) {
            for (OrderDerateVo orderDerateVo : orderDerateVoList) {
                //  创建订单对象
                OrderDerate orderDerate = new OrderDerate();
                //  属性拷贝：
                BeanUtils.copyProperties(orderDerateVo, orderDerate);
                //  赋值订单Id;
                orderDerate.setOrderId(orderInfo.getId());
                this.orderDerateMapper.insert(orderDerate);
            }
        }
    }

    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        //  创建对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //  原始金额
        BigDecimal originalAmount = new BigDecimal("0.00");
        //  减免金额
        BigDecimal derateAmount = new BigDecimal("0.00");
        //  实际金额
        BigDecimal orderAmount = new BigDecimal("0.00");
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //  通过itemType 进行判断
        if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_ALBUM)) {
            //  购买专辑
            //  获取到专辑Id
            Long albumId = tradeVo.getItemId();
            //  需要远程调用查看是否购买过专辑！
            Result<Boolean> isPaidAlbumResult = this.userInfoFeignClient.isPaidAlbum(albumId);
            //  判断
            Assert.notNull(isPaidAlbumResult, "查询是否购买专辑结果为空");
            if (isPaidAlbumResult.getData()) {
                //  购买过专辑 直接返回
                throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
            }
            //  根据专辑Id 获取到专辑对象；
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(albumInfoResult, "专辑信息结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            //  获取当前专辑的金额：
            originalAmount = albumInfo.getPrice();
            //  判断当前用户是否属于vip; 当前专辑是否有折扣！
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
            Assert.notNull(userInfoVoResult, "用户信息结果集为空");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().after(new Date())) {
                //  属于vip , 判断是否有折扣
                if (albumInfo.getVipDiscount().compareTo(new BigDecimal("-1")) == 1) {
                    //  说明有折扣：1000 2折； 折扣:800; 实际：200; 0.1--9.9;  1000*(10-2)/10=800;
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getVipDiscount())).divide(new BigDecimal("10"), 2, BigDecimal.ROUND_HALF_UP);
                }
            } else {
                //  不属于vip
                //  属于vip , 判断是否有折扣
                if (albumInfo.getDiscount().compareTo(new BigDecimal("-1")) == 1) {
                    //  说明有折扣：1000 2折； 折扣:800; 实际：200; 0.1--9.9;  1000*(10-2)/10=800;
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getDiscount())).divide(new BigDecimal("10"), 2, BigDecimal.ROUND_HALF_UP);
                }
            }
            //  实际金额：
            orderAmount = originalAmount.subtract(derateAmount);
            //  订单明细：
            //  创建OrderDetailVo 对象
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(albumId);
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVoList.add(orderDetailVo);
            //  减免明细：是否有减免
            if (derateAmount.compareTo(new BigDecimal("0")) == 1) {
                //  添加减免明细集合
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setRemarks("专辑折扣");
                orderDerateVoList.add(orderDerateVo);
            }
        } else if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_TRACK)) {
            //  购买声音
            //  {"itemType":"1002","itemId":48241,"trackCount":0}
            if (tradeVo.getTrackCount() < 0) {
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
            //  有声音明细列表; trackId; 购买数量: 5;
            Result<List<TrackInfo>> trackInfoListResult = trackInfoFeignClient.findPaidTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount());
            Assert.notNull(trackInfoListResult, "查询购买声音结果为空");
            List<TrackInfo> trackInfoList = trackInfoListResult.getData();
            Assert.notNull(trackInfoList, "购买声音明细列表为空");
            //  获取专辑Id ，根据专辑Id获取到专辑对象
            //  获取到专辑对象 因为声音列表对应的专辑Id 都是同一个！
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(trackInfoList.get(0).getAlbumId());
            Assert.notNull(albumInfoResult, "专辑信息结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            //  组成订单明细列表：
            orderDetailVoList = trackInfoList.stream().map(trackInfo -> {
                //  创建订单明细对象
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                //  错误的： orderDetailVo.setItemId(tradeVo.getItemId());
                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemName(trackInfo.getTrackTitle());
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                orderDetailVo.setItemPrice(albumInfo.getPrice());
                return orderDetailVo;
            }).collect(Collectors.toList());

            //  获取购买声音的数量
            originalAmount = tradeVo.getTrackCount() == 0 ? albumInfo.getPrice() : albumInfo.getPrice().multiply(new BigDecimal(tradeVo.getTrackCount().toString()));
            //  声音不支持折扣; 计算实际金额
            orderAmount = originalAmount;


        } else {
            //  购买vip
            Long vipServiceConfigId = tradeVo.getItemId();
            //  远程调用获取购买vip 的服务配置信息.
            Result<VipServiceConfig> vipServiceConfigResult = vipServiceConfigFeignClient.getVipServiceConfig(vipServiceConfigId);
            Assert.notNull(vipServiceConfigResult, "查询vip服务配置结果为空");
            VipServiceConfig vipServiceConfig = vipServiceConfigResult.getData();
            //  原始金额
            originalAmount = vipServiceConfig.getPrice();
            //  减免金额
            derateAmount = originalAmount.subtract(vipServiceConfig.getDiscountPrice());
            //  实际金额
            orderAmount = vipServiceConfig.getDiscountPrice();

            //  创建减免明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(vipServiceConfigId);
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVoList.add(orderDetailVo);

            //  减免明细：是否有减免
            if (derateAmount.compareTo(new BigDecimal("0")) == 1) {
                //  添加减免明细集合
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setRemarks("VIP折扣");
                orderDerateVoList.add(orderDerateVo);
            }
        }
        //  交易号
        String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
        //  定义一个key
        String tradeNoKey = "tradeNo:" + userId + ":" + tradeNo;
        //  将这个交易号存储到缓存！ setnx() setex();
        this.redisTemplate.opsForValue().setIfAbsent(tradeNoKey, tradeNo, 10, TimeUnit.MINUTES);
        //  给属性赋值：
        orderInfoVo.setTradeNo(tradeNo);
        //  交易方式
        orderInfoVo.setPayWay("");
        orderInfoVo.setItemType(tradeVo.getItemType());
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        orderInfoVo.setTimestamp(SignHelper.getTimestamp());
        //  签名： 1. 防止用户恶意篡改数据  2. 保证接口的安全性
        //  将这个orderInfoVo 对象转换为Map 集合
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        //  将返回的数据使用 | 拼接！ 并且添加一个固定的key; 在使用md5 对当前字符串加密;
        String sign = SignHelper.getSign(map);
        orderInfoVo.setSign(sign);
        //  返回数据
        return orderInfoVo;
    }
}
