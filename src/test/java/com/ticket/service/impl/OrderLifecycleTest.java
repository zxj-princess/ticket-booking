package com.ticket.service.impl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import com.ticket.common.context.UserContext;
import com.ticket.entity.Order;
import com.ticket.entity.Train;
import com.ticket.mapper.*;
import com.ticket.booking.InventoryService;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class OrderLifecycleTest {
 @AfterEach void clear(){UserContext.clear();}
 @Test void overdueCancellationReleasesOnlyOnce(){
  var service=new OrderServiceImpl();var orders=mock(OrderMapper.class);var trains=mock(TrainMapper.class);var inventory=mock(InventoryService.class);
  ReflectionTestUtils.setField(service,"orderMapper",orders);ReflectionTestUtils.setField(service,"trainMapper",trains);ReflectionTestUtils.setField(service,"inventory",inventory);
  Order order=new Order();order.setOrderNumber("TK1");order.setUserId(1L);order.setTrainId(2L);order.setSeatType(2);order.setStatus(0);order.setDepartTime(LocalDateTime.now().plusDays(1));order.setCreateTime(LocalDateTime.now().minusMinutes(16));
  when(orders.selectForUpdate("TK1")).thenReturn(order);Train train=new Train();when(trains.selectById(2L)).thenReturn(train);
  service.cancelOrder("TK1",true);service.cancelOrder("TK1",true);
  assertEquals(5,order.getStatus());verify(inventory,times(1)).release(eq(train),any(),eq(2));verify(orders,times(1)).updateById(order);
 }
 @Test void expiryDoesNotCancelPaidOrder(){
  var service=new OrderServiceImpl();var orders=mock(OrderMapper.class);var inventory=mock(InventoryService.class);
  ReflectionTestUtils.setField(service,"orderMapper",orders);ReflectionTestUtils.setField(service,"inventory",inventory);
  Order order=new Order();order.setStatus(1);when(orders.selectForUpdate("TK1")).thenReturn(order);
  service.cancelOrder("TK1",true);verifyNoInteractions(inventory);verify(orders,never()).updateById(any(Order.class));
 }
 @Test void paymentAfterDeadlineDoesNotConfirm(){
  var service=new OrderServiceImpl();var orders=mock(OrderMapper.class);
  ReflectionTestUtils.setField(service,"orderMapper",orders);UserContext.setUserId(1L);
  Order order=new Order();order.setUserId(1L);order.setStatus(0);order.setCreateTime(LocalDateTime.now().minusMinutes(16));when(orders.selectForUpdate("TK1")).thenReturn(order);
  assertThrows(com.ticket.common.exception.BusinessException.class,()->service.payOrder("TK1"));verify(orders,never()).updateById(any(Order.class));
 }
}
