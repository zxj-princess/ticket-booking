package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM `order` WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM `order` WHERE order_number = #{orderNumber} AND deleted = 0")
    Order selectByOrderNumber(@Param("orderNumber") String orderNumber);

    @Select("<script>" +
            "SELECT COUNT(*) AS totalOrders, " +
            "SUM(total_amount) AS totalAmount, " +
            "SUM(CASE WHEN status IN (1, 2) THEN 1 ELSE 0 END) AS completedOrders, " +
            "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS refundOrders " +
            "FROM `order` WHERE deleted = 0 " +
            "<if test='userId != null'>AND user_id = #{userId}</if>" +
            "</script>")
    Map<String, Object> selectOrderStats(@Param("userId") Long userId);
}
