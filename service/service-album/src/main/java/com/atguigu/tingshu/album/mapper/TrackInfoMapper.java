package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 分页查询用户声音
     * @param page
     * @param trackInfoQuery
     * @return
     */

    IPage<TrackListVo> selectUserTrackPage(Page<TrackListVo> page, @Param("vo") TrackInfoQuery trackInfoQuery);

    /**
     *
     * @param orderNum
     * @param albumId
     */
    @Update("update track_info set order_num = order_num - 1 where order_num > #{orderNum} and album_id = #{albumId} and is_deleted = 0")
    void updateOrderNum(@Param("orderNum") Integer orderNum, @Param("albumId") Long albumId);

    /**
     * 分页查询专辑声音
     * @param albumTrackListVoPage
     * @param albumId
     * @return
     */
    @Select("select track.id trackId,\n" +
            "       track.track_title,\n" +
            "       track.media_duration,\n" +
            "       track.order_num,\n" +
            "       track.create_time,\n" +
            "       max(if(stat.stat_type = '0701', stat.stat_num, 0)) playStatNum,\n" +
            "       max(if(stat.stat_type = '0704', stat.stat_num, 0)) commentStatNum\n" +
            "       from track_info track inner join track_stat stat on track.id = stat.track_id\n" +
            "       where track.album_id = #{albumId} and track.is_deleted = 0 and track.status = '0501'\n" +
            "group by track.id\n")
    IPage<AlbumTrackListVo> selectAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, @Param("albumId") Long albumId);

    /**
     *  更新专辑播放次数
     * @param trackId
     * @param count
     * @param statType
     */
    @Update("update track_stat set stat_num = stat_num + #{count} where stat_type = #{statType} and track_id = #{trackId} and is_deleted = 0;")
    void updateTrackStat(@Param("trackId") Long trackId, @Param("count") Integer count, @Param("statType") String statType);
}
