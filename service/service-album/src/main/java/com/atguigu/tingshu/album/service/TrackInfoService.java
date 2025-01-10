package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 上传声音
     * @param file
     */
    Map<String, Object> uploadTrack(MultipartFile file);

    /**
     * 保存声音
     * @param trackInfoVo
     * @param userId
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);

    /**
     * 分页查询用户声音列表
     * @param page
     * @param trackInfoQuery
     * @return
     */
    IPage<TrackListVo> getUserTrackPage(Page<TrackListVo> page, TrackInfoQuery trackInfoQuery);

    /**
     * 删除声音
     * @param trackId
     */
    void removeTrackInfoById(Long trackId);

    /**
     * 根据声音Id回显数据
     * @param trackId
     * @return
     */
    TrackInfo getTrackInfo(Long trackId);

    /**
     * 修改声音
     * @param trackId
     * @param trackInfoVo
     */
    void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo);

    /**
     * 根据专辑Id获取专辑对应的声音列表
     * @param albumTrackListVoPage
     * @param albumId
     * @param userId
     * @return
     */
    IPage<AlbumTrackListVo> getAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, Long albumId, Long userId);

    /**
     * 更新声音播放次数
     * @param trackStatMqVo
     */
    void updateTrackStat(TrackStatMqVo trackStatMqVo);

    /**
     * 根据声音Id获取到用户分集购买列表
     * @param trackId
     * @return
     */
    List<Map<String, Object>> findUserTrackPaidList(Long trackId);

    /**
     * 批量获取下单付费声音列表
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount);
}
