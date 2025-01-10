package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;

	/**
	 * 查看我的订单  /findUserPage/{pageNo}/{pageSize}?orderStatus=UNPAID
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	@TsLogin
	@Operation(summary = "查看我的订单")
	@GetMapping("/findUserPage/{pageNo}/{pageSize}")
	public Result findUserPage(@PathVariable Long pageNo,
							   @PathVariable Long pageSize,
							   HttpServletRequest request) {
		//	获取用户Id
		Long userId = AuthContextHolder.getUserId();
		//	获取订单状态
		String orderStatus = request.getParameter("orderStatus");
		//	构建分页对象
		Page<OrderInfo> orderInfoPage = new Page<>(pageNo,pageSize);
		//	调用服务层方法
		IPage<OrderInfo> iPage = this.orderInfoService.findUserPage(orderInfoPage,userId,orderStatus);
		//	返回数据
		return Result.ok(iPage);
	}

	/**
	 * 根据订单编号查看订单
	 * @param orderNo
	 * @return
	 */
	@TsLogin
	@Operation(summary = "根据订单编号查看订单")
	@GetMapping("/getOrderInfo/{orderNo}")
	public Result getOrderInfo(@PathVariable String orderNo) {
		//	调用服务层方法
		OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(orderNo);
		//	返回数据
		return Result.ok(orderInfo);
	}

	/**
	 * 立即结算
	 * @param orderInfoVo
	 * @return
	 */
	@TsLogin
	@Operation(summary = "立即结算")
	@PostMapping("/submitOrder")
	public Result submitOrder(@RequestBody OrderInfoVo orderInfoVo) {
		//	获取用户Id
		Long userId = AuthContextHolder.getUserId();
		//	调用服务层方法
		String orderNo = this.orderInfoService.submitOrder(orderInfoVo,userId);
		//	创建Map 集合
		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("orderNo",orderNo);
		//	返回数据
		return Result.ok(hashMap);
	}

	/**
	 * 订单结算页
	 * @param tradeVo
	 * @return
	 */
	@TsLogin
	@Operation(summary = "订单结算页")
	@PostMapping("/trade")
	public Result trade(@RequestBody TradeVo tradeVo) {
		//	获取用户Id
		Long userId = AuthContextHolder.getUserId();
		//	调用服务层方法
		OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo,userId);
		//	返回数据
		return Result.ok(orderInfoVo);
	}

}

