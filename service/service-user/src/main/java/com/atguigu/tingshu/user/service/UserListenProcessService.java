package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 获取声音播放进度
     * @param trackId
     * @param userId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long trackId, Long userId);

    /**
     * 保存用户声音播放进度
     * @param userListenProcessVo
     * @param userId
     */
    void updateListenProcess(UserListenProcessVo userListenProcessVo, Long userId);

    /**
     * 获取最近播放的一次声音
     * @param userId
     * @return
     */
    Map<String, Object> getLatelyTrack(Long userId);
}
