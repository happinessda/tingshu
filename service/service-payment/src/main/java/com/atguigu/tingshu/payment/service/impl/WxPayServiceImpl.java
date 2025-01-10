package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.account.client.RechargeInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private WxPayV3Config wxPayV3Config;

    @Autowired
    private RSAAutoCertificateConfig config;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private RechargeInfoFeignClient rechargeInfoFeignClient;


    @Override
    public Transaction wxNotify(HttpServletRequest request) {
        //  从请求头中获取数据
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String wechatpayNonce = request.getHeader("Wechatpay-Nonce");
        String wechatSignature = request.getHeader("Wechatpay-Signature");
        String wechatTimestamp = request.getHeader("Wechatpay-Timestamp");
        String requestBody = PayUtil.readData(request);

        // 构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(wechatpayNonce)
                .signature(wechatSignature)
                .timestamp(wechatTimestamp)
                .body(requestBody)
                .build();
        // 初始化 NotificationParser
        NotificationParser parser = new NotificationParser(config);
        try {
            // 以支付通知回调为例，验签、解密并转换成 Transaction
            Transaction transaction = parser.parse(requestParam, Transaction.class);

            //  返回数据
            return transaction;
        } catch (ValidationException e) {
            // 签名验证失败，返回 401 UNAUTHORIZED 状态码
            log.error("sign verification failed", e);
        }
        //  默认返回数据
        return null;
    }

    @Override
    public Transaction queryPayStatus(String orderNo) {
//        QueryOrderByIdRequest queryRequest = new QueryOrderByIdRequest();
        QueryOrderByOutTradeNoRequest outTradeNoRequest = new QueryOrderByOutTradeNoRequest();
        outTradeNoRequest.setMchid(wxPayV3Config.getMerchantId());
        outTradeNoRequest.setOutTradeNo(orderNo);
        try {
            //  构建service 对象
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();
            Transaction transaction = service.queryOrderByOutTradeNo(outTradeNoRequest);
            System.out.println(transaction.getTradeState());
            //  返回这个对象transaction
            return transaction;
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
            System.out.printf("reponse body=[%s]\n", e.getResponseBody());
        }
        return null;
    }

    @Override
    public Map<String, Object> createJsapi(String paymentType, String orderNo, Long userId) {
        //	创建map 集合
        Map<String, Object> result = new HashMap();
        //  记录交易记录，方便后续对账！payment_info
        //  声明一个变量存储交易记录金额;
        BigDecimal payAmount = new BigDecimal("0.00");
        //  根据订单编号查询订单对象;
        if (paymentType.equals(SystemConstant.PAYMENT_TYPE_ORDER)){
            //  order_info.order_amount
            Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderNo);
            Assert.notNull(orderInfoResult, "订单信息为空");
            OrderInfo orderInfo = orderInfoResult.getData();
            Assert.notNull(orderInfo, "订单信息为空");
            //  判断当前这个订单状态,如果这个订单已取消了，则不能再次点击支付.
            if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderInfo.getOrderStatus())){
                result.put("code","666");
                result.put("message","订单已取消");
                return result;
            }
            payAmount = orderInfo.getOrderAmount();
        } else {
            //  充值：recharge_info.recharge_amount
            Result<RechargeInfo> rechargeInfoResult = rechargeInfoFeignClient.getRechargeInfo(orderNo);
            Assert.notNull(rechargeInfoResult, "充值信息为空");
            RechargeInfo rechargeInfo = rechargeInfoResult.getData();
            Assert.notNull(rechargeInfo, "充值信息为空");
            payAmount = rechargeInfo.getRechargeAmount();
        }
        //  保存支付交易记录：
        paymentInfoService.savePaymentInfo(userId,paymentType,orderNo,payAmount);
        //  构建service 对象
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();

        // 跟之前下单示例一样，填充预下单参数
        PrepayRequest request = new PrepayRequest();
        // request.setXxx(val)设置所需参数，具体参数可见Request定义
        Amount amount = new Amount();
        amount.setTotal(1); // 1分钱;
        request.setAmount(amount);
        request.setAppid(wxPayV3Config.getAppid());
        request.setMchid(wxPayV3Config.getMerchantId());
        request.setDescription("测试商品标题");
        request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
        request.setOutTradeNo(orderNo);
        //  给设置一个过期时间：等于取消订单时间！
        //  request.setTimeExpire();
        //  创建一个Payer
        Payer payer = new Payer();
        //  远程调用
        Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
        Assert.notNull(userInfoVoResult, "用户信息结果集为空");
        UserInfoVo userInfoVo = userInfoVoResult.getData();
        Assert.notNull(userInfoVo, "用户信息为空");
        payer.setOpenid(userInfoVo.getWxOpenId());
        //  设置支付者;
        request.setPayer(payer);
        // response包含了调起支付所需的所有参数，可直接用于前端调起支付
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
        //	赋值：
        result.put("timeStamp", response.getTimeStamp()); // 时间戳
        result.put("nonceStr", response.getNonceStr());   // 随机字符串
        result.put("package", response.getPackageVal());  // 订单详情扩展字符串
        result.put("signType", response.getSignType());   // 签名方式
        result.put("paySign", response.getPaySign());     // 签名

        //	返回map集合
        return result;
    }
}
