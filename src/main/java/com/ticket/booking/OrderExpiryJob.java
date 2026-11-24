package com.ticket.booking;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ticket.service.OrderService;
@Component
@EnableScheduling
public class OrderExpiryJob {
 private final JdbcTemplate db; private final OrderService orders;
 public OrderExpiryJob(JdbcTemplate db,OrderService orders) {this.db=db;this.orders=orders;}
 @Scheduled(fixedDelay=30000) public void expire() {
  var numbers=db.queryForList("SELECT order_number FROM `order` WHERE status=0 AND deleted=0 AND create_time<=? ORDER BY id LIMIT 100",String.class,java.time.LocalDateTime.now().minusMinutes(15));
  for(String n:numbers) { try {orders.cancelOrder(n,true);} catch(Exception e) {org.slf4j.LoggerFactory.getLogger(getClass()).warn("Expire order failed: {}",n,e);} }
 }
}
