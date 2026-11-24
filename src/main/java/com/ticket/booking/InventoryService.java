package com.ticket.booking;
import com.ticket.entity.*;
import com.ticket.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
@Service
public class InventoryService {
 private final JdbcTemplate db;
 public InventoryService(JdbcTemplate db, BookingSchema schema) { this.db=db; }
 public static int capacity(Train t,int seat) {
  Integer n=switch(seat) {case 0->t.getBusinessSeats();case 1->t.getFirstSeats();case 2->t.getSecondSeats();case 3->t.getSoftSleepers();case 4->t.getHardSleepers();case 5->t.getHardSeats();default->throw new BusinessException("席别无效");};
  return n==null?0:n;
 }
 public static BigDecimal price(Train t,int seat) {
  BigDecimal n=switch(seat) {case 0->t.getBusinessPrice();case 1->t.getFirstPrice();case 2->t.getSecondPrice();case 3->t.getSoftPrice();case 4->t.getHardPrice();case 5->t.getSeatPrice();default->throw new BusinessException("席别无效");};
  if(n==null||n.signum()<=0) throw new BusinessException("该席别不可售"); return n;
 }
 public void lockTrain(long id) { if(db.queryForList("SELECT id FROM train WHERE id=? AND deleted=0 FOR UPDATE",id).isEmpty()) throw new BusinessException("车次不存在"); }
 private void initialize(Train t,LocalDate date,int seat) {
  lockTrain(t.getId());
  if(!db.queryForList("SELECT available FROM ticket_inventory WHERE train_id=? AND travel_date=? AND seat_type=? FOR UPDATE",t.getId(),date,seat).isEmpty()) return;
  int used=db.queryForObject("SELECT COUNT(*) FROM `order` WHERE train_id=? AND depart_time>=? AND depart_time<? AND seat_type=? AND status IN (0,1,2) AND deleted=0",Integer.class,t.getId(),date.atStartOfDay(),date.plusDays(1).atStartOfDay(),seat);
  int capacity=capacity(t,seat);
  db.update("INSERT INTO ticket_inventory(train_id,travel_date,seat_type,capacity,available) VALUES(?,?,?,?,?)",t.getId(),date,seat,capacity,capacity-used);
 }
 public void reserve(Train t,LocalDate date,int seat) {
  initialize(t,date,seat);
  if(db.update("UPDATE ticket_inventory SET available=available-1 WHERE train_id=? AND travel_date=? AND seat_type=? AND available>0",t.getId(),date,seat)!=1) throw new BusinessException("余票不足，请重新选择车次");
 }
 public void release(Train t,LocalDate date,int seat) {
  initialize(t,date,seat);
  db.update("UPDATE ticket_inventory SET available=LEAST(capacity,available+1) WHERE train_id=? AND travel_date=? AND seat_type=?",t.getId(),date,seat);
 }
 public int available(Train t,LocalDate date,int seat) {
  var rows=db.queryForList("SELECT available FROM ticket_inventory WHERE train_id=? AND travel_date=? AND seat_type=?",Integer.class,t.getId(),date,seat);
  if(!rows.isEmpty()) return Math.max(0,rows.get(0));
  int used=db.queryForObject("SELECT COUNT(*) FROM `order` WHERE train_id=? AND depart_time>=? AND depart_time<? AND seat_type=? AND status IN (0,1,2) AND deleted=0",Integer.class,t.getId(),date.atStartOfDay(),date.plusDays(1).atStartOfDay(),seat);
  return Math.max(0,capacity(t,seat)-used);
 }
 public void event(String order,String type,BigDecimal amount,String detail) { db.update("INSERT INTO ticket_event(order_number,event_type,amount,detail) VALUES(?,?,?,?)",order,type,amount,detail); }
 public java.util.List<java.util.Map<String,Object>> events(String order) { return db.queryForList("SELECT event_type,amount,detail,created_at FROM ticket_event WHERE order_number=? ORDER BY id",order); }
}
