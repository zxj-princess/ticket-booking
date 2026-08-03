package com.ticket.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.entity.Order;

import java.util.Map;

public interface OrderService {

    void createOrder(Order order);

    void payOrder(String orderNumber);

    Order getOrderByNumber(String orderNumber);

    Order getOrderById(Long id);

    Page<Order> listByUserId(Long userId, int page, int size);

    Page<Order> listAllOrders(int page, int size, Integer status);

    void autoCompletePastOrders();

    void refundOrder(String orderNumber);

    void changeOrder(String orderNumber, Long newTrainId, Integer seatType);

    void changeOrder(String orderNumber, Long newTrainId, Integer seatType, String dateStr);

    void repairOrderAfterChange(String orderNumber, Long trainId, Integer seatType, String dateStr);

    void repairOrderStations(String orderNumber, String fromStation, String toStation);

    Map<String, Object> getOrderStats();
}
