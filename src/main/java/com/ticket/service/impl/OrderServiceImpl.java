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

        // 1. 状态校验
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new BusinessException("当前订单状态不支持退票，只有待出行或已完成的订单才能退票");
        }

        // 2. 已发车拦截
        if (order.getDepartTime() != null && java.time.LocalDateTime.now().isAfter(order.getDepartTime())) {
            throw new BusinessException("列车已发车，不允许退票");
        }

        // 3. 开车后改签的车票禁止退票
        if (order.getRemark() != null && order.getRemark().contains("开车后改签")) {
            throw new BusinessException("开车后改签生成的车票不支持退票");
        }

        // 4. 计算退票费
        BigDecimal refundFee = calculateRefundFee(order);
        BigDecimal refundAmount = order.getTotalAmount().subtract(refundFee);

        order.setStatus(3); // 已退票
        order.setRefundTime(java.time.LocalDateTime.now());
        order.setRefundAmount(refundAmount);
        order.setRefundFee(refundFee);
        orderMapper.updateById(order);

        // 5. 退款到钱包
        refundToWallet(order.getUserId(), refundAmount);
    }

    /**
     * 退票费计算：4档费率
     * - ≥8天: 0%
     * - 48h~8天: 5%
     * - 24h~48h: 10%
     * - <24h: 20%
     * - 最低2元，5角尾数
     */
    private BigDecimal calculateRefundFee(Order order) {
        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime departTime = order.getDepartTime();
        BigDecimal rate;

        if (departTime != null) {
            long hoursToDepart = java.time.Duration.between(now, departTime).toHours();
            long daysToDepart = hoursToDepart / 24;

            if (daysToDepart >= 8) {
                rate = BigDecimal.ZERO;
            } else if (hoursToDepart >= 48) {
                rate = new BigDecimal("0.05");
            } else if (hoursToDepart >= 24) {
                rate = new BigDecimal("0.10");
            } else {
                rate = new BigDecimal("0.20");
            }
        } else {
            rate = new BigDecimal("0.05");
        }

        BigDecimal fee = totalAmount.multiply(rate);

        // 最低2元
        BigDecimal minFee = new BigDecimal("2.00");
        if (fee.compareTo(minFee) < 0 && fee.compareTo(BigDecimal.ZERO) > 0) {
            fee = minFee;
        }

        // 5角尾数处理
        fee = roundTo50Cents(fee);

        // 不超过票面金额
        if (fee.compareTo(totalAmount) > 0) {
            fee = totalAmount;
        }

        return fee.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 5角尾数处理
     * ＜2.5角舍去；≥2.5且＜7.5计5角；≥7.5进1元
     */
    private BigDecimal roundTo50Cents(BigDecimal value) {
        double yuan = value.doubleValue();
        double jiao = (yuan - Math.floor(yuan)) * 10;
        BigDecimal base = BigDecimal.valueOf(Math.floor(yuan));
        if (jiao < 2.5) {
            return base.setScale(2, java.math.RoundingMode.DOWN);
        } else if (jiao < 7.5) {
            return base.add(new BigDecimal("0.50"));
        } else {
            return base.add(BigDecimal.ONE);
        }
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

        java.time.LocalDate targetDate = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            targetDate = java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else if (order.getDepartTime() != null) {
            targetDate = order.getDepartTime().toLocalDate();
        } else {
            targetDate = java.time.LocalDate.now();
        }

        BigDecimal fee = calculateChangeFeeV2(order, oldPrice, newPrice, targetDate);
        BigDecimal finalAmount = diff.add(fee);

        order.setTrainId(newTrain.getId());
        order.setTrainNumber(newTrain.getTrainNumber());
        order.setSeatType(finalSeatType);
        order.setRemark("已改签,原价:" + oldPrice + ",新价:" + newPrice + ",改签费:" + fee + ",差额:" + diff);

        try {
            java.time.LocalTime departTime = java.time.LocalTime.parse(newTrain.getDepartTime(), java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            java.time.LocalTime arriveTime = java.time.LocalTime.parse(newTrain.getArriveTime(), java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            order.setDepartTime(java.time.LocalDateTime.of(targetDate, departTime));
            order.setArriveTime(java.time.LocalDateTime.of(targetDate, arriveTime));
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

    /**
     * 改签手续费V2：7档费率系统
     */
    private BigDecimal calculateChangeFeeV2(Order order, BigDecimal oldPrice, BigDecimal newPrice, java.time.LocalDate targetDate) {
        if (order.getDepartTime() == null || oldPrice == null) return BigDecimal.ZERO;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime departTime = order.getDepartTime();
        boolean isAfterDepart = now.isAfter(departTime);
        long hoursToDepart = java.time.Duration.between(now, departTime).toHours();

        java.time.LocalDate originalDate = departTime.toLocalDate();
        boolean targetAfterOriginal = targetDate != null && targetDate.isAfter(originalDate);

        BigDecimal feeBase = oldPrice.min(newPrice);
        BigDecimal rate = BigDecimal.ZERO;

        if (isAfterDepart) {
            rate = targetAfterOriginal ? new BigDecimal("0.40") : BigDecimal.ZERO;
        } else if (hoursToDepart >= 48) {
            rate = BigDecimal.ZERO;
        } else if (hoursToDepart >= 24) {
            rate = targetAfterOriginal ? new BigDecimal("0.05") : BigDecimal.ZERO;
        } else {
            rate = targetAfterOriginal ? new BigDecimal("0.15") : BigDecimal.ZERO;
        }

        return feeBase.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
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
