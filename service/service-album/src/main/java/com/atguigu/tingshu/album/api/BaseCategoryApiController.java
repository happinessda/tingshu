package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album/category")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {
	
	@Autowired
	private BaseCategoryService baseCategoryService;

	/**
	 * 获取全部一级分类
	 * @return
	 */
	@Operation(summary = "获取所有一级分类数据")
	@GetMapping("/findAllCategory1")
	public Result<List<BaseCategory1>> findAllCategory1(){
		//	获取所有一级分类数据
		List<BaseCategory1> baseCategory1List = baseCategoryService.list();
		return Result.ok(baseCategory1List);
	}

	/**
	 * 根据一级分类Id查询置顶到频道页的三级分类列表
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据一级分类Id查询置顶到频道页的三级分类列表")
	@GetMapping("/findTopBaseCategory3/{category1Id}")
	public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id){
		//	调用服务层方法
		List<BaseCategory3> baseCategory3List = baseCategoryService.findTopBaseCategory3(category1Id);
		//	返回数据
		return Result.ok(baseCategory3List);
	}

	/**
	 * 根据一级分类Id获取全部分类数据
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据一级分类Id获取全部分类数据")
	@GetMapping("/getBaseCategoryList/{category1Id}")
	public Result getBaseCategoryList(@PathVariable Long category1Id){
		//	调用服务层方法
		JSONObject category1 = baseCategoryService.getBaseCategoryById(category1Id);
		//	返回数据
		return Result.ok(category1);
	}


	/**
	 * 根据三级分类Id 获取到分类数据
	 * @param category3Id
	 * @return
	 */
	@Operation(summary = "根据三级分类Id 获取到分类数据")
	@GetMapping("/getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id){
		//	调用服务层方法
		BaseCategoryView baseCategoryView =baseCategoryService.getCategoryView(category3Id);
		//	返回数据
		return Result.ok(baseCategoryView);
	}

	/**
	 * 根据一级分类Id获取属性数据
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据一级分类Id获取属性数据")
	@GetMapping("/findAttribute/{category1Id}")
	public Result findAttribute(@PathVariable Long category1Id){
		//	调用服务层方法
		List<BaseAttribute> baseAttributeList = this.baseCategoryService.findAttribute(category1Id);
		//	返回数据
		return Result.ok(baseAttributeList);
	}

	/**
	 * 获取一级分类数据
	 * @return
	 */
	@Operation(summary = "获取专辑分类数据")
	@GetMapping("/getBaseCategoryList")
	public Result getBaseCategoryList(){
		//	调用服务层方法 查询所有数据并组成成一个 Json 集合并返回数据！ 将返回数据封装到 categoryName categoryId categoryChild
		//	将 Json 中的内容 ，转换为实体类！ 还可以使用Map 来代替实体类！
		/*
		class Param{
			private String categoryName;
			private Long categoryId;
			get/set
		}
		向map中添加数据； 等同于 setCategoryName();
		map.put("categoryName","音乐");
		map.put("categoryId","1");

		map.get("categoryName");
		getCategoryName();
		 */
		List<JSONObject> categoryList = baseCategoryService.getBaseCategoryList();
		//	返回数据
		return Result.ok(categoryList);
	}
}

