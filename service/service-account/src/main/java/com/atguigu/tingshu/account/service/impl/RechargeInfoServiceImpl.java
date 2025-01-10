package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;

    @Override
    public String submitRecharge(RechargeInfoVo rechargeInfoVo, Long userId) {
        //	创建对象
        RechargeInfo rechargeInfo = new RechargeInfo();
        rechargeInfo.setUserId(userId);
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        rechargeInfo.setOrderNo(orderNo);
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());

        //	保存充值信息。
        this.save(rechargeInfo);
        //	返回订单编号
        return orderNo;
    }
}
