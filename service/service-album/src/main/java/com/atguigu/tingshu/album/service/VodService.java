package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;

public interface VodService {

    /**
     * 根据流媒体Id获取对象
     * @param mediaFileId
     * @return
     */
    TrackMediaInfoVo getMediaInfo(String mediaFileId);

    /**
     * 删除流媒体数据
     * @param mediaFileId
     */
    void removeMedia(String mediaFileId);
}
