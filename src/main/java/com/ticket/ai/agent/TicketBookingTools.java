package com.ticket.ai.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.ai.rag.KnowledgeService;
import com.ticket.common.context.UserContext;
import com.ticket.dto.BookDTO;
import com.ticket.entity.Order;
import com.ticket.entity.Passenger;
import com.ticket.entity.Station;
import com.ticket.entity.Train;
import com.ticket.mapper.OrderMapper;
import com.ticket.mapper.PassengerMapper;
import com.ticket.mapper.TrainMapper;
import com.ticket.service.OrderService;
import com.ticket.service.StationService;
import com.ticket.service.TrainService;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TicketBookingTools {

    private static final Logger log = LoggerFactory.getLogger(TicketBookingTools.class);

    /**
     * 当前AI对话的用户ID（解决LangChain4j异步调用导致ThreadLocal丢失的问题）
     */


    /**
     * 设置当前AI对话的用户ID，在Agent调用前设置，调用后清除
     */
    public void setCurrentUserId(Long userId) {
        // Identity comes from the authenticated request context only.
    }

    /**
     * 获取当前AI对话的用户ID，优先使用保存的ID，回退到UserContext
     */
    private Long getCurrentUserId() {
        return UserContext.getUserId();
    }

    @Autowired
    private TrainService trainService;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private StationService stationService;

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private OrderService orderService;
    @Autowired private com.ticket.booking.ActionService actions;
    @Autowired private com.ticket.booking.InventoryService inventory;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private PassengerMapper passengerMapper;

    @Tool("查询车次信息。参数fromCity为出发城市名，toCity为到达城市名，date为出发日期(格式yyyy-MM-dd，可选，默认今天)。返回符合条件的车次列表信息，包含车次ID用于后续订票/改签操作。")
    public String queryTrains(String fromCity, String toCity, String date) {
        try {
            if (date == null || date.isEmpty()) {
                date = LocalDate.now().toString();
            }

            List<Station> fromStations = stationService.searchStations(fromCity);
            List<Station> toStations = stationService.searchStations(toCity);

            if (fromStations.isEmpty()) {
                return "未找到出发城市「" + fromCity + "」对应的站点，请确认城市名称是否正确。";
            }
            if (toStations.isEmpty()) {
                return "未找到到达城市「" + toCity + "」对应的站点，请确认城市名称是否正确。";
            }

            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int dayOfWeek = localDate.getDayOfWeek().getValue();

            List<Long> fromIds = fromStations.stream().map(Station::getId).toList();
            List<Long> toIds = toStations.stream().map(Station::getId).toList();
            List<Train> trains = trainMapper.selectByStationIdsAndDate(fromIds, toIds, dayOfWeek);

            StringBuilder result = new StringBuilder();
            result.append("【").append(fromCity).append(" → ").append(toCity).append("】 (").append(date).append(") 共").append(trains.size()).append("趟车次：\n\n");

            if (trains.isEmpty()) {
                result.append("该日期没有查询到从「").append(fromCity).append("」到「").append(toCity).append("」的车次。");
            } else {
                for (Train t : trains) {
                    t.setBusinessSeats(inventory.available(t,localDate,0)); t.setFirstSeats(inventory.available(t,localDate,1)); t.setSecondSeats(inventory.available(t,localDate,2));
                    t.setSoftSleepers(inventory.available(t,localDate,3)); t.setHardSleepers(inventory.available(t,localDate,4)); t.setHardSeats(inventory.available(t,localDate,5));
                    result.append(formatTrainInfo(t)).append("\n");
                }
            }
            return result.toString();
        } catch (Exception e) {
            return "查询车次失败：" + e.getMessage();
        }
    }

    @Tool("获取指定车次的详细信息。参数trainNumber为车次号(如G1234)或车次ID(数字)。返回车次的完整时刻、座位、价格等信息。")
    public String getTrainDetails(String trainNumber) {
        try {
            Train train = null;
            try {
                Long id = Long.parseLong(trainNumber);
                train = trainMapper.selectById(id);
            } catch (NumberFormatException ignored) {
                LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Train::getTrainNumber, trainNumber.toUpperCase());
                train = trainMapper.selectOne(wrapper);
            }
            if (train == null) {
                return "未找到车次「" + trainNumber + "」的信息。";
            }
            return formatTrainDetail(train);
        } catch (Exception e) {
            return "查询车次详情失败：" + e.getMessage();
        }
    }

    @Tool("计算指定车次和座位类型的票价。参数trainNumber为车次号，seatType为座位类型(可选:商务座/一等座/二等座/软卧/硬卧/硬座)。返回对应座位的价格和余票信息。")
    public String calculateTicketPrice(String trainNumber, String seatType) {
        try {
            LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Train::getTrainNumber, trainNumber.toUpperCase());
            Train train = trainMapper.selectOne(wrapper);
            if (train == null) {
                return "未找到车次「" + trainNumber + "」的信息，无法计算票价。";
            }

            StringBuilder result = new StringBuilder();
            result.append("车次 ").append(trainNumber).append(" 的票价信息：\n\n");

            Map<String, Object> SeatMap = getSeatInfo(train);
            if (seatType != null && !seatType.isEmpty()) {
                Object info = SeatMap.get(seatType);
                if (info != null) {
                    result.append("【").append(seatType).append("】 ").append(info).append("\n");
                } else {
                    result.append("抱歉，车次「").append(trainNumber).append("」没有").append(seatType).append("这种座位类型。\n");
                    result.append("可选座位类型：").append(String.join("、", SeatMap.keySet())).append("\n");
                }
            } else {
                result.append("所有座位类型票价：\n");
                for (Map.Entry<String, Object> entry : SeatMap.entrySet()) {
                    result.append("  【").append(entry.getKey()).append("】 ").append(entry.getValue()).append("\n");
                }
            }
            return result.toString();
        } catch (Exception e) {
            return "计算票价失败：" + e.getMessage();
        }
    }

    @Tool("获取全国主要火车站点列表。返回所有可用的站点信息。")
    public String getStationList() {
        try {
            List<Station> stations = stationService.listStations();
            if (stations.isEmpty()) {
                return "当前系统中没有站点数据。";
            }
            StringBuilder result = new StringBuilder();
            result.append("全国主要火车站点列表（共").append(stations.size()).append("个）：\n\n");

            Map<String, List<Station>> grouped = stations.stream()
                    .collect(Collectors.groupingBy(s -> s.getProvince() != null ? s.getProvince() : "未知"));

            for (Map.Entry<String, List<Station>> entry : grouped.entrySet()) {
                result.append("【").append(entry.getKey()).append("】\n");
                for (Station s : entry.getValue()) {
                    result.append("  ").append(s.getName());
                    if (s.getCode() != null) {
                        result.append(" (").append(s.getCode()).append(")");
                    }
                    result.append("\n");
                }
            }
            return result.toString();
        } catch (Exception e) {
            return "获取站点列表失败：" + e.getMessage();
        }
    }

    @Tool("获取火车票退票规则说明。返回退票的时间规定、手续费标准等信息。")
    public String getRefundRules() {
        return """
                火车票退票规则：
                
                1. 退票时间规定（按距发车时间计算）：
                   - 开车前8天（含）以上退票：不收手续费
                   - 开车前48小时至8天以内退票：收取票面金额5%的手续费
                   - 开车前24小时至48小时以内退票：收取票面金额10%的手续费
                   - 开车前24小时以内退票：收取票面金额20%的手续费
                
                2. 手续费规则：
                   - 最低退票费2元
                   - 金额尾数四舍五入到5角
                
                3. 禁止退票场景：
                   - 列车已经发车
                   - 开车后改签生成的车票
                   - 车票已退票/作废
                
                4. 退票方式：
                   - 网上购票可在12306官网或APP直接退票
                   - 车站购票需到车站售票窗口退票
                   - 已换取纸质车票的，需到车站窗口办理
                
                5. 到账时间：
                   - 退票款将原路返回至支付账户，一般3-7个工作日到账
                """;
    }

    @Tool("获取丢失车票的处理方法说明。返回电子客票丢失、纸质车票丢失的补办流程。")
    public String getLostTicketHelp() {
        return """
                火车票丢失处理指南：
                
                1. 如果是电子客票（多数情况）：
                   - 电子客票不存在"丢失"问题，直接凭身份证即可乘车
                   - 在12306 APP或车站自助机上可随时查看/下载电子客票
                   - 如果需要报销凭证，可在乘车后30天内打印

                2. 如果是纸质车票丢失：
                   - 请携带购票时使用的有效身份证件
                   - 到车站售票窗口办理挂失补办手续
                   - 需缴纳票面价格的手续费补办新车票
                   - 原车票在办理补办后自动作废

                3. 温馨提示：
                   - 建议使用电子客票，更安全便捷
                   - 乘车当天可在车站自助机上查询和打印车票信息
                   - 如遇特殊情况请联系车站工作人员协助
                """;
    }

    @Tool("获取火车票改签规则说明。改签是指改变乘车日期、车次或席位。返回改签的条件、手续费等信息。")
    public String getChangeRules() {
        return """
                火车票改签规则：

                1. 改签基本规则：
                   - 每张车票仅可改签一次
                   - 不支持改变出发站
                   - 开车后改签的车票不支持退票
                   - 已发车且超过当日24点不可改签

                2. 改签手续费（7档费率，按距发车时间×改签目标计算）：
                   - 开车前≥48小时：免费
                   - 开车前24-48小时，改签至原票日期及之前：免费
                   - 开车前24-48小时，改签至原票日期之后：5%
                   - 开车前<24小时，改签至原票日期及之前：免费
                   - 开车前<24小时，改签至原票日期之后：15%
                   - 开车后改签至当日其他列车：免费
                   - 开车后改签至次日及以后：40%
                   - 费率基数 = min(原票价, 新票价)

                3. 票价差额：
                   - 新票价 > 原票价：需补差价
                   - 新票价 < 原票价：退还差价
                   - 改签费与差额独立计算

                4. 办理方式：
                   - 网上购票可在App内直接改签
                   - 车站购票需到车站窗口办理
                """;
    }

    /**
     * 改签阶段1校验：检查订单是否可改签（已改签过/已过期/状态异常均拒绝）。
     * 返回校验结果对象。
     */
    public String validateOrderForChange(String orderNumber) {
        try {
            Order order = findOrder(orderNumber);
            if (order == null) {
                return "FAIL:未找到订单「" + orderNumber + "」，请确认订单号是否正确。";
            }
            if (order.getStatus() == null || order.getStatus() != 1) {
                return "FAIL:当前订单状态为「" + orderStatusText(order.getStatus()) + "」，不支持改签。只有待出行的订单才能改签。";
            }
            if (order.getRemark() != null && order.getRemark().contains("已改签")) {
                return "FAIL:该订单已经改签过一次，根据规则每张车票仅可改签一次，无法再次改签。如需调整行程，请先退票后重新购票。";
            }
            if (order.getDepartTime() != null) {
                java.time.LocalDateTime depart = order.getDepartTime();
                java.time.LocalDateTime todayEnd = java.time.LocalDate.now().atTime(23, 59, 59);
                if (depart.isBefore(todayEnd) && depart.isBefore(java.time.LocalDateTime.now())) {
                    return "FAIL:原车次已于" + depart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "发车，且超过当日24点，不支持改签。";
                }
            }
            return "OK:" + formatOrderInfo(order);
        } catch (Exception e) {
            return "FAIL:校验订单失败：" + e.getMessage();
        }
    }

    /**
     * 改签阶段3：按原订单起止站、目标日期、时段筛选有座位的列车。
     * timePeriod: morning(6-12)/afternoon(12-18)/evening(18-24)/空(全部)
     */
    public String searchChangeTrains(String orderNumber, String date, String timePeriod) {
        try {
            Order order = findOrder(orderNumber);
            if (order == null) return "未找到订单「" + orderNumber + "」。";

            String fromCity = order.getFromStation();
            String toCity = order.getToStation();
            if (fromCity == null || toCity == null) return "原订单缺少起止站信息，无法查询可改签车次。";

            if (date == null || date.isEmpty()) date = LocalDate.now().toString();
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int dayOfWeek = localDate.getDayOfWeek().getValue();

            List<Station> fromStations = stationService.searchStations(fromCity);
            List<Station> toStations = stationService.searchStations(toCity);
            if (fromStations.isEmpty() || toStations.isEmpty()) {
                return "未找到" + fromCity + "到" + toCity + "的站点信息。";
            }

            List<Long> fromIds = fromStations.stream().map(Station::getId).toList();
            List<Long> toIds = toStations.stream().map(Station::getId).toList();
            List<Train> trains = trainMapper.selectByStationIdsAndDate(fromIds, toIds, dayOfWeek);

            int[] range = parseTimePeriod(timePeriod);
            List<Train> filtered = new java.util.ArrayList<>();
            for (Train t : trains) {
                int hour = parseHour(t.getDepartTime());
                boolean hasStock = hasAnySeat(t);
                boolean inRange = range == null || (hour >= range[0] && hour < range[1]);
                if (hasStock && inRange && !t.getId().equals(order.getTrainId())) {
                    filtered.add(t);
                }
            }

            StringBuilder result = new StringBuilder();
            result.append("【").append(fromCity).append(" → ").append(toCity).append("】 ").append(date);
            if (range != null) {
                result.append(" ").append(timePeriodLabel(timePeriod));
            }
            result.append(" 共").append(filtered.size()).append("趟可选车次：\n\n");

            if (filtered.isEmpty()) {
                result.append("该时段暂无余票车次。");
                if (range != null) {
                    result.append("建议您放宽时间段（如改为全天），或选择邻近日期。");
                }
                return result.toString();
            }
            for (Train t : filtered) {
                result.append(formatTrainInfo(t)).append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            return "查询可改签车次失败：" + e.getMessage();
        }
    }

    /**
     * 改签阶段3/4：计算改签方案的费用明细（原价、新价、差价、手续费、最终应付/退款）。
     * 返回格式化字符串，供确认展示。
     */
    public String getChangePlan(String orderNumber, String trainIdOrNum, Integer seatType, String date) {
        try {
            Order order = findOrder(orderNumber);
            if (order == null) return "未找到订单「" + orderNumber + "」。";

            Train newTrain = resolveTrain(trainIdOrNum);
            if (newTrain == null) return "未找到新车次「" + trainIdOrNum + "」。";

            int finalSeatType = seatType != null ? seatType : (order.getSeatType() != null ? order.getSeatType() : 2);
            int stock = getSeatStock(newTrain, finalSeatType);
            if (stock <= 0) {
                return "FAIL:新车次「" + newTrain.getTrainNumber() + "」的" + seatTypeLabel(finalSeatType) + "已无余票，请选择其他车次或席别。";
            }

            BigDecimal oldPrice = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal newPrice = getPriceBySeatType(newTrain, finalSeatType);
            BigDecimal diff = newPrice.subtract(oldPrice);

            LocalDate targetDateForFee = null;
            if (date != null && !date.isEmpty()) {
                targetDateForFee = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } else if (order.getDepartTime() != null) {
                targetDateForFee = order.getDepartTime().toLocalDate();
            }
            BigDecimal fee = calculateChangeFee(order, oldPrice, newPrice, targetDateForFee);
            BigDecimal finalAmount = diff.add(fee);

            StringBuilder sb = new StringBuilder();
            sb.append("【改签方案确认】\n\n");
            sb.append("原车票：");
            if (order.getDepartTime() != null) {
                sb.append(order.getDepartTime().format(DateTimeFormatter.ofPattern("MM月dd日 ")));
            }
            sb.append(order.getTrainNumber()).append("次 ").append(seatTypeLabel(order.getSeatType())).append("\n");
            sb.append("路线：").append(order.getFromStation()).append(" → ").append(order.getToStation()).append("\n\n");

            sb.append("拟改签至：");
            if (date != null && !date.isEmpty()) {
                sb.append(date).append(" ");
            }
            sb.append(newTrain.getTrainNumber()).append("次（").append(newTrain.getDepartTime()).append("开）\n");
            sb.append("席位：").append(seatTypeLabel(finalSeatType)).append("\n\n");

            sb.append("【费用明细】\n");
            sb.append("原票价：¥").append(oldPrice).append("\n");
            sb.append("新票价：¥").append(newPrice).append("\n");
            sb.append("票价差额：").append(diff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "").append("¥").append(diff).append("\n");
            sb.append("改签手续费：¥").append(fee).append("\n");
            if (finalAmount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("最终需支付：¥").append(finalAmount).append("\n");
            } else if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                sb.append("最终退还：¥").append(finalAmount.abs()).append("（退回钱包）\n");
            } else {
                sb.append("最终需支付：¥0（无需补退）\n");
            }
            sb.append("\n温馨提示：车票仅支持一次改签，确认后将无法再次改签。\n");
            sb.append("请回复「确认」办理改签，或回复「取消」放弃改签。");
            return sb.toString();
        } catch (Exception e) {
            return "FAIL:计算改签方案失败：" + e.getMessage();
        }
    }

    /**
     * 改签手续费：7档费率系统
     * 规则：https://www.12306.cn
     * - 费率基数 = min(原票价, 新票价)
     * - 开车前≥48h: 0%
     * - 开车前24h≤t<48h，改签至原票日期及之前: 0%
     * - 开车前24h≤t<48h，改签至原票日期之后: 5%
     * - 开车前t<24h，改签至原票日期及之前: 0%
     * - 开车前t<24h，改签至原票日期之后: 15%
     * - 开车后，改签至当日其他列车: 0%
     * - 开车后，改签至次日及以后: 40%
     */
    private BigDecimal calculateChangeFee(Order order, BigDecimal oldPrice, BigDecimal newPrice, LocalDate targetDate) {
        if (order.getDepartTime() == null || oldPrice == null) return BigDecimal.ZERO;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departTime = order.getDepartTime();
        boolean isAfterDepart = now.isAfter(departTime);
        long hoursToDepart = java.time.Duration.between(now, departTime).toHours();

        LocalDate originalDate = departTime.toLocalDate();
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

        BigDecimal fee = feeBase.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
        log.info("改签费计算: oldPrice={}, newPrice={}, targetDate={}, hoursToDepart={}, isAfterDepart={}, rate={}, fee={}",
                oldPrice, newPrice, targetDate, hoursToDepart, isAfterDepart, rate, fee);
        return fee;
    }

    /**
     * 旧版改签费（兼容）
     */
    private BigDecimal calculateChangeFee(Order order, BigDecimal oldPrice) {
        if (order.getDepartTime() == null || oldPrice == null) return BigDecimal.ZERO;
        long hours = java.time.Duration.between(java.time.LocalDateTime.now(), order.getDepartTime()).toHours();
        if (hours >= 48) return BigDecimal.ZERO;
        if (hours >= 24) return oldPrice.multiply(new BigDecimal("0.05")).setScale(2, java.math.RoundingMode.HALF_UP);
        return oldPrice.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private int[] parseTimePeriod(String period) {
        if (period == null) return null;
        String p = period.toLowerCase();
        if (p.contains("morning") || p.contains("上午") || p.contains("早上") || p.contains("早晨")) return new int[]{6, 12};
        if (p.contains("afternoon") || p.contains("下午") || p.contains("中午")) return new int[]{12, 18};
        if (p.contains("evening") || p.contains("晚上") || p.contains("晚间") || p.contains("夜间")) return new int[]{18, 24};
        return null;
    }

    private String timePeriodLabel(String period) {
        if (period == null) return "";
        if (period.contains("morning") || period.contains("上午")) return "上午";
        if (period.contains("afternoon") || period.contains("下午")) return "下午";
        if (period.contains("evening") || period.contains("晚上")) return "晚上";
        return period;
    }

    private int parseHour(String timeStr) {
        try {
            if (timeStr == null) return 0;
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean hasAnySeat(Train t) {
        return getSeatStock(t, 2) > 0 || getSeatStock(t, 1) > 0 || getSeatStock(t, 0) > 0 ||
               getSeatStock(t, 3) > 0 || getSeatStock(t, 4) > 0 || getSeatStock(t, 5) > 0;
    }

    private int getSeatStock(Train t, Integer seatType) {
        if (seatType == null || seatType == 0) return t.getBusinessSeats() != null ? t.getBusinessSeats() : 0;
        if (seatType == 1) return t.getFirstSeats() != null ? t.getFirstSeats() : 0;
        if (seatType == 2) return t.getSecondSeats() != null ? t.getSecondSeats() : 0;
        if (seatType == 3) return t.getSoftSleepers() != null ? t.getSoftSleepers() : 0;
        if (seatType == 4) return t.getHardSleepers() != null ? t.getHardSleepers() : 0;
        if (seatType == 5) return t.getHardSeats() != null ? t.getHardSeats() : 0;
        return 0;
    }

    private Train resolveTrain(String trainIdOrNum) {
        if (trainIdOrNum == null || trainIdOrNum.isEmpty()) return null;
        try {
            Long id = Long.parseLong(trainIdOrNum);
            Train t = trainMapper.selectById(id);
            if (t != null) return t;
        } catch (NumberFormatException ignored) {
        }
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Train::getTrainNumber, trainIdOrNum.toUpperCase());
        return trainMapper.selectOne(wrapper);
    }

    @Tool("获取学生票购票规则说明。返回学生票的购买条件、折扣标准、使用限制等信息。")
    public String getStudentTicketRules() {
        return """
                学生票购票规则：
                
                1. 购买条件：
                   - 在国家教育主管部门批准的普通高等学校、中等职业学校、中小学就读的学生
                   - 家庭居住地与学校所在地不在同一城市
                   - 每年仅限购买往返各一次（寒暑假期间）
                
                2. 优惠标准：
                   - 硬座：半价
                   - 硬卧：半价（指硬座部分）
                   - 动车组二等座：75折
                   - 软卧、高级软卧：不发售学生票
                
                3. 购票凭证：
                   - 凭有效学生证（加盖学校公章的火车票优惠卡）
                   - 优惠卡需经学校每学年注册
                   - 新生凭录取通知书，毕业生凭学校证明
                
                4. 使用限制：
                   - 仅限于寒暑假期间使用（每年12月1日至次年3月31日、6月1日至9月30日）
                   - 每年限用4次（往返各2次）
                   - 超过次数需购买全价票
                
                5. 购票渠道：
                   - 可在12306官网、APP购买
                   - 可在车站售票窗口购买
                   - 取票时需携带学生证
                """;
    }

    @Tool("获取儿童票购票规则说明。返回儿童票的购买条件、价格标准等信息。")
    public String getChildTicketRules() {
        return """
                儿童票购票规则：
                
                1. 购买条件：
                   - 年满6周岁且未满14周岁的儿童，应购买儿童票
                   - 年满14周岁及以上的儿童，应购买全价票
                   - 未满6周岁的儿童可免票（不单独占用席位）
                
                2. 优惠标准：
                   - 身高1.2米以下、不单独占用座位的儿童免票
                   - 身高1.2米至1.5米的儿童购买儿童票
                   - 儿童票价格为全价票的50%
                   - 动车组儿童票为二等座全价票的50%
                
                3. 购票方式：
                   - 可在12306官网、APP购买
                   - 凭儿童的有效身份证件（户口本或身份证）购票
                   - 一个成人旅客可免费携带一名未满6周岁的儿童
                
                4. 注意事项：
                   - 儿童票需与成人票一同购买
                   - 单独出行的儿童需购买全价票
                   - 身高以实际测量为准
                """;
    }

    @Tool("从铁路出行知识库中检索相关信息。参数query为用户的问题或关键词。返回知识库中与问题最相关的内容，包括乘车须知、行李规定、改签退票流程、常见问题等。")
    public String searchKnowledge(String query) {
        try {
            return knowledgeService.searchKnowledge(query);
        } catch (Exception e) {
            return "知识库检索失败：" + e.getMessage();
        }
    }

    private String formatTrainInfo(Train t) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ID:").append(t.getId()).append("] ");
        sb.append("车次 ").append(t.getTrainNumber()).append(" | ");
        sb.append(t.getDepartTime()).append("开 - ").append(t.getArriveTime()).append("到 | ");
        sb.append("历时").append(t.getDuration()).append("分钟 | ");
        sb.append(appendSeatTypes(t));
        return sb.toString();
    }

    private String formatTrainDetail(Train t) {
        StringBuilder sb = new StringBuilder();
        sb.append("【车次详情】ID:").append(t.getId()).append(" | ").append(t.getTrainNumber()).append("\n");
        sb.append("出发时间：").append(t.getDepartTime()).append("\n");
        sb.append("到达时间：").append(t.getArriveTime()).append("\n");
        sb.append("历时：").append(t.getDuration()).append("分钟\n");
        sb.append("运行日期：").append(t.getRunningDays()).append("\n\n");

        Map<String, Object> seatInfo = getSeatInfo(t);
        sb.append("座位类型及票价：\n");
        for (Map.Entry<String, Object> entry : seatInfo.entrySet()) {
            sb.append("  【").append(entry.getKey()).append("】 ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String appendSeatTypes(Train t) {
        StringBuilder sb = new StringBuilder();
        if (t.getSecondPrice() != null) {
            sb.append("二等座¥").append(t.getSecondPrice()).append("(").append(t.getSecondSeats()).append("张) ");
        }
        if (t.getFirstPrice() != null) {
            sb.append("一等座¥").append(t.getFirstPrice()).append("(").append(t.getFirstSeats()).append("张) ");
        }
        if (t.getBusinessPrice() != null) {
            sb.append("商务座¥").append(t.getBusinessPrice()).append("(").append(t.getBusinessSeats()).append("张) ");
        }
        if (t.getSoftPrice() != null) {
            sb.append("软卧¥").append(t.getSoftPrice()).append("(").append(t.getSoftSleepers()).append("张) ");
        }
        if (t.getHardPrice() != null) {
            sb.append("硬卧¥").append(t.getHardPrice()).append("(").append(t.getHardSleepers()).append("张) ");
        }
        if (t.getSeatPrice() != null) {
            sb.append("硬座¥").append(t.getSeatPrice()).append("(").append(t.getHardSeats()).append("张) ");
        }
        return sb.toString();
    }

    private Map<String, Object> getSeatInfo(Train t) {
        Map<String, Object> map = new HashMap<>();
        if (t.getBusinessPrice() != null) {
            map.put("商务座", "¥" + t.getBusinessPrice() + "，余票" + t.getBusinessSeats() + "张");
        }
        if (t.getFirstPrice() != null) {
            map.put("一等座", "¥" + t.getFirstPrice() + "，余票" + t.getFirstSeats() + "张");
        }
        if (t.getSecondPrice() != null) {
            map.put("二等座", "¥" + t.getSecondPrice() + "，余票" + t.getSecondSeats() + "张");
        }
        if (t.getSoftPrice() != null) {
            map.put("软卧", "¥" + t.getSoftPrice() + "，余票" + t.getSoftSleepers() + "张");
        }
        if (t.getHardPrice() != null) {
            map.put("硬卧", "¥" + t.getHardPrice() + "，余票" + t.getHardSleepers() + "张");
        }
        if (t.getSeatPrice() != null) {
            map.put("硬座", "¥" + t.getSeatPrice() + "，余票" + t.getHardSeats() + "张");
        }
        return map;
    }

    @Tool("为用户预订火车票。参数trainId为车次ID或车次号(如15或G1601)，seatType为座位类型(0商务座/1一等座/2二等座/3软卧/4硬卧/5硬座)，passengerName可选(乘客姓名，不填则自动选择第一个乘客)，date为出发日期(yyyy-MM-dd)。系统自动获取当前用户和乘客信息。返回订单创建结果。")
    public String bookTicket(String trainId, Integer seatType, String passengerName, String date) {
        try {
            log.info("bookTicket called: trainId={}, seatType={}, passengerName={}, date={}", trainId, seatType, passengerName, date);

            if (trainId == null || trainId.isEmpty()) return "请提供有效的车次ID或车次号。";

            Train train = null;
            try {
                Long id = Long.parseLong(trainId);
                train = trainMapper.selectById(id);
            } catch (NumberFormatException ignored) {
                LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Train::getTrainNumber, trainId.toUpperCase());
                train = trainMapper.selectOne(wrapper);
            }
            if (train == null) return "未找到对应车次「" + trainId + "」，请确认车次信息是否正确。";

            Long userId = getCurrentUserId();
            log.info("bookTicket userId={}, currentUserId={}", userId, UserContext.getUserId());
            if (userId == null) return "请先登录后再订票。";

            List<Passenger> passengers = passengerMapper.selectList(
                new LambdaQueryWrapper<Passenger>()
                    .eq(Passenger::getUserId, userId)
                    .eq(Passenger::getDeleted, 0)
            );

            if (passengers.isEmpty()) {
                return "您还没有添加乘客，请先到「个人中心-乘客管理」添加乘客信息后再订票。";
            }

            Passenger selectedPassenger = null;
            String actualPassengerName = passengerName;

            if (passengerName != null && !passengerName.isEmpty()) {
                selectedPassenger = passengers.stream()
                    .filter(p -> p.getName().equals(passengerName))
                    .findFirst()
                    .orElse(null);
                if (selectedPassenger == null) {
                    return "指定乘客不存在，请先添加或重新选择乘客。";
                }
            } else {
                if(passengers.size()>1) return "有多位常用乘客，请明确乘客姓名后再预订";
                // A unique passenger is still shown on the confirmation card.
                selectedPassenger = passengers.get(0);
                actualPassengerName = selectedPassenger.getName();
                log.info("未指定乘客，自动使用第一个乘客[{}]", actualPassengerName);
            }

            int finalSeatType = seatType != null ? seatType : 2;

            Station fromStation = stationService.getStationById(train.getFromStationId());
            Station toStation = stationService.getStationById(train.getToStationId());

            Order order = new Order();
            order.setUserId(userId);
            order.setTrainId(train.getId());
            order.setTrainNumber(train.getTrainNumber());
            order.setFromStation(fromStation != null ? fromStation.getName() : "");
            order.setToStation(toStation != null ? toStation.getName() : "");
            order.setSeatType(finalSeatType);
            order.setStatus(0);

            order.setPassengerId(selectedPassenger.getId());
            order.setPassengerName(selectedPassenger.getName());
            order.setPassengerIdCard(selectedPassenger.getIdCard());

            BigDecimal seatPrice = getPriceBySeatType(train, finalSeatType);
            order.setTotalAmount(seatPrice);

            if(date==null || date.isBlank()) return "请先明确出发日期（yyyy-MM-dd）";
            java.time.LocalDate targetDate=java.time.LocalDate.parse(date);
            java.time.LocalTime departTime=java.time.LocalTime.parse(train.getDepartTime());
            java.time.LocalTime arriveTime=java.time.LocalTime.parse(train.getArriveTime());
            order.setDepartTime(java.time.LocalDateTime.of(targetDate,departTime));
            order.setArriveTime(java.time.LocalDateTime.of(arriveTime.isBefore(departTime)?targetDate.plusDays(1):targetDate,arriveTime));
            if(!order.getDepartTime().isAfter(java.time.LocalDateTime.now())) return "请选择尚未发车的日期和车次";

            return actions.book(order);
        } catch (Exception e) {
            return "订票失败：" + e.getMessage() + "。请检查车次和乘客信息是否正确。";
        }
    }

    @Tool("查询当前用户的乘客列表。无需传参，自动获取当前登录用户的所有乘客信息（姓名、证件号、手机号等）。")
    public String listMyPassengers() {
        try {
            Long userId = getCurrentUserId();
            log.info("listMyPassengers: userId={}, currentUserId={}", userId, UserContext.getUserId());
            if (userId == null) return "请先登录后再查询乘客信息。";

            List<Passenger> passengers = passengerMapper.selectList(
                new LambdaQueryWrapper<Passenger>()
                    .eq(Passenger::getUserId, userId)
                    .eq(Passenger::getDeleted, 0)
            );

            log.info("listMyPassengers: found {} passengers for userId={}", passengers.size(), userId);
            if (passengers.isEmpty()) return "您还没有添加乘客，请先到「个人中心-乘客管理」添加乘客信息。";

            StringBuilder result = new StringBuilder();
            result.append("您的乘客列表（共").append(passengers.size()).append("人）：\n\n");
            for (Passenger p : passengers) {
                result.append("【乘客姓名】").append(p.getName()).append("\n");
                result.append("  证件号：").append(p.getIdCard()).append("\n");
                result.append("  手机号：").append(p.getPhone() != null ? p.getPhone() : "未填写").append("\n");
                result.append("  类型：").append(p.getType() == 0 ? "成人" : (p.getType() == 1 ? "学生" : "儿童")).append("\n\n");
            }
            return result.toString();
        } catch (Exception e) {
            return "查询乘客失败：" + e.getMessage();
        }
    }

    @Tool("查询当前用户的火车票订单列表，包括订单状态、车次、时间、乘客等信息。无需传参，自动获取当前登录用户。")
    public String queryUserOrders() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) return "请先登录后再查询订单。";

            orderService.autoCompletePastOrders();

            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getUserId, userId);
            wrapper.notIn(Order::getStatus, 3, 5);
            wrapper.orderByDesc(Order::getCreateTime);
            List<Order> orders = orderMapper.selectList(wrapper);

            if (orders.isEmpty()) return "您暂无火车票订单。";

            StringBuilder result = new StringBuilder();
            result.append("您的火车票订单（共").append(orders.size()).append("条）：\n\n");

            for (Order o : orders) {
                result.append(formatOrderInfo(o)).append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            return "查询订单失败：" + e.getMessage();
        }
    }

    @Tool("办理退票。参数orderNumber可选(订单号或订单ID)，如果不传则自动获取当前用户的唯一活跃订单。返回退票办理结果、退款金额等信息。")
    public String processRefund(String orderNumber) {
        try {
            Order order=resolveOrderForOperation(orderNumber,"退票");
            if(order==null) return "未找到订单";
            return actions.refund(order);
        } catch(Exception e) { return "无法准备操作："+e.getMessage(); }
    }

    /**
     * AI侧退票费计算（与OrderServiceImpl保持一致）
     */
    private BigDecimal calculateRefundFeeForOrder(Order order) {
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

        BigDecimal fee = totalAmount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);

        // 最低2元
        BigDecimal minFee = new BigDecimal("2.00");
        if (fee.compareTo(minFee) < 0 && fee.compareTo(BigDecimal.ZERO) > 0) {
            fee = minFee;
        }

        return fee;
    }

    @Tool("办理改签。参数orderNumber可选(订单号或订单ID，不传则自动获取当前用户唯一活跃订单)，newTrainId为新车次ID或车次号(如15或G1601)，seatType可选(新座位类型)，date可选(新日期yyyy-MM-dd)。返回改签办理结果。")
    public String processChange(String orderNumber, String newTrainId, Integer seatType, String date) {
        try {
            Order order=resolveOrderForOperation(orderNumber,"改签");
            Train train=resolveTrain(newTrainId);
            if(order==null || train==null) return "未找到订单或目标车次";
            return actions.change(order,train,seatType==null?order.getSeatType():seatType,date==null?order.getDepartTime().toLocalDate().toString():date);
        } catch(Exception e) { return "无法准备操作："+e.getMessage(); }
    }

    @Tool("获取出行中转推荐方案。参数fromCity为出发城市，toCity为到达城市，date为出发日期。当没有直达列车时，推荐中转方案。返回直达和中转车次信息。")
    public String getTransitRecommendations(String fromCity, String toCity, String date) {
        try {
            StringBuilder result = new StringBuilder();
            result.append("【智能出行推荐】\n\n");

            String directResult = queryTrains(fromCity, toCity, date);
            if (directResult.contains("没有查询到")) {
                result.append("从「").append(fromCity).append("」到「").append(toCity).append("没有直达列车。\n\n");
                result.append("【推荐中转方案】\n");

                List<String> majorCities = Arrays.asList("北京", "上海", "广州", "郑州", "武汉", "南京", "西安", "济南", "合肥", "南昌");
                int planCount = 0;

                for (String midCity : majorCities) {
                    if (midCity.equals(fromCity) || midCity.equals(toCity)) continue;
                    if (planCount >= 3) break;

                    String leg1 = queryTrains(fromCity, midCity, date);
                    if (leg1.contains("没有查询到")) continue;
                    String leg2 = queryTrains(midCity, toCity, date);
                    if (leg2.contains("没有查询到")) continue;

                    planCount++;
                    result.append("\n方案").append(planCount).append("：经「").append(midCity).append("」中转\n");
                    result.append("第一段：").append(fromCity).append(" → ").append(midCity).append("\n");
                    result.append(leg1).append("\n");
                    result.append("第二段：").append(midCity).append(" → ").append(toCity).append("\n");
                    result.append(leg2).append("\n");
                }

                if (planCount == 0) {
                    result.append("抱歉，暂时没有找到合适的中转方案。建议调整出行日期。");
                }
            } else {
                result.append(directResult);
                result.append("\n您可以直接选择以上车次购票。如需更多选择，也可以考虑中转出行。");
            }

            return result.toString();
        } catch (Exception e) {
            return "获取出行推荐失败：" + e.getMessage();
        }
    }

    @Tool("根据订单号或订单ID查询订单详情。参数orderNumber为订单号或订单ID（数字ID也支持）。返回订单的车次、乘客、状态等详细信息。")
    public String queryOrderDetail(String orderNumber) {
        try {
            if (orderNumber == null || orderNumber.isEmpty()) return "请提供订单号。";
            Order order = findOrder(orderNumber);
            if (order == null) return "未找到订单「" + orderNumber + "」。";
            return formatOrderInfo(order);
        } catch (Exception e) {
            return "查询订单失败：" + e.getMessage();
        }
    }

    private Order findOrder(String orderNumberOrId) {
        if (orderNumberOrId == null || orderNumberOrId.isEmpty()) return null;
        Order order = orderMapper.selectByOrderNumber(orderNumberOrId);
        if (order == null && orderNumberOrId.matches("\\d+")) {
            try {
                Long id = Long.parseLong(orderNumberOrId);
                order = orderMapper.selectById(id);
            } catch (NumberFormatException ignored) {
            }
        }
        return order;
    }

    private Order resolveOrderForOperation(String orderNumber, String operation) {
        if (orderNumber != null && !orderNumber.isEmpty()) {
            Order order = findOrder(orderNumber);
            if (order == null) {
                throw new RuntimeException("未找到订单「" + orderNumber + "」。请先查询您的订单列表。");
            }
            return order;
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("请先登录后再办理" + operation + "。");
        }

        orderService.autoCompletePastOrders();

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.eq(Order::getStatus, 1);
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            throw new RuntimeException("您暂无待出行的订单，无法办理" + operation + "。");
        }

        if (orders.size() == 1) {
            return orders.get(0);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您有多个待出行订单，请提供订单号以办理").append(operation).append("：\n\n");
        for (Order o : orders) {
            sb.append("【订单号】").append(o.getOrderNumber()).append(" | ")
              .append(o.getTrainNumber()).append(" | ")
              .append(o.getFromStation()).append("→").append(o.getToStation()).append(" | ");
            if (o.getDepartTime() != null) {
                sb.append(o.getDepartTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
            }
            sb.append("\n");
        }
        throw new RuntimeException(sb.toString());
    }

    /**
     * 获取当前用户唯一待出行订单号（用于改签自动识别）。
     * 返回：订单号；若无可改签订单返回"FAIL:..."；若多个返回"MULTI:列表"。
     */
    public String getActiveOrderForChange() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) return "FAIL:请先登录后再办理改签。";

            orderService.autoCompletePastOrders();

            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getUserId, userId);
            wrapper.eq(Order::getStatus, 1);
            wrapper.orderByDesc(Order::getCreateTime);
            List<Order> orders = orderMapper.selectList(wrapper);

            List<Order> changeable = new java.util.ArrayList<>();
            for (Order o : orders) {
                if (o.getRemark() == null || !o.getRemark().contains("已改签")) {
                    changeable.add(o);
                }
            }

            if (changeable.isEmpty()) {
                if (orders.isEmpty()) return "FAIL:您暂无可改签的待出行订单。";
                return "FAIL:您的待出行订单均已改签过，无法再次改签。可考虑退票后重新购票。";
            }
            if (changeable.size() == 1) {
                return changeable.get(0).getOrderNumber();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("您有多个待出行订单，请提供订单号：\n\n");
            for (Order o : changeable) {
                sb.append("【订单号】").append(o.getOrderNumber()).append(" | ")
                  .append(o.getTrainNumber()).append(" | ")
                  .append(o.getFromStation()).append("→").append(o.getToStation()).append(" | ");
                if (o.getDepartTime() != null) {
                    sb.append(o.getDepartTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
                }
                sb.append("\n");
            }
            return "MULTI:" + sb;
        } catch (Exception e) {
            return "FAIL:获取订单失败：" + e.getMessage();
        }
    }

    private String formatOrderInfo(Order o) {
        StringBuilder sb = new StringBuilder();
        sb.append("【订单号】").append(o.getOrderNumber()).append("\n");
        sb.append("车次：").append(o.getTrainNumber()).append("\n");
        sb.append("路线：").append(o.getFromStation()).append(" → ").append(o.getToStation()).append("\n");
        if (o.getDepartTime() != null) {
            sb.append("发车：").append(o.getDepartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        }
        if (o.getArriveTime() != null) {
            sb.append("到达：").append(o.getArriveTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        }
        sb.append("座位：").append(seatTypeLabel(o.getSeatType())).append("\n");
        sb.append("乘客：").append(o.getPassengerName() != null ? o.getPassengerName() : "未填写").append("\n");
        sb.append("金额：¥").append(o.getTotalAmount() != null ? o.getTotalAmount() : 0).append("\n");
        String statusText = orderStatusText(o.getStatus());
        if (o.getRemark() != null && o.getRemark().contains("已改签") && o.getStatus() != null && o.getStatus() == 1) {
            statusText = statusText + "（已改签）";
        }
        sb.append("状态：").append(statusText);
        return sb.toString();
    }

    private String orderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "待出行";
            case 2: return "已完成";
            case 3: return "已退票";
            case 4: return "已改签";
            case 5: return "已取消";
            default: return "未知";
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

    private BigDecimal getPriceBySeatType(Train t, Integer seatType) {
        if (seatType == null || seatType == 0) {
            return t.getBusinessPrice() != null ? t.getBusinessPrice() : BigDecimal.ZERO;
        } else if (seatType == 1) {
            return t.getFirstPrice() != null ? t.getFirstPrice() : BigDecimal.ZERO;
        } else if (seatType == 2) {
            return t.getSecondPrice() != null ? t.getSecondPrice() : BigDecimal.ZERO;
        } else if (seatType == 3) {
            return t.getSoftPrice() != null ? t.getSoftPrice() : BigDecimal.ZERO;
        } else if (seatType == 4) {
            return t.getHardPrice() != null ? t.getHardPrice() : BigDecimal.ZERO;
        } else if (seatType == 5) {
            return t.getSeatPrice() != null ? t.getSeatPrice() : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }
}