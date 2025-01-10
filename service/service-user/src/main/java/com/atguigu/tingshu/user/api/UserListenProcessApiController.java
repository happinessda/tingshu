package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user/userListenProcess")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

	@Autowired
	private UserListenProcessService userListenProcessService;

	/**
	 * 获取最近播放的一次声音
	 * @return
	 */
	@TsLogin
	@Operation(summary = "获取最近播放的一次声音")
	@GetMapping("/getLatelyTrack")
	public Result getLatelyTrack() {
		//	获取到用户Id
		Long userId = AuthContextHolder.getUserId();
		//	调用服务层方法.
		Map<String,Object> map = userListenProcessService.getLatelyTrack(userId);
		//	返回数据.
		return Result.ok(map);
	}
	/**
	 * 获取声音播放进度
	 * @param trackId
	 * @return
	 */
	@TsLogin
	@Operation(summary = "获取声音播放进度")
	@GetMapping("/getTrackBreakSecond/{trackId}")
	public Result getTrackBreakSecond(@PathVariable Long trackId) {
		//	获取到用户Id
		Long userId = AuthContextHolder.getUserId();
		//	调用服务层方法
		BigDecimal trackBreakSecond = this.userListenProcessService.getTrackBreakSecond(trackId,userId);
		//	返回播放进度
		return Result.ok(trackBreakSecond);
	}

	/**
	 * 保存用户声音播放进度
	 * @param userListenProcessVo
	 * @return
	 */
	@TsLogin
	@Operation(summary = "保存用户声音播放进度")
	@PostMapping("/updateListenProcess")
	public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
		//	获取到用户Id
		Long userId = AuthContextHolder.getUserId();
		//	调用服务层方法
		this.userListenProcessService.updateListenProcess(userListenProcessVo,userId);
		//	返回结果
		return Result.ok();
	}
}

