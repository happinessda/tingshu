package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserInfoDegradeFeignClient implements UserInfoFeignClient {

    @Override
    public Result updateVipExpireStatus() {
        return null;
    }

    @Override
    public Result savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        return null;
    }

    @Override
    public Result<List<Long>> findUserPaidTrackList(Long albumId) {
        return null;
    }

    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        return null;
    }

    @Override
    public Result<Map<Long, Integer>> userIsPaidTrack(Long albumId, List<Long> trackIdList) {
        return null;
    }

    @Override
    public Result<UserInfoVo> getUserInfoVo(Long userId) {
        return null;
    }
}
