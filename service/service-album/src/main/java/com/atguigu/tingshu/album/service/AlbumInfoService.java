package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    /**
     * 保存专辑信息
     * @param albumInfoVo
     * @param userId
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo,Long userId);

    /**
     * 查看专辑分页列表
     * @param albumListVoPage
     * @param albumInfoQuery
     * @return
     */
    IPage<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> albumListVoPage,AlbumInfoQuery albumInfoQuery);

    /**
     * 删除专辑
     * @param albumId
     */
    void removeAlbumInfoById(Long albumId);

    /**
     * 根据专辑id查询专辑对象
     * @param albumId
     * @return
     */
    AlbumInfo getAlbumInfo(Long albumId);

    /**
     * 修改专辑
     * @param albumId
     * @param albumInfoVo
     */
    void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo);

    /**
     * 获取当前用户全部专辑列表
     * @param userId
     * @return
     */
    List<AlbumInfo> findUserAllAlbumList(Long userId);

    /**
     * 根据专辑id获取专辑属性值列表
     * @param albumId
     * @return
     */
    List<AlbumAttributeValue> getAlbumAttributeValueList(Long albumId);

    /**
     * 获取专辑统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStatVo(Long albumId);
}
