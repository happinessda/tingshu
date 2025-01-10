package com.atguigu.tingshu.payment.service;

import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {

    /**
     * 微信支付
     * @param paymentType
     * @param orderNo
     * @param userId
     * @return
     */
    Map<String, Object> createJsapi(String paymentType, String orderNo, Long userId);

    /**
     * 查询支付状态
     * @param orderNo
     * @return
     */
    Transaction queryPayStatus(String orderNo);

    /**
     * 异步回调
     * @param request
     * @return
     */
    Transaction wxNotify(HttpServletRequest request);

}
