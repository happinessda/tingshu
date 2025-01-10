package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单结算页
     * @param tradeVo
     * @param userId
     * @return
     */
    OrderInfoVo trade(TradeVo tradeVo, Long userId);

    /**
     * 立即结算
     * @param orderInfoVo
     * @param userId
     * @return
     */
    String submitOrder(OrderInfoVo orderInfoVo, Long userId);

    /**
     * 取消订单
     * @param orderNo
     */
    void cancelOrder(String orderNo);

    /**
     * 根据订单编号查询订单信息
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfoByOrderNo(String orderNo);

    /**
     * 查看我的订单
     * @param orderInfoPage
     * @param userId
     * @param orderStatus
     * @return
     */
    IPage<OrderInfo> findUserPage(Page<OrderInfo> orderInfoPage, Long userId, String orderStatus);

    /**
     * 订单支付成功
     * @param orderNo
     */
    void orderPaySuccess(String orderNo);
}
