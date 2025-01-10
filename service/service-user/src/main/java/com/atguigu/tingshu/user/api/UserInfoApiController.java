package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"all"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 更新Vip到期失效状态
     * @return
     */
    @Operation(summary = "更新Vip到期失效状态")
    @GetMapping("/updateVipExpireStatus")
    public Result updateVipExpireStatus(){
        //  调用服务层方法
        userInfoService.updateVipExpireStatus();
        //  返回数据
        return Result.ok();
    }
    /**
     * 处理用户购买记录
     * @param userPaidRecordVo
     * @return
     */
    @Operation(summary = "处理用户购买记录")
    @PostMapping("/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo){
        //  调用服务层方法.
        this.userInfoService.savePaidRecord(userPaidRecordVo);
        //  返回数据
        return Result.ok();
    }

    /**
     * 获取专辑已支付的声音Id集合列表
     * @param albumId
     * @return
     */
    @TsLogin
    @Operation(summary = "获取专辑已支付的声音Id集合列表")
    @GetMapping("/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId){
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  调用服务层方法.
        List<Long> trackIdList = this.userInfoService.findUserPaidTrackList(albumId,userId);
        //  返回数据
        return Result.ok(trackIdList);
    }

    /**
     *  判断用户是否购买过专辑
     * @param albumId
     * @return
     */

    @TsLogin
    @Operation(summary = "判断用户是否购买过专辑")
    @GetMapping("isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId) {
        //  获取到当前用户Id
        Long userId = AuthContextHolder.getUserId();
        //  远程调用服务层方法
        Boolean result = userInfoService.isPaidAlbum(albumId,userId);
        return Result.ok(result);
    }

    /**
     * 判断用户是否购买声音列表
     * @param albumId
     * @param trackIdList
     * @return
     */
    @TsLogin(required = false)
    @Operation(summary = "判断用户是否购买声音列表")
    @PostMapping("userIsPaidTrack/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long albumId, @RequestBody List<Long> trackIdList) {
        //  获取到用户Id
        Long userId = AuthContextHolder.getUserId();
        //  调用服务层方法.
        Map<Long, Integer> map = userInfoService.userIsPaidTrack(albumId, trackIdList,userId);
        //  返回数据
        return Result.ok(map);
    }

    /**
     * 根据用户Id获取用户信息
     * @param userId
     * @return
     */
    @Operation(summary = "根据用户Id获取用户信息")
    @GetMapping("/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId) {
        //  调用服务层方法.
        UserInfoVo userInfoVo = this.userInfoService.getUserInfo(userId);
        //  返回数据
        return Result.ok(userInfoVo);
    }
}
