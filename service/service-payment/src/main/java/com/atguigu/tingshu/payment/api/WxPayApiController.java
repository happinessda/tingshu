package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment/wxPay")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    /**
     * 异步回调
     * @param request {code: message: data:{ code: "",message:""}}
     * @return
     */
    @Operation(summary = "异步回调")
    @PostMapping("/notify")
    public Map<String,Object> wxNotify(HttpServletRequest request) {
        System.out.println("异步回调通知...");
        //  创建Map 集合
        Map<String, Object> result = new HashMap<>();
        //  调用服务层方法
        Transaction transaction = wxPayService.wxNotify(request);
        if (null != transaction && transaction.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)){
            //  调用修改交易记录状态，修改订单状态，记录购买记录.
            paymentInfoService.updatePaymentStatus(transaction);
            //返回成功
            result.put("code", "SUCCESS");
            result.put("message", "成功");
            //  返回数据
        } else {
            //返回失败
            result.put("code", "FAIL");
            result.put("message", "失败");
        }
        //  返回结果
        return result;
    }

    /**
     * 查询支付状态 https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_5_2.shtml
     * @param orderNo
     * @return
     */
    @Operation(summary = "支付状态查询")
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result queryPayStatus(@PathVariable String orderNo) {
        //  调用服务层方法
        Transaction transaction = wxPayService.queryPayStatus(orderNo);
        //  判断
        if (null != transaction && transaction.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)){
            //  说明这个用户支付成功了,更新交易记录状态.
            paymentInfoService.updatePaymentStatus(transaction);
            //  返回true;
            return Result.ok(true);
        }
        //  返回数据
        return Result.ok(false);
    }
    /**
     * 微信支付
     *
     * @param orderNo
     * @return
     */
    @TsLogin
    @Operation(summary = "微信支付")
    @PostMapping("/createJsapi/{paymentType}/{orderNo}")
    public Result createJsapi(@PathVariable String paymentType,
                              @PathVariable String orderNo) {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  调用服务层方法
        Map<String,Object> map = wxPayService.createJsapi(paymentType,orderNo,userId);
        //  返回数据
        return Result.ok(map);
    }


}
