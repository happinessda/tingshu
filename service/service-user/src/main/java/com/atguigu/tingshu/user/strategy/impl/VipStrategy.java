package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author fzx
 * @ClassName VipStrategy
 * @description: TODO
 * @date 2024年12月09日
 * @version: 1.0
 * 1003 --> VipStrategy  key-value;
 */
@Service(value = "1003")
public class VipStrategy implements ItemTypeStrategy {

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //  添加购买vip,续期 user_vip_service
        //  先获取到购买vip哪种配置 1,3,12 月；
        Long vipServiceConfigId = userPaidRecordVo.getItemIdList().get(0);
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipServiceConfigId);
        //  获取购买的月数
        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        //  分情况：是否属于续期情况（当前这个用户属于vip也没有过期）
        UserInfo userInfo = this.userInfoMapper.selectById(userPaidRecordVo.getUserId());
        //  创建购买vip记录的对象
        UserVipService userVipService = new UserVipService();
        userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        userVipService.setUserId(userPaidRecordVo.getUserId());
        //  获取当前系统时间;
        Date currentTime = new Date();
        //  应该是没有续期的情况下;
        userVipService.setStartTime(currentTime);
        //  续期： start_time 开始时间; expire_time 过期时间 在原有的过期时间基础上，再加上购买的时长.
        if (1 == userInfo.getIsVip() && userInfo.getVipExpireTime().after(new Date())){
            //  续期 - 在原有的过期时间基础上，再加上购买的时长.
            currentTime = userInfo.getVipExpireTime();
        }
        //  当前系统时间+购买时间;
        Date expireTime = new LocalDateTime(currentTime).plusMonths(serviceMonth).toDate();
        userVipService.setExpireTime(expireTime);
        //  保存用户购买记录;
        userVipServiceMapper.insert(userVipService);

        //  判断是否属于vip 只关心 is_vip and exprie_time;
        userInfo.setIsVip(1);
        userInfo.setVipExpireTime(expireTime);
        //  更新数据
        this.userInfoMapper.updateById(userInfo);
    }
}
