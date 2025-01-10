package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 初始化用户账户
     * @param userId
     */
    void initUserAccount(Long userId);

    /**
     * 获取用户账户可用余额
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 检查并扣减账户余额
     * @param accountDeductVo
     * @return
     */
    boolean checkAndDeduct(AccountDeductVo accountDeductVo);

    /**
     * 充值成功
     * @param orderNo
     */
    void rechargePaySuccess(String orderNo);

    /**
     * 查看消费记录
     * @param detailPage
     * @param userId
     * @return
     */
    IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> detailPage, Long userId);

    /**
     * 查看充值记录
     * @param detailPage
     * @param userId
     * @return
     */
    IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> detailPage, Long userId);

}
