package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "专辑详情管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"all"})
public class itemApiController {

	@Autowired
	private ItemService itemService;

	//	http://127.0.0.1/api/search/albumInfo/1429

	/**
	 *  根据专辑Id获取专辑详情
	 * @param albumId
	 * @return
	 */
	@Operation(summary = "根据专辑Id获取专辑详情")
	@GetMapping("{albumId}")
	public Result getAlbumInfoItem(@PathVariable Long albumId){
		//	调用服务层方法.
		Map<String,Object> map = itemService.getAlbumInfoItem(albumId);
		//	返回数据
		return Result.ok(map);
	}

}

