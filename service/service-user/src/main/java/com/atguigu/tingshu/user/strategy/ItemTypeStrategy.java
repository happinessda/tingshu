package com.atguigu.tingshu.user.strategy;

import com.atguigu.tingshu.vo.user.UserPaidRecordVo;

/**
 * @author fzx
 * @ClassName ItemTypeStrategy
 * @description: TODO
 * @date 2024年12月09日
 * @version: 1.0
 */
public interface ItemTypeStrategy {

    /**
     * 处理用户购买记录
     *
     * @param userPaidRecordVo
     */
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo);
}
