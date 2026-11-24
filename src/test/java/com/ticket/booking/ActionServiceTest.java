package com.ticket.booking;
import org.junit.jupiter.api.*;
import com.ticket.common.context.UserContext;
import com.ticket.common.exception.BusinessException;
import com.ticket.entity.Order;
import com.ticket.mapper.*;
import com.ticket.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ActionServiceTest {
 ActionService actions; OrderService orders; JdbcTemplate db; Order order;
 @BeforeEach void setup(){
  db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
  var schema=new BookingSchema(db);schema.initialize();orders=mock(OrderService.class);
  actions=new ActionService(db,new ObjectMapper().registerModule(new JavaTimeModule()),orders,mock(TrainMapper.class),mock(OrderMapper.class),mock(InventoryService.class));
  order=new Order();order.setOrderNumber("TK1");order.setTotalAmount(new BigDecimal("100"));order.setTrainNumber("G1");
  when(orders.quoteRefund("TK1")).thenReturn(new BigDecimal("95"));UserContext.setUserId(1L);
 }
 @AfterEach void clear(){UserContext.clear();}
 String prepare(){actions.refund(order);return actions.list().get(0).id();}
 @Test void preparingDoesNotExecute(){prepare();verify(orders,never()).refundOrder(anyString());}
 @Test void repeatedConfirmationExecutesOnlyOnce(){String id=prepare();String first=actions.confirm(id);assertEquals(first,actions.confirm(id));verify(orders,times(1)).refundOrder("TK1");}
 @Test void anotherUserCannotConfirm(){String id=prepare();UserContext.setUserId(2L);assertThrows(BusinessException.class,()->actions.confirm(id));verify(orders,never()).refundOrder(anyString());}
 @Test void expiredAndCancelledActionsCannotExecute(){String id=prepare();db.update("UPDATE ticket_action SET expires_at=? WHERE id=?",LocalDateTime.now().minusMinutes(1),id);assertThrows(BusinessException.class,()->actions.confirm(id));String second=prepare();actions.cancel(second);assertThrows(BusinessException.class,()->actions.confirm(second));}
 @Test void changedQuoteRequiresNewConfirmation(){String id=prepare();when(orders.quoteRefund("TK1")).thenReturn(new BigDecimal("90"));assertThrows(BusinessException.class,()->actions.confirm(id));verify(orders,never()).refundOrder(anyString());}
}
