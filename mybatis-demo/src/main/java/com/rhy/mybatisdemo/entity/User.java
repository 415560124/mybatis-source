package com.rhy.mybatisdemo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author: Herion Lemon
 * @date: 2021年05月27日 16:43:00
 * @slogan: 如果你想攀登高峰，切莫把彩虹当梯子
 * @description:
 */
@Data
@Accessors(chain = true)
public class User {
    private Long id ;
    private String userName ;
    private Date createTime;
}
