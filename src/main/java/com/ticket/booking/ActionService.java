package com.ticket.booking;
import com.ticket.entity.*;
import com.ticket.mapper.*;
import com.ticket.service.OrderService;
import com.ticket.common.context.UserContext;
import com.ticket.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
@Service
public class ActionService {
 public record Payload(Order order,String number,Long trainId,Integer seat,String date,BigDecimal quote) {}
 public record Card(String id,String type,String summary,String status,LocalDateTime expiresAt,String result) {}
 private final JdbcTemplate db; private final ObjectMapper json; private final OrderService orders; private final TrainMapper trains; private final OrderMapper orderMapper; private final InventoryService inventory;
 public ActionService(JdbcTemplate db,ObjectMapper json,OrderService orders,TrainMapper trains,OrderMapper orderMapper,InventoryService inventory) {this.db=db;this.json=json;this.orders=orders;this.trains=trains;this.orderMapper=orderMapper;this.inventory=inventory;}
 private String seatLabel(int seat) {return switch(seat){case 0->"商务座";case 1->"一等座";case 2->"二等座";case 3->"软卧";case 4->"硬卧";case 5->"硬座";default->throw new BusinessException("席别无效");};}
 private String save(String type,Payload payload,String summary) {
  Long user=UserContext.getUserId(); if(user==null) throw new BusinessException("请先登录");
  String id=UUID.randomUUID().toString();
  try {db.update("INSERT INTO ticket_action(id,user_id,action_type,payload,summary,status,expires_at) VALUES(?,?,?,?,?,'PENDING',?)",id,user,type,json.writeValueAsString(payload),summary,LocalDateTime.now().plusMinutes(5));}
  catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new IllegalStateException(e);}
  return "操作尚未执行，请点击确认卡片。编号："+id+"\n"+summary;
 }
 public String book(Order order) {
  Train t=trains.selectById(order.getTrainId()); BigDecimal quote=InventoryService.price(t,order.getSeatType());
  order.setTotalAmount(quote);
  return save("BOOK",new Payload(order,null,t.getId(),order.getSeatType(),order.getDepartTime().toLocalDate().toString(),quote),"预订 "+t.getTrainNumber()+" / "+order.getDepartTime()+" / "+order.getPassengerName()+" / "+seatLabel(order.getSeatType())+" / ¥"+quote+"；确认后锁座15分钟，需另行模拟支付");
 }
 public String refund(Order order) {
  BigDecimal quote=orders.quoteRefund(order.getOrderNumber());
  return save("REFUND",new Payload(null,order.getOrderNumber(),null,null,null,quote),"退票 "+order.getOrderNumber()+" / "+order.getTrainNumber()+"；原价 ¥"+order.getTotalAmount()+"；手续费 ¥"+order.getTotalAmount().subtract(quote)+"；模拟退回钱包 ¥"+quote);
 }
 public String change(Order order,Train train,int seat,String date) {
  BigDecimal quote=orders.quoteChange(order.getOrderNumber(),train.getId(),seat,date);
  return save("CHANGE",new Payload(null,order.getOrderNumber(),train.getId(),seat,date,quote),"改签 "+order.getOrderNumber()+" → "+train.getTrainNumber()+" / "+date+" / "+seatLabel(seat)+"；模拟补退差额 ¥"+quote+"（负数退款），确认即模拟结算");
 }
 public List<Card> list() {
  return db.query("SELECT * FROM ticket_action WHERE user_id=? ORDER BY created_at DESC LIMIT 20",(rs,n)->new Card(rs.getString("id"),rs.getString("action_type"),rs.getString("summary"),rs.getString("status").equals("PENDING")&&rs.getTimestamp("expires_at").toLocalDateTime().isBefore(LocalDateTime.now())?"EXPIRED":rs.getString("status"),rs.getTimestamp("expires_at").toLocalDateTime(),rs.getString("result")),UserContext.getUserId());
 }
 @Transactional public String confirm(String id) {
  var rows=db.queryForList("SELECT * FROM ticket_action WHERE id=? AND user_id=? FOR UPDATE",id,UserContext.getUserId());
  if(rows.isEmpty()) throw new BusinessException("确认操作不存在"); var row=rows.get(0);
  if("DONE".equals(row.get("status"))) return (String)row.get("result");
  if(!"PENDING".equals(row.get("status")) || ((java.sql.Timestamp)row.get("expires_at")).toLocalDateTime().isBefore(LocalDateTime.now())) throw new BusinessException("操作已取消或过期，请重新发起");
  try {
   Payload p=json.readValue((String)row.get("payload"),Payload.class); String type=(String)row.get("action_type");
   if(p.number()!=null) orderMapper.selectForUpdate(p.number());
   if(p.trainId()!=null) inventory.lockTrain(p.trainId());
   BigDecimal now=switch(type) {case "BOOK"->InventoryService.price(trains.selectById(p.trainId()),p.seat());case "REFUND"->orders.quoteRefund(p.number());case "CHANGE"->orders.quoteChange(p.number(),p.trainId(),p.seat(),p.date());default->throw new BusinessException("未知操作");};
   if(now.compareTo(p.quote())!=0) throw new BusinessException("价格或费用已变化，请重新发起并确认");
   String result;
   switch(type) {
    case "BOOK" -> {orders.createOrder(p.order());result="订单已创建："+p.order().getOrderNumber()+"，请在15分钟内模拟支付";}
    case "REFUND" -> {orders.refundOrder(p.number());result="退票完成，模拟退款 ¥"+now;}
    case "CHANGE" -> {orders.changeOrder(p.number(),p.trainId(),p.seat(),p.date());result="改签完成，已模拟结算差额 ¥"+now;}
    default -> throw new BusinessException("未知操作");
   }
   db.update("UPDATE ticket_action SET status='DONE',result=? WHERE id=?",result,id); return result;
  } catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new IllegalStateException(e);}
 }
 public void cancel(String id) {db.update("UPDATE ticket_action SET status='CANCELLED' WHERE id=? AND user_id=? AND status='PENDING'",id,UserContext.getUserId());}
}
