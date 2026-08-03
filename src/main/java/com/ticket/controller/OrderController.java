package com.ticket.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.exception.BusinessException;
import com.ticket.common.result.R;
import com.ticket.dto.BookDTO;
import com.ticket.entity.Order;
import com.ticket.entity.Passenger;
import com.ticket.entity.Station;
import com.ticket.entity.Train;
import com.ticket.mapper.PassengerMapper;
import com.ticket.service.OrderService;
import com.ticket.service.StationService;
import com.ticket.service.TrainService;
import com.ticket.service.UserService;
import com.ticket.vo.OrderVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private TrainService trainService;

    @Autowired
    private UserService userService;

    @Autowired
    private StationService stationService;

    @Autowired
    private PassengerMapper passengerMapper;

    @PostMapping("/book")
    public R<OrderVO> bookTicket(@RequestBody BookDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Train train = trainService.getTrainById(dto.getTrainId());

        Station fromStation = stationService.getStationById(train.getFromStationId());
        Station toStation = stationService.getStationById(train.getToStationId());

        Order order = new Order();
        order.setUserId(userId);
        order.setTrainId(train.getId());
        order.setTrainNumber(train.getTrainNumber());
        order.setFromStation(fromStation != null ? fromStation.getName() : "");
        order.setToStation(toStation != null ? toStation.getName() : "");
        order.setSeatType(dto.getSeatType());
        order.setStatus(0);

        // 解析乘客信息
        if (dto.getPassengerIds() != null && !dto.getPassengerIds().isEmpty()) {
            Long passengerId = dto.getPassengerIds().get(0);
            Passenger passenger = passengerMapper.selectById(passengerId);
            if (passenger == null) {
                throw new BusinessException("乘客信息不存在");
            }
            order.setPassengerId(passenger.getId());
            order.setPassengerName(passenger.getName());
            order.setPassengerIdCard(passenger.getIdCard());
        } else {
            order.setPassengerId(0L);
            order.setPassengerName("");
            order.setPassengerIdCard("");
        }

        // 计算时间和价格
        int seatCount = dto.getSeatCount() != null ? dto.getSeatCount() : 1;
        BigDecimal seatPrice = getSeatPrice(train, dto.getSeatType());
        order.setTotalAmount(seatPrice.multiply(BigDecimal.valueOf(seatCount)));

        // 设置发车和到达时间
        try {
            LocalDate today = LocalDate.now();
            if (dto.getDate() != null) {
                today = LocalDate.parse(dto.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            LocalTime departTime = LocalTime.parse(train.getDepartTime(), DateTimeFormatter.ofPattern("HH:mm:ss"));
            LocalTime arriveTime = LocalTime.parse(train.getArriveTime(), DateTimeFormatter.ofPattern("HH:mm:ss"));
            order.setDepartTime(LocalDateTime.of(today, departTime));
            order.setArriveTime(LocalDateTime.of(today, arriveTime));
        } catch (Exception e) {
            order.setDepartTime(LocalDateTime.now());
            order.setArriveTime(LocalDateTime.now().plusMinutes(train.getDuration() != null ? train.getDuration() : 60));
        }

        orderService.createOrder(order);
        return R.success(convertToVO(order));
    }

    @GetMapping("/{orderNumber}")
    public R<OrderVO> getOrder(@PathVariable String orderNumber) {
        Order order;
        if (orderNumber.matches("\\d+")) {
            order = orderService.getOrderById(Long.parseLong(orderNumber));
        } else {
            order = orderService.getOrderByNumber(orderNumber);
        }
        return R.success(convertToVO(order));
    }

    @GetMapping("/id/{id}")
    public R<OrderVO> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return R.success(convertToVO(order));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/my")
    public R<Object> listMyOrders(HttpServletRequest request,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        Page<Order> orderPage = orderService.listByUserId(userId, page, size);
        IPage<OrderVO> voPage = orderPage.convert(this::convertToVO);
        return R.success((Object) voPage);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/list")
    public R<Object> listAllOrders(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) Integer status) {
        Page<Order> orderPage = orderService.listAllOrders(page, size, status);
        IPage<OrderVO> voPage = orderPage.convert(this::convertToVO);
        return R.success((Object) voPage);
    }

    @PostMapping("/refund/{orderNumber}")
    public R<Void> refundOrder(@PathVariable String orderNumber) {
        Order order = resolveOrder(orderNumber);
        orderService.refundOrder(order.getOrderNumber());
        return R.success();
    }

    @PostMapping("/change/{orderNumber}")
    public R<OrderVO> changeOrder(@PathVariable String orderNumber,
                                   @RequestBody BookDTO dto) {
        Order order = resolveOrder(orderNumber);
        orderService.changeOrder(order.getOrderNumber(), dto.getTrainId(), dto.getSeatType());
        Order updated = orderService.getOrderByNumber(order.getOrderNumber());
        return R.success(convertToVO(updated));
    }

    @PostMapping("/pay/{orderNumber}")
    public R<Void> payOrder(@PathVariable String orderNumber) {
        orderService.payOrder(orderNumber);
        return R.success();
    }

    @PostMapping("/repair/{orderNumber}")
    public R<Void> repairOrder(@PathVariable String orderNumber,
                                 @RequestParam Long trainId,
                                 @RequestParam(required = false) Integer seatType,
                                 @RequestParam(required = false) String date) {
        orderService.repairOrderAfterChange(orderNumber, trainId, seatType, date);
        return R.success();
    }

    @PostMapping("/repair-stations/{orderNumber}")
    public R<Void> repairOrderStations(@PathVariable String orderNumber,
                                        @RequestParam String fromStation,
                                        @RequestParam String toStation) {
        orderService.repairOrderStations(orderNumber, fromStation, toStation);
        return R.success();
    }

    private Order resolveOrder(String orderNumberOrId) {
        if (orderNumberOrId.matches("\\d+")) {
            return orderService.getOrderById(Long.parseLong(orderNumberOrId));
        }
        return orderService.getOrderByNumber(orderNumberOrId);
    }

    private BigDecimal getSeatPrice(Train train, Integer seatType) {
        if (seatType == null || seatType == 0) {
            return train.getBusinessPrice();
        } else if (seatType == 1) {
            return train.getFirstPrice();
        } else if (seatType == 2) {
            return train.getSecondPrice();
        } else if (seatType == 3) {
            return train.getSoftPrice();
        } else if (seatType == 4) {
            return train.getHardPrice();
        } else if (seatType == 5) {
            return train.getSeatPrice();
        }
        return train.getSecondPrice();
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        vo.setOrderNo(order.getOrderNumber());
        vo.setTrainNo(order.getTrainNumber());
        vo.setStatusStr(orderStatusStr(order.getStatus()));
        vo.setSeatLabel(seatTypeLabel(order.getSeatType()));
        vo.setDate(formatDate(order.getDepartTime()));
        vo.setDepartTimeStr(formatDateTime(order.getDepartTime()));
        vo.setArriveTimeStr(formatDateTime(order.getArriveTime()));
        vo.setDurationText(calcDuration(order.getDepartTime(), order.getArriveTime()));
        vo.setTotalPrice(order.getTotalAmount());
        vo.setPrice(order.getTotalAmount());

        return vo;
    }

    /** 计算历时文案：如 "1小时30分" */
    private String calcDuration(LocalDateTime depart, LocalDateTime arrive) {
        if (depart == null || arrive == null) return "";
        long minutes = java.time.Duration.between(depart, arrive).toMinutes();
        if (minutes < 0) minutes = 0;
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (hours == 0) return mins + "分钟";
        if (mins == 0) return hours + "小时";
        return hours + "小时" + mins + "分";
    }

    private String orderStatusStr(Integer status) {
        if (status == null) return "UNPAID";
        switch (status) {
            case 0: return "UNPAID";      // 待支付
            case 1: return "PENDING";    // 待出行
            case 2: return "COMPLETED";  // 已完成
            case 3: return "REFUNDED";   // 已退票
            case 4: return "CHANGED";    // 已改签
            case 5: return "CANCELLED";  // 已取消
            default: return "UNPAID";
        }
    }

    private String seatTypeLabel(Integer type) {
        if (type == null) return "二等座";
        switch (type) {
            case 0: return "商务座";
            case 1: return "一等座";
            case 2: return "二等座";
            case 3: return "软卧";
            case 4: return "硬卧";
            case 5: return "硬座";
            default: return "二等座";
        }
    }

    private String formatDate(LocalDateTime time) {
        if (time == null) return "";
        return time.toLocalDate().toString();
    }

    private String formatDateTime(LocalDateTime time) {
        if (time == null) return "";
        return time.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
