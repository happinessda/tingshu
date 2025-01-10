package com.atguigu.tingshu.entity;

import lombok.Data;

import javax.persistence.Column;

/**
 *
 * @author: atguigu
 * @create: 2024-1-9 10:53
 */
@Data
public class CDCEntity {
    // 注意Column 注解必须是persistence包下的
    @Column(name = "id")
    private Long id;
}