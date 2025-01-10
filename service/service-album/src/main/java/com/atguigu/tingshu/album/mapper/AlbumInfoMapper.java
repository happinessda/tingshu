package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    IPage<AlbumListVo> selectUserAlbumPage(Page<AlbumListVo> albumListVoPage, @Param("vo") AlbumInfoQuery albumInfoQuery);

    /**
     * 查询专辑统计数据
     * @param albumId
     * @return
     */
    @Select("select album_id,\n" +
            "       max(if(stat.stat_type = '0401', stat.stat_num, 0)) playStatNum,\n" +
            "       max(if(stat.stat_type = '0402', stat.stat_num, 0)) subscribeStatNum,\n" +
            "       max(if(stat.stat_type = '0403', stat.stat_num, 0)) buyStatNum,\n" +
            "       max(if(stat.stat_type = '0404', stat.stat_num, 0)) commentStatNum\n" +
            "from album_stat stat\n" +
            "where album_id = #{albumId}\n" +
            "group by album_id")
    AlbumStatVo selectAlbumStat(@Param("albumId") Long albumId);

    /**
     * 更新专辑统计数据
     * @param albumId
     * @param count
     * @param statType
     */
    @Update("update album_stat set stat_num = stat_num + #{count} where stat_type = #{statType} and album_id = #{albumId} and is_deleted = 0")
    void updateAlbumStat(@Param("albumId") Long albumId, @Param("count") Integer count, @Param("statType") String statType);

}
