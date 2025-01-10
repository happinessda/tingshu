package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;

    @Override
    public IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> detailPage, Long userId) {
        //  1204
        return userAccountDetailMapper.selectPage(detailPage, new LambdaQueryWrapper<UserAccountDetail>().eq(UserAccountDetail::getUserId, userId).eq(UserAccountDetail::getTradeType, SystemConstant.ACCOUNT_TRADE_TYPE_MINUS));
    }

    @Override
    public IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> detailPage, Long userId) {
        //  1201
        return userAccountDetailMapper.selectPage(detailPage, new LambdaQueryWrapper<UserAccountDetail>().eq(UserAccountDetail::getUserId, userId).eq(UserAccountDetail::getTradeType, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT));

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rechargePaySuccess(String orderNo) {
        //  充值成功要做什么?   修改状态, 增加余额; 记录充值记录
        RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
        if (null != rechargeInfo && rechargeInfo.getRechargeStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)){
            //  更新为已支付
            rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
            this.rechargeInfoMapper.updateById(rechargeInfo);
            //  增加交易金额；
            userAccountMapper.updateUserAccount(rechargeInfo.getUserId(),rechargeInfo.getRechargeAmount());

            //  记录充值记录：
            this.addUserAccountDetail(rechargeInfo.getUserId(),"充值", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT,rechargeInfo.getRechargeAmount(),orderNo);
        }
    }

    @Override
    @Transactional
    public boolean checkAndDeduct(AccountDeductVo accountDeductVo) {
        //	执行sql语句;
        int result = userAccountMapper.updateCheckAndDeduct(accountDeductVo.getUserId(), accountDeductVo.getAmount());
        if (result > 0){
            //  记录当前账户资金流向;
            this.addUserAccountDetail(accountDeductVo.getUserId(),accountDeductVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,accountDeductVo.getAmount(),accountDeductVo.getOrderNo());
            //  返回true;
            return true;
        }
        return false;
    }

    private void addUserAccountDetail(Long userId, String content, String typeMinus, BigDecimal amount, String orderNo) {
        //  创建对象
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(content);
        userAccountDetail.setTradeType(typeMinus);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }

    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        //	sql: select available_amount from user_account where user_id = ? and is_deleted = 0;
        // SELECT id,user_id,total_amount,lock_amount,available_amount,total_income_amount,total_pay_amount,create_time,update_time,is_deleted FROM user_account WHERE is_deleted=0 AND (user_id = ?)
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId).select(UserAccount::getAvailableAmount);
        UserAccount userAccount = userAccountMapper.selectOne(wrapper);
        //	返回数据
        return userAccount.getAvailableAmount();
    }

    @Override
    public void initUserAccount(Long userId) {
        //	user_account
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setTotalAmount(new BigDecimal("1000"));
        userAccount.setAvailableAmount(new BigDecimal("1000"));
        userAccountMapper.insert(userAccount);
    }
}
