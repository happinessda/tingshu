package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    /**
     * 批量获取下单付费声音列表
     * @param trackId
     * @param trackCount
     * @return
     */
    @TsLogin
    @Operation(summary = "批量获取下单付费声音列表")
    @GetMapping("/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable("trackId") Long trackId, @PathVariable("trackCount") Integer trackCount){
        //  调用服务层方法. 根据用户Id 有关系！== 除去用户已购买的声音Id; 在user微服务
        List<TrackInfo> trackInfoList = trackInfoService.findPaidTrackInfoList(trackId, trackCount);
        //  返回数据
        return Result.ok(trackInfoList);
    }

    /**
     * 根据声音Id获取到用户分集购买列表
     * @param trackId
     * @return
     */
    @TsLogin
    @Operation(summary = "根据声音Id获取到用户分集购买列表")
    @GetMapping("/findUserTrackPaidList/{trackId}")
    public Result findUserTrackPaidList(@PathVariable Long trackId) {
        //  调用服务层方法
        List<Map<String,Object>> mapList = trackInfoService.findUserTrackPaidList(trackId);
        //  返回数据
        return Result.ok(mapList);
    }
    /**
     *  根据专辑Id获取到专辑对应的声音列表
     * @param albumId
     * @param pageNo
     * @param pageSize
     * @return
     */
    @TsLogin(required = false)
    @Operation(summary = "根据专辑Id获取到专辑对应的声音列表")
    @GetMapping("/findAlbumTrackPage/{albumId}/{pageNo}/{pageSize}")
    public Result findAlbumTrackPage(@PathVariable Long albumId,
                                     @PathVariable Long pageNo,
                                     @PathVariable Long pageSize) {
        //  获取到用户Id
        Long userId = AuthContextHolder.getUserId();
        //  构建分页对象
        Page<AlbumTrackListVo> albumTrackListVoPage = new Page<>(pageNo, pageSize);
        //  调用服务层方法
        IPage<AlbumTrackListVo> iPage = trackInfoService.getAlbumTrackPage(albumTrackListVoPage, albumId, userId);
        //  返回数据
        return Result.ok(iPage);
    }

    /**
     * 修改声音
     *
     * @param trackId
     * @param trackInfoVo
     * @return
     */
    @Operation(summary = "修改声音")
    @PutMapping("/updateTrackInfo/{trackId}")
    public Result updateTrackInfo(@PathVariable Long trackId,
                                  @RequestBody TrackInfoVo trackInfoVo) {
        //	调用服务层方法
        trackInfoService.updateTrackInfo(trackId, trackInfoVo);
        //	返回数据
        return Result.ok();
    }

    /**
     * 根据声音Id回显数据
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "根据声音Id回显数据")
    @GetMapping("/getTrackInfo/{trackId}")
    public Result getTrackInfo(@PathVariable Long trackId) {
        //	调用服务层方法
        TrackInfo trackInfo = this.trackInfoService.getTrackInfo(trackId);
        //	返回数据
        return Result.ok(trackInfo);
    }

    /**
     * 根据声音Id删除声音
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "根据声音Id删除声音")
    @DeleteMapping("/removeTrackInfo/{trackId}")
    public Result removeTrackInfo(@PathVariable Long trackId) {
        //	调用服务层方法
        trackInfoService.removeTrackInfoById(trackId);
        //	返回数据
        return Result.ok();
    }

    /**
     * 查看声音分页列表
     *
     * @param pageNo
     * @param pageSize
     * @param trackInfoQuery
     * @return
     */
    @Operation(summary = "查看声音列表")
    @PostMapping("/findUserTrackPage/{pageNo}/{pageSize}")
    public Result findUserTrackPage(@PathVariable Long pageNo,
                                    @PathVariable Long pageSize,
                                    @RequestBody TrackInfoQuery trackInfoQuery) {
        //	需要获取用户Id
        Long userId = AuthContextHolder.getUserId() == null ? 1l : AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        //	构建分页对象
        Page<TrackListVo> page = new Page<>(pageNo, pageSize);
        //	调用服务层方法
        IPage<TrackListVo> iPage = this.trackInfoService.getUserTrackPage(page, trackInfoQuery);
        return Result.ok(iPage);
    }

    /**
     * 保存声音
     *
     * @param trackInfoVo
     * @return
     */
    @Operation(summary = "保存声音")
    @PostMapping("/saveTrackInfo")
    public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {
        //	获取用户Id
        Long userId = AuthContextHolder.getUserId() == null ? 1l : AuthContextHolder.getUserId();
        //	调用服务层方法
        trackInfoService.saveTrackInfo(trackInfoVo, userId);
        //	返回数据
        return Result.ok();
    }

    /**
     * 上传声音
     *
     * @param file
     * @return
     */
    @Operation(summary = "上传声音")
    @PostMapping("/uploadTrack")
    public Result uploadTrack(MultipartFile file) {
        //	调用服务层方法
        Map<String, Object> map = trackInfoService.uploadTrack(file);
        //	返回数据
        return Result.ok(map);
    }

}

