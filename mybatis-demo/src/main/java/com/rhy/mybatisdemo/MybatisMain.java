package com.rhy.mybatisdemo;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * @author: Herion Lemon
 * @date: 2021年05月27日 16:43:00
 * @slogan: 如果你想攀登高峰，切莫把彩虹当梯子
 * @description:
 */
public class MybatisMain {
    public static void main(String[] args) throws IOException {
        String mybatisConfig = "mybatis-config.xml";
        //将XML配置读取成Reader流
        Reader reader = Resources.getResourceAsReader(mybatisConfig);
        /**
         * 加载读取配置文件，并构建一个{@link SqlSessionFactory}
         */
        SqlSessionFactory build = new SqlSessionFactoryBuilder().build(reader);
    }
}
