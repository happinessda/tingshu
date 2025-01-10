package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;

    /**
     * 通过专辑Id 获取到专辑状态信息
     * @param albumId
     * @return
     */
    @GetMapping("/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId){
        //  调用服务层方法
        AlbumStatVo albumStatVo = albumInfoService.getAlbumStatVo(albumId);
        //  返回数据
        return Result.ok(albumStatVo);
    }

    /**
     * 获取专辑属性值列表
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "获取专辑属性值列表")
    @GetMapping("/findAlbumAttributeValue/{albumId}")
    public Result<List<AlbumAttributeValue>> findAlbumAttributeValue(@PathVariable("albumId") Long albumId) {
        //  调用服务层方法
        List<AlbumAttributeValue> attributeValueList = this.albumInfoService.getAlbumAttributeValueList(albumId);
        //  返回数据
        return Result.ok(attributeValueList);
    }

    /**
     * 获取当前用户专辑列表
     *
     * @return
     */
    @Operation(summary = "获取当前用户全部专辑列表")
    @GetMapping("findUserAllAlbumList")
    public Result findUserAllAlbumList() {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId() == null ? 1l : AuthContextHolder.getUserId();
        //  获取用户对应的专辑列表
        List<AlbumInfo> albumInfoList = this.albumInfoService.findUserAllAlbumList(userId);
        //  返回数据
        return Result.ok(albumInfoList);
    }

    /**
     * 根据专辑Id修改专辑数据 @RequestBody ： Json --> JavaObject
     *
     * @param albumId
     * @param albumInfoVo
     * @return
     */
    @Operation(summary = "根据专辑Id修改专辑数据")
    @PutMapping("/updateAlbumInfo/{albumId}")
    public Result updateAlbumInfo(@PathVariable Long albumId, @RequestBody AlbumInfoVo albumInfoVo) {
        //  调用服务层方法
        albumInfoService.updateAlbumInfo(albumId, albumInfoVo);
        //  返回数据
        return Result.ok();
    }

    /**
     * 根据专辑Id获取专辑对象
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "根据专辑Id获取专辑对象")
    @GetMapping("/getAlbumInfo/{albumId}")
    public Result getAlbumInfo(@PathVariable Long albumId) {
        //  调用服务层方法
        AlbumInfo albumInfo = this.albumInfoService.getAlbumInfo(albumId);
        //  返回数据
        return Result.ok(albumInfo);
    }

    /**
     * 根据专辑Id删除专辑数据
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "根据专辑Id删除专辑数据")
    @DeleteMapping("/removeAlbumInfo/{albumId}")
    public Result removeAlbumInfo(@PathVariable Long albumId) {
        //  调用服务层方法
        this.albumInfoService.removeAlbumInfoById(albumId);
        //  返回数据
        return Result.ok();
    }


    /**
     * 保存专辑,必须要实现登录！
     *
     * @param albumInfoVo
     * @return
     */
    @TsLogin
    @Operation(summary = "保存专辑")
    @PostMapping("/saveAlbumInfo")
    public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {
        //	登录功能写完成之后，会将用户Id 存储到本地 线程中!
        Long userId = AuthContextHolder.getUserId() == null ? 1l : AuthContextHolder.getUserId();
        //	调用服务层方法；
        albumInfoService.saveAlbumInfo(albumInfoVo, userId);
        //	返回数据
        return Result.ok();
    }

    /**
     * 查看专辑分页列表
     *
     * @param pageNo
     * @param pageSize
     * @param albumInfoQuery
     * @return
     */
    @TsLogin
    @Operation(summary = "查看专辑分页列表")
    @PostMapping("/findUserAlbumPage/{pageNo}/{pageSize}")
    public Result findUserAlbumPage(@PathVariable Long pageNo,
                                    @PathVariable Long pageSize,
                                    @RequestBody AlbumInfoQuery albumInfoQuery) {
        //  构建分页对象
        Page<AlbumListVo> albumListVoPage = new Page<>(pageNo, pageSize);
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId() == null ? 1l : AuthContextHolder.getUserId();
        //  封装数据
        albumInfoQuery.setUserId(userId);
        //  调用服务层方法
        IPage<AlbumListVo> iPage = albumInfoService.findUserAlbumPage(albumListVoPage, albumInfoQuery);
        //  返回数据
        return Result.ok(iPage);
    }

}

