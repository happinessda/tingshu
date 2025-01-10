package com.atguigu.tingshu.user.api;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 修改用户信息
     * @param userInfoVo
     * @return
     */
    @TsLogin
    @Operation(summary = "修改用户信息")
    @PostMapping("updateUser")
    public Result updateUser(@RequestBody UserInfoVo userInfoVo) {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  调用更新方法;
        this.userInfoService.updateUser(userInfoVo,userId);
        //  返回数据
        return Result.ok();
    }

    /**
     * 获取用户信息
     * @return
     */
    @TsLogin
    @Operation(summary = "获取用户信息")
    @GetMapping("/getUserInfo")
    public Result getUserInfo() {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        //  获取用户信息;
        return Result.ok(userInfoVo);
    }

    /**
     * 微信登录
     * @param code 微信小程序前端生成的.code
     * @return
     */
    @Operation(summary = "微信登录")
    @GetMapping("/wxLogin/{code}")
    public Result wxLogin(@PathVariable String code) {
        log.info("微信登录，code：{}", code);
        //  调用服务层方法
        Map<String,Object> result = this.userInfoService.wxLogin(code);
        //  返回数据
        return Result.ok(result);
    }


}
