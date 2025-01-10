package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {


    @Autowired
    private RabbitService rabbitService;

    @Override
    public void updatePaymentStatus(Transaction transaction) {
        //  更新用户交易记录！payment_info;
        PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, transaction.getOutTradeNo()));
        if (null == paymentInfo){
            return;
        }
        //  否则不为空，更新支付交易状态.
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
        //  微信支付内部交易号;
        paymentInfo.setOutTradeNo(transaction.getTransactionId());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(transaction.toString());
        this.updateById(paymentInfo);
        //  更新订单表支付状态;
        //  判断区分是订单，还是充值业务；根据不同的业务选择不同的路由键！
        String routingKey = paymentInfo.getPaymentType().equals(SystemConstant.PAYMENT_TYPE_ORDER)?MqConst.ROUTING_ORDER_PAY_SUCCESS:MqConst.ROUTING_RECHARGE_PAY_SUCCESS;
        //  发送消息形式更新状态; paymentInfo.getOrderNo() = transaction.getOutTradeNo();
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER,routingKey,paymentInfo.getOrderNo());

    }

    @Override
    public void savePaymentInfo(Long userId, String paymentType, String orderNo, BigDecimal payAmount) {
        //  你可以在保存之前，先查询是否有交易记录，如果有，则跳过;
        PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        //  判断对象是否为空
        if (null != paymentInfo){
            return;
        }
        //  创建对象
        paymentInfo = new PaymentInfo();
        //  赋值：
        paymentInfo.setUserId(userId);
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setAmount(payAmount);
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        paymentInfo.setContent(paymentType.equals(SystemConstant.PAYMENT_TYPE_ORDER)?"订单支付":"充值支付");
        //  保存数据
        this.save(paymentInfo);
    }
}
