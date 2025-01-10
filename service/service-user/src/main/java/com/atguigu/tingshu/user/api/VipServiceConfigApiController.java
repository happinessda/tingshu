package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user/vipServiceConfig")
@SuppressWarnings({"all"})
public class VipServiceConfigApiController {

	@Autowired
	private VipServiceConfigService vipServiceConfigService;

	/**
	 * 根据itemId获取VIP服务配置信息
	 * @param id
	 * @return
	 */
	@Operation(summary = "根据itemId获取VIP服务配置信息")
	@GetMapping("/getVipServiceConfig/{id}")
	public Result<VipServiceConfig> getVipServiceConfig(@PathVariable("id") Long id){
		//	调用服务层方法
		return Result.ok( vipServiceConfigService.getById(id));
	}

	/**
	 * 获取全部服务配置
	 * @return
	 */
	@Operation(summary = "获取全部服务配置")
	@GetMapping("/findAll")
	public Result findAll() {
		//	查询所有vip服务配置
		return Result.ok(vipServiceConfigService.list());
	}

}

