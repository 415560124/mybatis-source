package com.rhy.mybatisdemo;

import java.sql.*;

/**
 * @author: Herion Lemon
 * @date: 2021年06月07日 16:21:00
 * @slogan: 如果你想攀登高峰，切莫把彩虹当梯子
 * @description:
 */
public class JDBCMain {
    public static void main(String[] args) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            //注册驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            //获取连接对象
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/spring_source?serverTimezone=Asia/Shanghai&useSSL=false", "root", "root");
            //创建SQL语句
            String username = "admin";
            String password = "admin";
            String sql = "select * from user where user_name = ? and password = ?";
            //创建PreparedStatement对象
            pstmt = conn.prepareStatement(sql);
            //给?赋值
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            //执行SQL语句
            rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String unm = rs.getString("user_name");
                String pwd = rs.getString("password");
                System.out.println(id + "--"+ unm + "--" + pwd);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (pstmt != null) {//避免空指针异常
                try {
                    pstmt.close();//释放资源
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
