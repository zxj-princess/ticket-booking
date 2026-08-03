package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.context.UserContext;
import com.ticket.common.exception.BusinessException;
import com.ticket.entity.Order;
import com.ticket.entity.Train;
import com.ticket.entity.User;
import com.ticket.mapper.OrderMapper;
import com.ticket.mapper.TrainMapper;
import com.ticket.mapper.UserMapper;
import com.ticket.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void createOrder(Order order) {
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
    }

    @Override
    @Transactional
    public void payOrder(String orderNumber) {
        Order order = orderMapper.selectByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BusinessException("订单状态异常，无法支付");
        }
        if (order.getRemark() != null && order.getRemark().contains("已改签")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("新价:(\\d+\\.\\d+)").matcher(order.getRemark());
            if (m.find()) {
                order.setTotalAmount(new BigDecimal(m.group(1)));
            }
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public Order getOrderByNumber(String orderNumber) {
        Order order = orderMapper.selectByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    @Override
    public Page<Order> listByUserId(Long userId, int page, int size) {
        autoCompletePastOrders();
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<Order> listAllOrders(int page, int size, Integer status) {
        autoCompletePastOrders();
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void autoCompletePastOrders() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 1)
               .isNotNull(Order::getDepartTime)
               .lt(Order::getDepartTime, java.time.LocalDateTime.now());
        List<Order> expiredOrders = orderMapper.selectList(wrapper);
        for (Order order : expiredOrders) {
            order.setStatus(2);
            order.setUpdateTime(java.time.LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    @Override
    @Transactional
    public void refundOrder(String orderNumber) {
        Order order = orderMapper.selectByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new BusinessException("当前订单状态不支持退票");
        }
        BigDecimal refundFee = order.getTotalAmount().multiply(new BigDecimal("0.05"));
        BigDecimal refundAmount = order.getTotalAmount().subtract(refundFee);
        order.setStatus(3);
        order.setRefundTime(LocalDateTime.now());
        order.setRefundAmount(refundAmount);
        order.setRefundFee(refundFee);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void changeOrder(String orderNumber, Long newTrainId, Integer seatType) {
        changeOrder(orderNumber, newTrainId, seatType, null);
    }

    @Override
    @Transactional
    public void changeOrder(String orderNumber, Long newTrainId, Integer seatType, String dateStr) {
        Order order = orderMapper.selectByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException("当前订单状态不支持改签，只有待出行的订单才能改签");
        }
        if (order.getRemark() != null && order.getRemark().contains("已改签")) {
            throw new BusinessException("该订单已经改签过一次，每张车票仅可改签一次，无法再次改签");
        }
        if (order.getDepartTime() != null) {
            java.time.LocalDateTime todayEnd = java.time.LocalDate.now().atTime(23, 59, 59);
            if (order.getDepartTime().isBefore(todayEnd) && order.getDepartTime().isBefore(java.time.LocalDateTime.now())) {
                throw new BusinessException("原车次已发车且超过当日24点，不支持改签");
            }
        }
        Train newTrain = trainMapper.selectById(newTrainId);
        if (newTrain == null) {
            throw new BusinessException("新车次不存在");
        }

        int finalSeatType = seatType != null ? seatType : (order.getSeatType() != null ? order.getSeatType() : 2);

        BigDecimal oldPrice = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal newPrice = getSeatPrice(newTrain, finalSeatType);
        BigDecimal diff = newPrice.subtract(oldPrice);
        BigDecimal fee = calculateChangeFee(order, oldPrice);
        BigDecimal finalAmount = diff.add(fee);

        order.setTrainId(newTrain.getId());
        order.setTrainNumber(newTrain.getTrainNumber());
        order.setSeatType(finalSeatType);
        order.setRemark("已改签,原价:" + oldPrice + ",新价:" + newPrice + ",手续费:" + fee);

        java.time.LocalDate travelDate;
        if (dateStr != null && !dateStr.isEmpty()) {
            travelDate = java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else if (order.getDepartTime() != null) {
            travelDate = order.getDepartTime().toLocalDate();
        } else {
            travelDate = java.time.LocalDate.now();
        }

        try {
            java.time.LocalTime departTime = java.time.LocalTime.parse(newTrain.getDepartTime(), java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            java.time.LocalTime arriveTime = java.time.LocalTime.parse(newTrain.getArriveTime(), java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            order.setDepartTime(java.time.LocalDateTime.of(travelDate, departTime));
            order.setArriveTime(java.time.LocalDateTime.of(travelDate, arriveTime));
        } catch (Exception e) {
            order.setDepartTime(order.getDepartTime());
            order.setArriveTime(order.getArriveTime());
        }

        int cmp = finalAmount.compareTo(BigDecimal.ZERO);
        if (cmp > 0) {
            order.setTotalAmount(finalAmount);
            order.setStatus(0);
        } else if (cmp < 0) {
            order.setTotalAmount(newPrice);
            order.setStatus(1);
            refundToWallet(order.getUserId(), finalAmount.abs());
        } else {
            order.setTotalAmount(newPrice);
            order.setStatus(1);
        }

        orderMapper.updateById(order);
    }

    private BigDecimal calculateChangeFee(Order order, BigDecimal oldPrice) {
        if (order.getDepartTime() == null || oldPrice == null) return BigDecimal.ZERO;
        long hours = java.time.Duration.between(java.time.LocalDateTime.now(), order.getDepartTime()).toHours();
        if (hours >= 48) return BigDecimal.ZERO;
        if (hours >= 24) return oldPrice.multiply(new BigDecimal("0.05")).setScale(2, java.math.RoundingMode.HALF_UP);
        return oldPrice.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void refundToWallet(Long userId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        User user = userMapper.selectById(userId);
        if (user == null) return;
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(currentBalance.add(amount));
        userMapper.updateById(user);
    }

    private BigDecimal getSeatPrice(Train train, Integer seatType) {
        if (seatType == null || seatType == 0) {
            return train.getBusinessPrice() != null ? train.getBusinessPrice() : BigDecimal.ZERO;
        } else if (seatType == 1) {
            return train.getFirstPrice() != null ? train.getFirstPrice() : BigDecimal.ZERO;
        } else if (seatType == 2) {
            return train.getSecondPrice() != null ? train.getSecondPrice() : BigDecimal.ZERO;
        } else if (seatType == 3) {
            return train.getSoftPrice() != null ? train.getSoftPrice() : BigDecimal.ZERO;
        } else if (seatType == 4) {
            return train.getHardPrice() != null ? train.getHardPrice() : BigDecimal.ZERO;
        } else if (seatType == 5) {
            return train.getSeatPrice() != null ? train.getSeatPrice() : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void repairOrderAfterChange(String orderNumber, Long trainId, Integer seatType, String dateStr) {
        Order order = orderMapper.selectByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new BusinessException("车次不存在");
        }

        int finalSeatType = seatType != null ? seatType : (order.getSeatType() != null ? order.getSeatType() : 2);

        BigDecimal oldPrice = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal newPrice = getSeatPrice(train, finalSeatType);
        BigDecimal diff = newPrice.subtract(oldPrice);

        order.setTrainId(train.getId());
        order.setTrainNumber(train.getTrainNumber());
        order.setSeatType(finalSeatType);
        order.setRemark("已改签,原价:" + oldPrice + ",新价:" + newPrice);

        java.time.LocalDate travelDate;
        if (dateStr != null && !dateStr.isEmpty()) {
            travelDate = java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else if (order.getDepartTime() != null) {
            travelDate = order.getDepartTime().toLocalDate();
        } else {
            travelDate = java.time.LocalDate.now();
        }

        try {
            java.time.LocalTime departTime = java.time.LocalTime.parse(train.getDepartTime(), java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            java.time.LocalTime arriveTime = java.time.LocalTime.parse(train.getArriveTime(), java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            order.setDepartTime(java.time.LocalDateTime.of(travelDate, departTime));
            order.setArriveTime(java.time.LocalDateTime.of(travelDate, arriveTime));
        } catch (Exception e) {
            // keep original if parsing fails
        }

        int cmp = diff.compareTo(BigDecimal.ZERO);
        if (cmp > 0) {
            order.setTotalAmount(diff);
            order.setStatus(0);
        } else if (cmp < 0) {
            order.setTotalAmount(newPrice);
            order.setStatus(1);
            refundToWallet(order.getUserId(), diff.abs());
        } else {
            order.setTotalAmount(oldPrice);
            order.setStatus(1);
        }

        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void repairOrderStations(String orderNumber, String fromStation, String toStation) {
        Order order = orderMapper.selectByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setFromStation(fromStation);
        order.setToStation(toStation);
        orderMapper.updateById(order);
    }

    @Override
    public Map<String, Object> getOrderStats() {
        return orderMapper.selectOrderStats(null);
    }

    private String generateOrderNumber() {
        return "TK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
