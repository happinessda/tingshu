package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author fzx
 * @ClassName TrackStrategy
 * @description: TODO
 * @date 2024年12月09日
 * @version: 1.0
 * 1002 --> TrackStrategy  key-value;
 */
@Service(value = "1002")
public class TrackStrategy implements ItemTypeStrategy {

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;

    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //  查询数据
        Long count = this.userPaidTrackMapper.selectCount(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo()).eq(UserPaidTrack::getUserId, userPaidRecordVo.getUserId()));
        if (count > 0)
        {
            return;
        }
        //  获取到当前订单明细;
        List<Long> trackIdList = userPaidRecordVo.getItemIdList();
        //  可以通过声音Id 查询声音对象；
        Result<TrackInfo> trackInfoResult = trackInfoFeignClient.getTrackInfoById(trackIdList.get(0));
        Assert.notNull(trackInfoResult, "查询购买声音结果为空");
        TrackInfo trackInfo = trackInfoResult.getData();
        Assert.notNull(trackInfo, "查询购买声音对象为空");
        for (Long trackId : trackIdList) {
            //  插入数据；
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setAlbumId(trackInfo.getAlbumId());
            userPaidTrack.setTrackId(trackId);
            userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
            userPaidTrack.setUserId(userPaidRecordVo.getUserId());
            userPaidTrackMapper.insert(userPaidTrack);
        }
    }
}
