package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author fzx
 * @ClassName AlbumStrategy
 * @description: TODO
 * @date 2024年12月09日
 * @version: 1.0
 * 1001 --> AlbumStrategy  key-value;
 */
@Service(value = "1001")
public class AlbumStrategy implements ItemTypeStrategy {


    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //  先查询是否有购买记录;
        //  Boolean result = userInfoService.isPaidAlbum(userPaidRecordVo.getItemIdList().get(0), userPaidRecordVo.getUserId());
        UserPaidAlbum paidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getAlbumId, userPaidRecordVo.getItemIdList().get(0)).eq(UserPaidAlbum::getUserId, userPaidRecordVo.getUserId()));
        if (null != paidAlbum) {
            return;
        }
        //  添加购买专辑信息;
        UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        userPaidAlbumMapper.insert(userPaidAlbum);
    }
}
