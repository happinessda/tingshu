package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 微信登录
     * @param code
     */
    Map<String,Object> wxLogin(String code);

    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 修改用户信息
     * @param userInfoVo
     * @param userId
     */
    void updateUser(UserInfoVo userInfoVo, Long userId);

    /**
     * 判断用户是否购买声音列表
     * @param albumId
     * @param trackIdList
     * @param userId
     * @return
     */
    Map<Long, Integer> userIsPaidTrack(Long albumId, List<Long> trackIdList, Long userId);

    /**
     * 判断用户是否购买专辑
     * @param albumId
     * @param userId
     * @return
     */
    Boolean isPaidAlbum(Long albumId, Long userId);

    /**
     * 获取用户已购买的声音Id集合
     * @param albumId
     * @param userId
     * @return
     */
    List<Long> findUserPaidTrackList(Long albumId, Long userId);

    /**
     * 处理用户购买
     * @param userPaidRecordVo
     */
    void savePaidRecord(UserPaidRecordVo userPaidRecordVo);

    /**
     * 更新VIP到期失效状态
     */
    void updateVipExpireStatus();

}
