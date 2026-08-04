package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.ai.agent.TicketBookingAgent;
import com.ticket.ai.agent.TicketBookingTools;
import com.ticket.common.context.UserContext;
import com.ticket.common.exception.BusinessException;
import com.ticket.entity.AiChatRecord;
import com.ticket.mapper.AiChatRecordMapper;
import com.ticket.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private final Map<String, BookingContext> sessionContextMap = new ConcurrentHashMap<>();

    private static class BookingContext {
        String lastDate;
        String lastFromCity;
        String lastToCity;
        ChangeFlow changeFlow;
        long timestamp;
    }

    /**
     * 改签流程状态机。
     * stage: AWAIT_ORDER(待提供订单号) / AWAIT_DATE(待提供目标日期) /
     *        AWAIT_TRAIN(待选择车次) / AWAIT_CONFIRM(待确认)
     */
    private static class ChangeFlow {
        String stage;
        String orderNumber;
        String originalOrderInfo;
        String fromStation;
        String toStation;
        String targetDate;
        String timePeriod;
        Integer seatType;
        String selectedTrainRef;
        String selectedDate;
        long startedAt;
    }

    private BookingContext getContext(String sessionId, Long userId) {
        String key = sessionId + "_" + userId;
        BookingContext ctx = sessionContextMap.get(key);
        if (ctx != null && System.currentTimeMillis() - ctx.timestamp < 30 * 60 * 1000) {
            return ctx;
        }
        return null;
    }

    private BookingContext getOrCreateContext(String sessionId, Long userId) {
        String key = sessionId + "_" + userId;
        BookingContext ctx = sessionContextMap.get(key);
        if (ctx == null || System.currentTimeMillis() - ctx.timestamp >= 30 * 60 * 1000) {
            ctx = new BookingContext();
            sessionContextMap.put(key, ctx);
        }
        ctx.timestamp = System.currentTimeMillis();
        return ctx;
    }

    private void saveContext(String sessionId, Long userId, String date, String fromCity, String toCity) {
        BookingContext ctx = getOrCreateContext(sessionId, userId);
        ctx.lastDate = date;
        ctx.lastFromCity = fromCity;
        ctx.lastToCity = toCity;
    }

    private void startChangeFlow(String sessionId, Long userId, String orderNumber, String orderInfo, String fromStation, String toStation) {
        BookingContext ctx = getOrCreateContext(sessionId, userId);
        ChangeFlow flow = new ChangeFlow();
        flow.stage = "AWAIT_DATE";
        flow.orderNumber = orderNumber;
        flow.originalOrderInfo = orderInfo;
        flow.fromStation = fromStation;
        flow.toStation = toStation;
        flow.startedAt = System.currentTimeMillis();
        ctx.changeFlow = flow;
    }

    private ChangeFlow getChangeFlow(String sessionId, Long userId) {
        BookingContext ctx = getContext(sessionId, userId);
        if (ctx == null || ctx.changeFlow == null) return null;
        if (System.currentTimeMillis() - ctx.changeFlow.startedAt > 30 * 60 * 1000) {
            ctx.changeFlow = null;
            return null;
        }
        return ctx.changeFlow;
    }

    private void clearChangeFlow(String sessionId, Long userId) {
        BookingContext ctx = getContext(sessionId, userId);
        if (ctx != null) ctx.changeFlow = null;
    }

    private void clearContext(String sessionId, Long userId) {
        String key = sessionId + "_" + userId;
        sessionContextMap.remove(key);
    }

    @Autowired
    private AiChatRecordMapper aiChatRecordMapper;

    @Autowired
    private TicketBookingAgent ticketBookingAgent;

    @Autowired
    private TicketBookingTools ticketBookingTools;

    @Override
    public AiChatRecord chat(Long userId, String sessionId, String message) {
        UserContext.setUserId(userId);

        AiChatRecord userRecord = new AiChatRecord();
        userRecord.setUserId(userId);
        userRecord.setSessionId(sessionId);
        userRecord.setRole("user");
        userRecord.setContent(message);
        userRecord.setIntent(detectIntent(message));
        userRecord.setTokensUsed(countTokens(message));
        userRecord.setCreateTime(LocalDateTime.now());
        aiChatRecordMapper.insert(userRecord);

        String reply;
        String lower = message.toLowerCase();

        // 检查是否有未完成的改签流程
        boolean hasPendingFlow = false;
        BookingContext preCheckCtx = getContext(sessionId, userId);
        if (preCheckCtx != null && preCheckCtx.changeFlow != null) {
            ChangeFlow flow = preCheckCtx.changeFlow;
            if (System.currentTimeMillis() - flow.startedAt < 30 * 60 * 1000) {
                hasPendingFlow = true;
            }
        }

        if (hasPendingFlow) {
            reply = fallbackChat(message, userId, sessionId);
        } else {
            // 检查是否为知识问答类请求（只问规则/政策/流程，不操作）
            boolean isKnowledgeQuery = isKnowledgeQuery(message, lower);

            // 检查是否为明确的业务操作请求
            boolean isBusinessAction = isBusinessAction(message, lower);

            if (isKnowledgeQuery || !isBusinessAction) {
                // 知识问答或通用问题 -> 走 AI Agent
                try {
                    int memoryId = Math.abs((sessionId + userId).hashCode());
                    log.info("调用AI Agent(知识问答), sessionId={}, memoryId={}", sessionId, memoryId);
                    reply = ticketBookingAgent.chat(memoryId, message);
                    log.info("AI Agent回复: {}", reply);

                    if (isResponseEmptyOrGeneric(reply, message)) {
                        log.info("AI Agent回复无效，降级到工具模式");
                        reply = fallbackChat(message, userId, sessionId);
                    }
                } catch (Exception e) {
                    log.error("AI Agent调用失败，降级到工具模式", e);
                    reply = fallbackChat(message, userId, sessionId);
                }
            } else {
                // 明确的业务操作 -> 走 fallbackChat
                reply = fallbackChat(message, userId, sessionId);
            }
        }

        AiChatRecord aiRecord = new AiChatRecord();
        aiRecord.setUserId(userId);
        aiRecord.setSessionId(sessionId);
        aiRecord.setRole("assistant");
        aiRecord.setContent(reply);
        aiRecord.setIntent(userRecord.getIntent());
        aiRecord.setTokensUsed(countTokens(reply));
        aiRecord.setCreateTime(LocalDateTime.now());
        aiChatRecordMapper.insert(aiRecord);

        return aiRecord;
    }

    /**
     * 判断是否为知识问答类请求（用户只想了解规则/政策/流程，不涉及具体操作）
     * 关键特征：消息中没有明确的操作动词，而是询问性质
     */
    private boolean isKnowledgeQuery(String message, String lower) {
        // 知识问答关键词
        boolean hasKnowledgeKeywords = lower.contains("规则") || lower.contains("政策") ||
                lower.contains("规定") || lower.contains("流程") ||
                lower.contains("怎么") || lower.contains("如何") ||
                lower.contains("什么") || lower.contains("哪些") ||
                lower.contains("收费") || lower.contains("手续费") ||
                lower.contains("限制") || lower.contains("条件") ||
                lower.contains("优惠") || lower.contains("学生票") ||
                lower.contains("儿童票") || lower.contains("军残") ||
                lower.contains("丢失") || lower.contains("退钱");

        // 如果有明确的操作动词，则不是知识问答
        boolean hasExplicitAction = lower.contains("我要") || lower.contains("帮我") ||
                lower.contains("给我") || lower.contains("办理") ||
                lower.contains("立刻") || lower.contains("马上") ||
                lower.contains("请") && (lower.contains("退票") || lower.contains("改签") || lower.contains("订票") || lower.contains("买"));

        // "退票规则是什么" → 知识问答（有规则关键词，无操作动词）
        // "我要退票" → 业务操作（有操作动词）
        if (hasKnowledgeKeywords && !hasExplicitAction) {
            return true;
        }

        // 单独的短查询："退票规则"、"改签流程"、"学生票购买"等
        String trimmed = lower.trim();
        String[] knowledgePhrases = {"退票规则", "改签规则", "改签流程", "退票流程",
                "学生票", "儿童票", "怎么买", "怎么退", "退钱", "丢失", "军残",
                "怎么订", "怎么改", "怎么操作", "规定"};
        for (String phrase : knowledgePhrases) {
            if (trimmed.equals(phrase) || trimmed.contains(phrase)) {
                // 再次确认没有操作动词
                if (!lower.contains("我要") && !lower.contains("帮我") && !lower.contains("办理")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 判断是否为明确的业务操作请求（用户明确要执行订票/退票/改签等操作）
     */
    private boolean isBusinessAction(String message, String lower) {
        // 有明确的操作意图
        boolean hasActionVerb = lower.contains("我要") || lower.contains("帮我") ||
                lower.contains("给我") || lower.contains("办理") ||
                lower.contains("订票") || lower.contains("买票") ||
                lower.contains("购买") || lower.contains("退票") ||
                lower.contains("改签") || lower.contains("改期") ||
                lower.contains("退款") || lower.contains("退订") ||
                lower.contains("确认") || lower.contains("确定");

        boolean hasSpecificTarget = lower.contains("车次") || lower.contains("订单") ||
                lower.contains("车票") || lower.contains("高铁") ||
                lower.contains("火车") || lower.contains("票价") ||
                lower.contains("多少钱") || lower.contains("价格") ||
                lower.contains("北京") || lower.contains("上海") ||
                lower.contains("广州") || lower.contains("深圳") ||
                lower.contains("杭州") || lower.contains("成都") ||
                // 更多城市
                lower.contains("到") || lower.contains("→") || lower.contains("->");

        // 有操作动词 + 具体目标 → 明确业务操作
        // 或者消息很短但包含操作动词
        if (hasActionVerb) {
            // "我要退票" → 是业务操作
            // "退票规则" → 已被 isKnowledgeQuery 先拦截
            return true;
        }

        return false;
    }

    private boolean isResponseEmptyOrGeneric(String reply, String originalMessage) {
        if (reply == null || reply.trim().isEmpty()) return true;

        String lower = originalMessage.toLowerCase();
        boolean hasRoute = lower.contains("到") || lower.contains("→") || lower.contains("->");
        boolean hasTrainQuery = lower.contains("查") || lower.contains("查询") || lower.contains("高铁") ||
                lower.contains("火车") || lower.contains("车票") || lower.contains("班次");
        boolean hasOrder = lower.contains("订单") || lower.contains("订票");
        boolean hasChange = lower.contains("改签") || lower.contains("改期");
        boolean hasRefund = lower.contains("退票") || lower.contains("退款");
        boolean hasPrice = lower.contains("票价") || lower.contains("多少钱") || lower.contains("价格");

        if (hasRoute || hasTrainQuery || hasOrder || hasChange || hasRefund || hasPrice) {
            String replyLower = reply.toLowerCase();
            boolean hasTrainInfo = reply.contains("车次") || reply.contains("趟车") || reply.matches(".*[GDC]\\d{2,4}.*");
            boolean hasOrderInfo = reply.contains("订单号") || reply.contains("订单") || reply.contains("改签成功") || reply.contains("退票成功");
            boolean hasError = replyLower.contains("无法识别") || replyLower.contains("重新描述") ||
                    replyLower.contains("暂时无法") || replyLower.contains("繁忙") || replyLower.contains("无法回答");

            if (hasError) return true;
            if (!hasTrainInfo && !hasOrderInfo) return true;
        }
        return false;
    }

    private String fallbackChat(String message, Long userId, String sessionId) {
        StringBuilder toolResults = new StringBuilder();
        String lower = message.toLowerCase();

        try {
            boolean hasChange = lower.contains("改签") || lower.contains("改期");
            boolean hasRefund = lower.contains("退票") || lower.contains("退款");
            boolean hasOrderQuery = lower.contains("我的订单") || lower.contains("订单") ||
                    lower.contains("订票记录") || lower.contains("我的订票");
            boolean hasBook = lower.contains("订票") || lower.contains("买票") ||
                    lower.contains("购买") || lower.contains("订购") || lower.contains("预订") ||
                    lower.contains("订");
            boolean hasTrainQuery = lower.contains("查") || lower.contains("查询") || lower.contains("高铁") ||
                    lower.contains("火车") || lower.contains("车票") || lower.contains("班次");

            String trainNum = extractTrainNumber(message);
            String trainId = extractTrainId(message);
            boolean hasTrainRef = (trainNum != null || trainId != null);
            String effectiveTrainId = trainNum != null ? trainNum : trainId;

            BookingContext ctx = getContext(sessionId, userId);

            ChangeFlow flow = getChangeFlow(sessionId, userId);
            if (flow != null) {
                boolean isConfirm = lower.contains("确认") || lower.contains("确定") || lower.equals("是") ||
                        lower.contains("好的") || lower.contains("可以") || lower.equals("行") ||
                        lower.equals("yes") || lower.equals("y") || lower.contains("继续");
                boolean isCancel = lower.contains("取消") || lower.contains("不了") ||
                        lower.contains("算了") || lower.contains("放弃") || lower.contains("终止") ||
                        lower.contains("不改了") || lower.equals("no");
                boolean isRulesQuery = lower.contains("规则") || lower.contains("手续费") ||
                        lower.contains("限制") || lower.contains("怎么收费") || lower.contains("改签费");

                if (isCancel) {
                    clearChangeFlow(sessionId, userId);
                    toolResults.append("已取消改签操作，已释放车票锁定。如需改签请重新告诉我。");
                    return toolResults.toString();
                }
                if (isRulesQuery) {
                    toolResults.append(ticketBookingTools.getChangeRules());
                    toolResults.append("\n\n（改签流程仍在进行中，请继续告诉我您的选择）");
                    return toolResults.toString();
                }

                switch (flow.stage) {
                    case "AWAIT_ORDER": {
                        String orderNum = extractOrderNumber(message);
                        if (orderNum == null) {
                            toolResults.append("请提供您要改签的订单号，例如：改签订单TKxxxxxxxx。");
                            return toolResults.toString();
                        }
                        String validation = ticketBookingTools.validateOrderForChange(orderNum);
                        if (validation.startsWith("FAIL:")) {
                            toolResults.append(validation.substring(5)).append("\n\n如需调整行程，可考虑退票后重新购票。");
                            clearChangeFlow(sessionId, userId);
                            return toolResults.toString();
                        }
                        String orderInfo = validation.substring(3);
                        String fromStation = extractCityFromOrder(orderInfo, "from");
                        String toStation = extractCityFromOrder(orderInfo, "to");
                        startChangeFlow(sessionId, userId, orderNum, orderInfo, fromStation, toStation);
                        toolResults.append("已获取您的原订单信息：\n").append(orderInfo).append("\n\n");
                        toolResults.append("改签时出发站和到达站不可更改（").append(fromStation).append(" → ").append(toStation).append("）。\n");
                        toolResults.append("请告诉我您想改到哪一天，以及大致时段（上午/下午/晚上）？");
                        return toolResults.toString();
                    }
                    case "AWAIT_DATE": {
                        String date = extractDate(message);
                        String timePeriod = extractTimePeriod(message);
                        Integer seatType = extractSeatType(message);
                        if (date == null) {
                            toolResults.append("请告诉我您想改到的日期，例如：改到8月10日早上。");
                            return toolResults.toString();
                        }
                        flow.targetDate = date;
                        flow.timePeriod = timePeriod;
                        if (seatType != null) flow.seatType = seatType;

                        String trainList = ticketBookingTools.searchChangeTrains(flow.orderNumber, date, timePeriod);
                        toolResults.append("为您查询到").append(date);
                        if (timePeriod != null) toolResults.append(timePeriodLabel(timePeriod));
                        toolResults.append("可改签的车次：\n\n").append(trainList).append("\n\n");
                        if (trainList.contains("暂无余票") || trainList.contains("共0趟")) {
                            toolResults.append("建议您放宽时间段（如改为全天），或选择邻近日期，请重新告诉我。");
                            return toolResults.toString();
                        }
                        flow.stage = "AWAIT_TRAIN";
                        toolResults.append("请选择要改签的车次，告诉我车次号（如G25）或车次ID。");
                        return toolResults.toString();
                    }
                    case "AWAIT_TRAIN": {
                        String trainRef = trainNum != null ? trainNum : trainId;
                        if (trainRef == null) {
                            String date = extractDate(message);
                            String timePeriod = extractTimePeriod(message);
                            if (date != null || timePeriod != null) {
                                if (date != null) flow.targetDate = date;
                                if (timePeriod != null) flow.timePeriod = timePeriod;
                                String trainList = ticketBookingTools.searchChangeTrains(flow.orderNumber, flow.targetDate, flow.timePeriod);
                                toolResults.append("已为您重新查询").append(flow.targetDate);
                                if (flow.timePeriod != null) toolResults.append(timePeriodLabel(flow.timePeriod));
                                toolResults.append("的车次：\n\n").append(trainList).append("\n\n请选择车次号或车次ID。");
                                return toolResults.toString();
                            }
                            toolResults.append("请选择要改签的车次，告诉我车次号（如G25）或车次ID。");
                            return toolResults.toString();
                        }
                        String plan = ticketBookingTools.getChangePlan(flow.orderNumber, trainRef, flow.seatType, flow.targetDate);
                        if (plan.startsWith("FAIL:")) {
                            toolResults.append(plan.substring(5)).append("\n\n请选择其他车次或席别。");
                            return toolResults.toString();
                        }
                        flow.selectedTrainRef = trainRef;
                        flow.selectedDate = flow.targetDate;
                        flow.stage = "AWAIT_CONFIRM";
                        toolResults.append(plan);
                        return toolResults.toString();
                    }
                    case "AWAIT_CONFIRM": {
                        if (isConfirm) {
                            String pTrain = flow.selectedTrainRef;
                            String pOrder = flow.orderNumber;
                            Integer pSeat = flow.seatType;
                            String pDate = flow.selectedDate;
                            clearChangeFlow(sessionId, userId);
                            String result = ticketBookingTools.processChange(pOrder, pTrain, pSeat, pDate);
                            toolResults.append(result);
                            return toolResults.toString();
                        }
                        String plan = ticketBookingTools.getChangePlan(flow.orderNumber, flow.selectedTrainRef, flow.seatType, flow.selectedDate);
                        if (plan.startsWith("FAIL:")) {
                            toolResults.append("改签方案已失效，请重新选择车次。");
                            flow.stage = "AWAIT_TRAIN";
                            return toolResults.toString();
                        }
                        toolResults.append("请先确认改签方案：\n\n").append(plan);
                        return toolResults.toString();
                    }
                    default:
                        clearChangeFlow(sessionId, userId);
                        break;
                }
            }

            } else if (hasChange) {
                // 检查用户是要改签还是只是问改签规则
                boolean isChangeAction = lower.contains("我要") || lower.contains("帮我") ||
                        lower.contains("给我") || lower.contains("办理") ||
                        lower.contains("立刻") || lower.contains("马上");

                boolean isChangeRuleQuery = lower.contains("规则") || lower.contains("政策") ||
                        lower.contains("规定") || lower.contains("流程") ||
                        lower.contains("怎么") || lower.contains("如何") ||
                        lower.contains("什么") || lower.contains("哪些") ||
                        lower.contains("收费") || lower.contains("手续费") ||
                        lower.contains("限制") || lower.contains("条件") ||
                        lower.contains("多少");

                if (isChangeRuleQuery && !isChangeAction) {
                    // 用户问改签规则，只返回规则说明
                    toolResults.append(ticketBookingTools.getChangeRules());
                    return toolResults.toString();
                }

                String orderNum = extractOrderNumber(message);
                String date = extractDate(message);
                String timePeriod = extractTimePeriod(message);
                Integer seatType = extractSeatType(message);

                if (orderNum != null) {
                    String validation = ticketBookingTools.validateOrderForChange(orderNum);
                    if (validation.startsWith("FAIL:")) {
                        toolResults.append(validation.substring(5));
                        if (validation.contains("已改签")) {
                            toolResults.append("\n\n如需调整行程，可考虑退票后重新购票。");
                        }
                        return toolResults.toString();
                    }
                    String orderInfo = validation.substring(3);
                    String fromStation = extractCityFromOrder(orderInfo, "from");
                    String toStation = extractCityFromOrder(orderInfo, "to");
                    startChangeFlow(sessionId, userId, orderNum, orderInfo, fromStation, toStation);
                    ChangeFlow f = getChangeFlow(sessionId, userId);
                    if (date != null) f.targetDate = date;
                    if (timePeriod != null) f.timePeriod = timePeriod;
                    if (seatType != null) f.seatType = seatType;

                    if (date != null) {
                        String trainList = ticketBookingTools.searchChangeTrains(orderNum, date, timePeriod);
                        toolResults.append("已获取您的原订单信息：\n").append(orderInfo).append("\n\n");
                        toolResults.append("为您查询到").append(date);
                        if (timePeriod != null) toolResults.append(timePeriodLabel(timePeriod));
                        toolResults.append("可改签的车次：\n\n").append(trainList).append("\n\n");
                        if (trainList.contains("暂无余票") || trainList.contains("共0趟")) {
                            toolResults.append("建议放宽时间段或选择邻近日期，请重新告诉我。");
                            f.stage = "AWAIT_DATE";
                            return toolResults.toString();
                        }
                        f.stage = "AWAIT_TRAIN";
                        toolResults.append("请选择要改签的车次，告诉我车次号（如G25）或车次ID。");
                        return toolResults.toString();
                    }
                    toolResults.append("已获取您的原订单信息：\n").append(orderInfo).append("\n\n");
                    toolResults.append("改签时出发站和到达站不可更改（").append(fromStation).append(" → ").append(toStation).append("）。\n");
                    toolResults.append("请告诉我您想改到哪一天，以及大致时段（上午/下午/晚上）？");
                    return toolResults.toString();
                } else {
                    String autoOrder = ticketBookingTools.getActiveOrderForChange();
                    if (autoOrder != null && !autoOrder.startsWith("FAIL") && !autoOrder.startsWith("MULTI")) {
                        String validation = ticketBookingTools.validateOrderForChange(autoOrder);
                        if (validation.startsWith("OK:")) {
                            String orderInfo = validation.substring(3);
                            String fromStation = extractCityFromOrder(orderInfo, "from");
                            String toStation = extractCityFromOrder(orderInfo, "to");
                            startChangeFlow(sessionId, userId, autoOrder, orderInfo, fromStation, toStation);
                            ChangeFlow f = getChangeFlow(sessionId, userId);
                            if (date != null) f.targetDate = date;
                            if (timePeriod != null) f.timePeriod = timePeriod;
                            if (seatType != null) f.seatType = seatType;
                            toolResults.append("已获取您的原订单信息：\n").append(orderInfo).append("\n\n");
                            if (date != null) {
                                String trainList = ticketBookingTools.searchChangeTrains(autoOrder, date, timePeriod);
                                toolResults.append("为您查询到").append(date);
                                if (timePeriod != null) toolResults.append(timePeriodLabel(timePeriod));
                                toolResults.append("可改签的车次：\n\n").append(trainList).append("\n\n");
                                if (trainList.contains("暂无余票") || trainList.contains("共0趟")) {
                                    toolResults.append("建议放宽时间段或选择邻近日期，请重新告诉我。");
                                    f.stage = "AWAIT_DATE";
                                    return toolResults.toString();
                                }
                                f.stage = "AWAIT_TRAIN";
                                toolResults.append("请选择要改签的车次，告诉我车次号（如G25）或车次ID。");
                                return toolResults.toString();
                            }
                            toolResults.append("改签时出发站和到达站不可更改（").append(fromStation).append(" → ").append(toStation).append("）。\n");
                            toolResults.append("请告诉我您想改到哪一天，以及大致时段（上午/下午/晚上）？");
                            return toolResults.toString();
                        }
                    }
                    toolResults.append("请提供您要改签的订单号，例如：改签订单TKxxxxxxxx。");
                    if (autoOrder != null && autoOrder.startsWith("MULTI:")) {
                        toolResults.append("\n\n").append(autoOrder.substring(6));
                    }
                    ChangeFlow f = new ChangeFlow();
                    f.stage = "AWAIT_ORDER";
                    f.startedAt = System.currentTimeMillis();
                    BookingContext bc = getOrCreateContext(sessionId, userId);
                    bc.changeFlow = f;
                    return toolResults.toString();
                }
            } else if (hasRefund) {
                // 检查用户是要退票还是只是问退票规则
                boolean isRefundAction = lower.contains("我要") || lower.contains("帮我") ||
                        lower.contains("给我") || lower.contains("办理") ||
                        lower.contains("立刻") || lower.contains("马上") ||
                        lower.contains("确定") || lower.contains("确认");

                boolean isRefundRuleQuery = lower.contains("规则") || lower.contains("政策") ||
                        lower.contains("规定") || lower.contains("流程") ||
                        lower.contains("怎么") || lower.contains("如何") ||
                        lower.contains("什么") || lower.contains("哪些") ||
                        lower.contains("收费") || lower.contains("手续费") ||
                        lower.contains("限制") || lower.contains("条件") ||
                        lower.contains("多少");

                if (isRefundRuleQuery && !isRefundAction) {
                    // 用户问退票规则，只返回规则说明，不调用退票
                    toolResults.append(ticketBookingTools.getRefundRules());
                } else if (isRefundAction) {
                    // 用户明确要退票
                    String orderNum = extractOrderNumber(message);
                    if (orderNum != null) {
                        toolResults.append(ticketBookingTools.processRefund(orderNum));
                    } else {
                        toolResults.append(ticketBookingTools.processRefund(null));
                    }
                } else {
                    // 模糊意图，默认返回规则（安全优先）
                    toolResults.append(ticketBookingTools.getRefundRules());
                    toolResults.append("\n\n如需办理退票，请回复「我要退票」。");
                }
            } else if (hasOrderQuery) {
                toolResults.append(ticketBookingTools.queryUserOrders());
            } else if (hasBook && hasTrainRef) {
                String date = extractDate(message);
                if (date == null && ctx != null && ctx.lastDate != null) {
                    date = ctx.lastDate;
                }
                Integer seatType = extractSeatType(message);
                String passengerName = extractPassengerName(message);
                String result = ticketBookingTools.bookTicket(effectiveTrainId, seatType, passengerName, date);
                toolResults.append(result);
                if (result.contains("订票成功")) {
                    clearContext(sessionId, userId);
                }
            } else if (hasTrainRef) {
                String date = extractDate(message);
                if (date == null && ctx != null && ctx.lastDate != null) {
                    date = ctx.lastDate;
                }
                Integer seatType = extractSeatType(message);
                String passengerName = extractPassengerName(message);
                String result = ticketBookingTools.bookTicket(effectiveTrainId, seatType, passengerName, date);
                toolResults.append(result);
                if (result.contains("订票成功")) {
                    clearContext(sessionId, userId);
                }
            } else if (hasBook) {
                String fromCity = extractCity(message, "from");
                String toCity = extractCity(message, "to");
                String date = extractDate(message);
                if (fromCity != null && toCity != null) {
                    if (date == null && ctx != null && ctx.lastDate != null) {
                        date = ctx.lastDate;
                    }
                    toolResults.append(ticketBookingTools.queryTrains(fromCity, toCity, date != null ? date : ""));
                    saveContext(sessionId, userId, date, fromCity, toCity);
                    toolResults.append("\n\n您可以选择以上车次订票，告诉我车次号或车次ID即可。");
                } else {
                    if (ctx != null && ctx.lastFromCity != null && ctx.lastToCity != null) {
                        fromCity = ctx.lastFromCity;
                        toCity = ctx.lastToCity;
                        if (date == null) date = ctx.lastDate;
                        toolResults.append(ticketBookingTools.queryTrains(fromCity, toCity, date != null ? date : ""));
                        saveContext(sessionId, userId, date, fromCity, toCity);
                        toolResults.append("\n\n您可以选择以上车次订票，告诉我车次号或车次ID即可。");
                    } else {
                        toolResults.append("请告诉我出发城市和到达城市，例如：北京到上海");
                    }
                }
            } else if (hasTrainQuery) {
                String fromCity = extractCity(message, "from");
                String toCity = extractCity(message, "to");
                String date = extractDate(message);
                if (fromCity != null && toCity != null) {
                    toolResults.append(ticketBookingTools.queryTrains(fromCity, toCity, date != null ? date : ""));
                    saveContext(sessionId, userId, date, fromCity, toCity);
                } else {
                    toolResults.append("请告诉我出发城市和到达城市，例如：北京到上海");
                }
            } else if (lower.contains("学生") && (lower.contains("票") || lower.contains("优惠"))) {
                toolResults.append(ticketBookingTools.getStudentTicketRules());
            } else if (lower.contains("儿童") && (lower.contains("票") || lower.contains("优惠"))) {
                toolResults.append(ticketBookingTools.getChildTicketRules());
            } else {
                toolResults.append(ticketBookingTools.searchKnowledge(message));
                if (toolResults.length() == 0) {
                    toolResults.append("抱歉，我暂时无法回答您的问题。您可以咨询人工客服获取帮助。");
                }
            }
        } catch (Exception e) {
            log.error("工具调用失败", e);
            toolResults.append("抱歉，系统暂时繁忙，请稍后再试。");
        }

        return toolResults.toString();
    }

    private String extractOrderNumber(String message) {
        Pattern p = Pattern.compile("T[A-Z0-9]{10,}", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(message);
        if (m.find()) {
            return m.group(0);
        }
        Pattern p2 = Pattern.compile("订单号[为是\\s]*([A-Z0-9]{8,})");
        Matcher m2 = p2.matcher(message);
        if (m2.find()) {
            return m2.group(1);
        }
        return null;
    }

    private String extractTrainNumber(String message) {
        Pattern p = Pattern.compile("[GDC]\\d{2,5}", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(message);
        if (m.find()) {
            return m.group(0).toUpperCase();
        }
        return null;
    }

    private String extractTrainId(String message) {
        if (message == null) return null;

        Pattern p = Pattern.compile("(?<![A-Za-z\\d])(\\d{1,3})(?![A-Za-z\\d])");
        Matcher m = p.matcher(message);
        while (m.find()) {
            String num = m.group(1);
            int start = m.start();
            int end = m.end();

            boolean partOfDate = false;

            if (end < message.length() && message.charAt(end) == '.') {
                partOfDate = true;
            }
            if (end < message.length() && message.charAt(end) == '月') {
                partOfDate = true;
            }
            if (start > 0 && message.charAt(start - 1) == '月') {
                partOfDate = true;
            }
            if (start > 0 && message.charAt(start - 1) == '(' && end < message.length() && message.charAt(end) == ')') {
                partOfDate = true;
            }

            if (!partOfDate) {
                return num;
            }
        }
        return null;
    }

    private Integer extractSeatType(String message) {
        if (message == null) return null;
        String lower = message.toLowerCase();
        if (lower.contains("商务座")) return 0;
        if (lower.contains("一等座") || lower.contains("一等")) return 1;
        if (lower.contains("二等座") || lower.contains("二等")) return 2;
        if (lower.contains("软卧")) return 3;
        if (lower.contains("硬卧")) return 4;
        if (lower.contains("硬座")) return 5;
        return null;
    }

    private String extractPassengerName(String message) {
        if (message == null) return null;
        Pattern p = Pattern.compile("乘客[为是：:]*([\\u4e00-\\u9fa5]{2,4})");
        Matcher m = p.matcher(message);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String extractFirstTrainId(String trainList) {
        if (trainList == null) return null;
        Pattern p = Pattern.compile("\\[ID:(\\d+)\\]");
        Matcher m = p.matcher(trainList);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String extractCityFromOrder(String orderInfo, String type) {
        if (orderInfo == null) return null;
        Pattern p;
        if ("from".equals(type)) {
            p = Pattern.compile("出发站[：:][\\s]*([\\u4e00-\\u9fa5A-Za-z]+)");
        } else {
            p = Pattern.compile("到达站[：:][\\s]*([\\u4e00-\\u9fa5A-Za-z]+)");
        }
        Matcher m = p.matcher(orderInfo);
        if (m.find()) {
            return m.group(1).trim();
        }
        if ("from".equals(type)) {
            p = Pattern.compile("([\\u4e00-\\u9fa5]{2,5})\\s*→\\s*[\\u4e00-\\u9fa5]{2,5}");
        } else {
            p = Pattern.compile("[\\u4e00-\\u9fa5]{2,5}\\s*→\\s*([\\u4e00-\\u9fa5]{2,5})");
        }
        m = p.matcher(orderInfo);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    @Override
    public List<AiChatRecord> listBySessionId(String sessionId) {
        return aiChatRecordMapper.selectBySessionId(sessionId);
    }

    @Override
    public List<AiChatRecord> listByUserId(Long userId) {
        LambdaQueryWrapper<AiChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatRecord::getUserId, userId);
        wrapper.orderByDesc(AiChatRecord::getCreateTime);
        return aiChatRecordMapper.selectList(wrapper);
    }

    @Override
    public void deleteRecord(Long id) {
        AiChatRecord record = aiChatRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("对话记录不存在");
        }
        aiChatRecordMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getTopIntents() {
        return aiChatRecordMapper.selectTopIntents(10);
    }

    @Override
    public Page<AiChatRecord> listAllSessions(int page, int size) {
        Page<AiChatRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AiChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AiChatRecord::getCreateTime);
        return aiChatRecordMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 简单意图检测（仅用于记录统计，不决定回复逻辑）
     */
    private String detectIntent(String message) {
        if (message == null) {
            return "unknown";
        }
        String lower = message.toLowerCase();
        if (lower.contains("订票") || lower.contains("买票") || lower.contains("购买") || lower.contains("订")) {
            return "ticket_booking";
        }
        if (lower.contains("改签") || lower.contains("改期")) {
            return "change_ticket";
        }
        if (lower.contains("退票") || lower.contains("退款") || lower.contains("退订")) {
            return "refund";
        }
        if (lower.contains("查询") || lower.contains("查") || lower.contains("搜索") || lower.contains("班次") || lower.contains("高铁")) {
            return "query";
        }
        if (lower.contains("票价") || lower.contains("价格") || lower.contains("多少钱") || lower.contains("费用")) {
            return "price_query";
        }
        if (lower.contains("学生") || lower.contains("儿童") || lower.contains("优惠")) {
            return "ticket_type";
        }
        if (lower.contains("规则") || lower.contains("规定") || lower.contains("怎么")) {
            return "rule_query";
        }
        if (lower.contains("登录") || lower.contains("注册") || lower.contains("账号")) {
            return "account";
        }
        return "general";
    }

    private Integer countTokens(String text) {
        if (text == null) {
            return 0;
        }
        return text.length() / 2;
    }

    private String extractCity(String message, String type) {
        String[] cityKeywords;
        if ("from".equals(type)) {
            cityKeywords = new String[]{"从", "自", "由", "出发", "始发", "订", "买", "查", "查询", "预订", "购票", "买张", "订张", "张", "一张"};
        } else {
            cityKeywords = new String[]{"到", "去", "往", "到达", "抵达", "终点", "去", "回"};
        }

        String[] cities = {"杭州东", "上海虹桥", "上海南", "北京南", "北京西", "广州南", "深圳北",
            "成都东", "重庆北", "武汉东", "西安北", "南京南", "郑州东", "长沙南",
            "合肥南", "福州南", "厦门北", "青岛北", "济南西", "天津西", "沈阳北",
            "哈尔滨西", "长春西", "石家庄北", "太原南", "南昌西", "昆明南", "贵阳北",
            "南宁东", "兰州西", "银川东", "乌鲁木齐南", "拉萨南", "海口东",
            "无锡东", "苏州北", "宁波南", "温州南", "佛山西", "东莞南",
            "北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "重庆",
            "武汉", "西安", "郑州", "长沙", "合肥", "福州", "厦门", "青岛", "济南",
            "天津", "沈阳", "大连", "哈尔滨", "长春", "石家庄", "太原", "南昌",
            "昆明", "贵阳", "南宁", "兰州", "银川", "乌鲁木齐", "拉萨", "海口",
            "三亚", "苏州", "无锡", "宁波", "温州", "佛山", "东莞", "珠海"};

        for (String keyword : cityKeywords) {
            int idx = message.indexOf(keyword);
            if (idx >= 0) {
                String after = message.substring(idx + keyword.length());
                for (String city : cities) {
                    if (after.startsWith(city)) {
                        return city;
                    }
                }
            }
        }

        if ("from".equals(type)) {
            Pattern p = Pattern.compile("([\\u4e00-\\u9fa5]{2,5})\\s*(?:到|去|往|-|→)");
            Matcher m = p.matcher(message);
            if (m.find()) {
                return m.group(1);
            }
        } else {
            Pattern p = Pattern.compile("(?:到|去|往|-|→)\\s*([\\u4e00-\\u9fa5]{2,5})");
            Matcher m = p.matcher(message);
            if (m.find()) {
                return m.group(1);
            }
        }

        return null;
    }

    private String extractDate(String message) {
        Pattern p1 = Pattern.compile("(\\d{4})[年-](\\d{1,2})[月-](\\d{1,2})日?");
        Matcher m1 = p1.matcher(message);
        if (m1.find()) {
            return String.format("%s-%02d-%02d",
                Integer.parseInt(m1.group(1)),
                Integer.parseInt(m1.group(2)),
                Integer.parseInt(m1.group(3)));
        }

        Pattern p2 = Pattern.compile("(\\d{1,2})[月-](\\d{1,2})日?");
        Matcher m2 = p2.matcher(message);
        if (m2.find()) {
            int month = Integer.parseInt(m2.group(1));
            int day = Integer.parseInt(m2.group(2));
            return String.format("%d-%02d-%02d", LocalDateTime.now().getYear(), month, day);
        }

        if (message.contains("明天")) {
            return LocalDateTime.now().plusDays(1).toLocalDate().toString();
        }
        if (message.contains("后天")) {
            return LocalDateTime.now().plusDays(2).toLocalDate().toString();
        }
        if (message.contains("今天") || message.contains("今日")) {
            return LocalDateTime.now().toLocalDate().toString();
        }

        return null;
    }

    /**
     * 从消息中提取时段：上午/下午/晚上。
     */
    private String extractTimePeriod(String message) {
        if (message == null) return null;
        String m = message.toLowerCase();
        if (m.contains("上午") || m.contains("早上") || m.contains("早晨") || m.contains("早班") || m.contains("morning")) return "上午";
        if (m.contains("下午") || m.contains("中午") || m.contains("午后") || m.contains("afternoon")) return "下午";
        if (m.contains("晚上") || m.contains("晚间") || m.contains("夜间") || m.contains("晚班") || m.contains("evening")) return "晚上";
        return null;
    }

    private String timePeriodLabel(String period) {
        if (period == null) return "";
        return period;
    }
}
