package com.rhy.mybatisdemo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author: Herion Lemon
 * @date: 2021年06月01日 14:22:00
 * @slogan: 如果你想攀登高峰，切莫把彩虹当梯子
 * @description:
 */
@Data
@Accessors(chain = true)
public class UserCard {
    private Long userId ;
    private Long cardId ;
    private String cardName;
}
