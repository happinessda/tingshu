package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 扣减账户余额
     * @param userId
     * @param amount
     */
    @Update("update user_account\n" +
            "set available_amount = available_amount - #{amount},\n" +
            "    total_amount     = total_amount - #{amount},\n" +
            "    total_pay_amount = total_pay_amount + #{amount}\n" +
            "where user_id = #{userId}\n" +
            "  and available_amount >= #{amount}\n" +
            "  and is_deleted = 0")
    int updateCheckAndDeduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 更新账户余额
     * @param userId
     * @param amount
     */
    @Update("update user_account\n" +
            "set total_amount        = total_amount + #{amount},\n" +
            "    available_amount    = available_amount + #{amount},\n" +
            "    total_income_amount = total_income_amount + #{amount}\n" +
            "where user_id = #{userId}\n" +
            "  and is_deleted = 0")
    void updateUserAccount(@Param("userId")Long userId, @Param("amount") BigDecimal amount);
}
