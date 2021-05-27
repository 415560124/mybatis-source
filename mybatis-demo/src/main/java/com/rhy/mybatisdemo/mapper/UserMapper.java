package com.rhy.mybatisdemo.mapper;

import com.rhy.mybatisdemo.entity.User;

import java.util.List;

/**
 * @author: Herion Lemon
 * @date: 2021年05月27日 16:44:00
 * @slogan: 如果你想攀登高峰，切莫把彩虹当梯子
 * @description:
 */
public interface UserMapper {
    User selectById(Long id);

    List<User> selectAllUser();
}
