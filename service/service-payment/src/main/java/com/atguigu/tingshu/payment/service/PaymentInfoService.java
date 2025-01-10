package com.atguigu.tingshu.payment.service;

import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wechat.pay.java.service.payments.model.Transaction;

import java.math.BigDecimal;

public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 保存交易记录
     * @param userId
     * @param paymentType
     * @param orderNo
     * @param payAmount
     */
    void savePaymentInfo(Long userId, String paymentType, String orderNo, BigDecimal payAmount);

    /**
     * 更新订单
     * @param transaction
     */
    void updatePaymentStatus(Transaction transaction);

}
