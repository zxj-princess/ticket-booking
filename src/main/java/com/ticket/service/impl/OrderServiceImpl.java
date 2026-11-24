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

    @Autowired
    private com.ticket.booking.InventoryService inventory;

    @Autowired
    private com.ticket.mapper.PassengerMapper passengers;

    @Override
    @Transactional
    public void createOrder(Order order) {
        if (order.getUserId() == null || !order.getUserId().equals(UserContext.getUserId()))
            throw new BusinessException("订单用户不匹配");
        if (order.getSeatType() == null || order.getSeatType() < 0 || order.getSeatType() > 5)
            throw new BusinessException("请选择有效席别");
        if (order.getDepartTime() == null || !order.getDepartTime().isAfter(LocalDateTime.now()))
            throw new BusinessException("不能预订已发车的车次");
        if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0)
            throw new BusinessException("票价无效");
        Train train = trainMapper.selectById(order.getTrainId());
        if (train == null || !Integer.valueOf(1).equals(train.getStatus())) throw new BusinessException("车次不可售");
        var passenger = passengers.selectById(order.getPassengerId());
        if (passenger == null || !order.getUserId().equals(passenger.getUserId())) throw new BusinessException("乘客不属于当前用户");
        var date = order.getDepartTime().toLocalDate();
        if (date.isAfter(java.time.LocalDate.now().plusDays(15)) || train.getRunningDays()==null || !train.getRunningDays().contains(String.valueOf(date.getDayOfWeek().getValue()))) throw new BusinessException("该日期车次未开售");
        order.setTotalAmount(com.ticket.booking.InventoryService.price(train,order.getSeatType()));
        inventory.reserve(train,date,order.getSeatType());
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
        inventory.event(order.getOrderNumber(),"CREATED",order.getTotalAmount(),"锁座15分钟，等待模拟支付");
    }

    @Override
    @Transactional
    public void payOrder(String orderNumber) {
        Order order = orderMapper.selectForUpdate(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        requireOwner(order);
        if (Integer.valueOf(1).equals(order.getStatus())) return;
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BusinessException("订单状态异常，无法支付");
        }
        if (!order.getCreateTime().plusMinutes(15).isAfter(LocalDateTime.now())) throw new BusinessException("支付时间已过，请取消订单或等待自动关单");
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        inventory.event(orderNumber,"PAID",order.getTotalAmount(),"模拟支付完成，不产生真实扣款");
    }

    @Override
    public Order getOrderByNumber(String orderNumber) {
        Order order = orderMapper.selectByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        requireOwner(order);
        return order;
    }

    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        requireOwner(order);
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
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getStatus, 1)
                .isNotNull(Order::getArriveTime)
                .lt(Order::getArriveTime, LocalDateTime.now())
                .set(Order::getStatus, 2)
                .set(Order::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void refundOrder(String orderNumber) {
        Order order = orderMapper.selectForUpdate(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 1. 状态校验
        requireOwner(order);
        if (Integer.valueOf(3).equals(order.getStatus())) return;
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

        inventory.release(trainMapper.selectById(order.getTrainId()),order.getDepartTime().toLocalDate(),order.getSeatType());
        inventory.event(orderNumber,"REFUNDED",refundAmount,"模拟退款，手续费："+refundFee);
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
        Order order = orderMapper.selectForUpdate(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        requireOwner(order);
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
        BigDecimal newPrice = com.ticket.booking.InventoryService.price(newTrain, finalSeatType);
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

        if (newTrain.getStatus()!=1 || !order.getDepartTime().isAfter(LocalDateTime.now())) throw new BusinessException("已发车或目标车次不可售");
        Train oldTrain=trainMapper.selectById(order.getTrainId());
        if (!java.util.Objects.equals(oldTrain.getFromStationId(),newTrain.getFromStationId()) || !java.util.Objects.equals(oldTrain.getToStationId(),newTrain.getToStationId())) throw new BusinessException("只能改签同线路车次");
        var departure=java.time.LocalDateTime.of(targetDate,java.time.LocalTime.parse(newTrain.getDepartTime()));
        if (!departure.isAfter(LocalDateTime.now()) || targetDate.isAfter(java.time.LocalDate.now().plusDays(15)) || (newTrain.getRunningDays()==null || !newTrain.getRunningDays().contains(String.valueOf(targetDate.getDayOfWeek().getValue())))) throw new BusinessException("目标日期未开售或已发车");
        if (oldTrain.getId().equals(newTrainId) && order.getDepartTime().toLocalDate().equals(targetDate) && order.getSeatType().equals(finalSeatType)) throw new BusinessException("请选择不同的车次、日期或席别");
        inventory.lockTrain(Math.min(oldTrain.getId(),newTrainId));
        inventory.lockTrain(Math.max(oldTrain.getId(),newTrainId));
        inventory.release(oldTrain,order.getDepartTime().toLocalDate(),order.getSeatType());
        inventory.reserve(newTrain,targetDate,finalSeatType);
        order.setTrainId(newTrainId); order.setTrainNumber(newTrain.getTrainNumber()); order.setSeatType(finalSeatType);
        order.setDepartTime(departure);
        var arrival=java.time.LocalDateTime.of(targetDate,java.time.LocalTime.parse(newTrain.getArriveTime()));
        order.setArriveTime(arrival.isBefore(departure)?arrival.plusDays(1):arrival);
        order.setTotalAmount(newPrice); order.setStatus(1);
        order.setRemark("已改签；模拟结算差额："+finalAmount+"；手续费："+fee);
        if(finalAmount.signum()<0) refundToWallet(order.getUserId(),finalAmount.abs());
        inventory.event(orderNumber,"CHANGED",finalAmount,"模拟结算；原票价："+oldPrice+"；新票价："+newPrice+"；手续费："+fee);

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
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("balance = COALESCE(balance, 0) + {0}", amount));
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
        throw new BusinessException("修复入口已停用，请使用正式改签接口");
    }

    @Override
    @Transactional
    public void repairOrderStations(String orderNumber, String fromStation, String toStation) {
        Order order = orderMapper.selectForUpdate(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        requireOwner(order);
        order.setFromStation(fromStation);
        order.setToStation(toStation);
        orderMapper.updateById(order);
    }

    @Override
    public Map<String, Object> getOrderStats() {
        return orderMapper.selectOrderStats(null);
    }

    @Override
    @Transactional
    public void cancelOrder(String number, boolean expiredOnly) {
        Order order=orderMapper.selectForUpdate(number);
        if(order==null) throw new BusinessException("订单不存在");
        if(!expiredOnly) requireOwner(order);
        if(!Integer.valueOf(0).equals(order.getStatus())) return;
        if(expiredOnly && order.getCreateTime().plusMinutes(15).isAfter(LocalDateTime.now())) return;
        inventory.release(trainMapper.selectById(order.getTrainId()),order.getDepartTime().toLocalDate(),order.getSeatType());
        order.setStatus(5); order.setCancelTime(LocalDateTime.now()); orderMapper.updateById(order);
        inventory.event(number,"CANCELLED",BigDecimal.ZERO,expiredOnly?"支付超时，库存已释放":"用户取消，库存已释放");
    }

    @Override public BigDecimal quoteRefund(String number) {
        Order order=getOrderByNumber(number);
        if(order.getStatus()!=1 || !order.getDepartTime().isAfter(LocalDateTime.now())) throw new BusinessException("当前订单不可退票");
        return order.getTotalAmount().subtract(calculateRefundFee(order));
    }
    @Override public BigDecimal quoteChange(String number,Long trainId,Integer seat,String date) {
        Order order=getOrderByNumber(number); Train train=trainMapper.selectById(trainId);
        if(train==null || order.getStatus()!=1) throw new BusinessException("当前订单不可改签");
        BigDecimal price=com.ticket.booking.InventoryService.price(train,seat);
        return price.subtract(order.getTotalAmount()).add(calculateChangeFeeV2(order,order.getTotalAmount(),price,java.time.LocalDate.parse(date)));
    }

    private void requireOwner(Order order) {
        Long current = UserContext.getUserId();
        if (current == null) throw new BusinessException("请先登录");
        if (current.equals(order.getUserId())) return;
        User user = userMapper.selectById(current);
        if (user == null || !Integer.valueOf(1).equals(user.getRole())) {
            throw new BusinessException("无权操作该订单");
        }
    }

    private String generateOrderNumber() {
        return "TK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
