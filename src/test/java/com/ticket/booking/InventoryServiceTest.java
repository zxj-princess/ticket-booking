package com.ticket.booking;
import com.ticket.entity.Train;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class InventoryServiceTest {
 JdbcTemplate db; InventoryService inventory; TransactionTemplate tx; Train train; LocalDate date=LocalDate.now().plusDays(1);
 @BeforeEach void setup() {
  var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");
  db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
  db.execute("CREATE TABLE train(id BIGINT PRIMARY KEY,deleted INT)");db.update("INSERT INTO train VALUES(1,0)");
  db.execute("CREATE TABLE `order` (train_id BIGINT,depart_time TIMESTAMP,seat_type INT,status INT,deleted INT)");
  var schema=new BookingSchema(db);schema.initialize();inventory=new InventoryService(db,schema);
  train=new Train();train.setId(1L);train.setSecondSeats(2);train.setFirstSeats(1);
 }
 @Test void concurrentBuyersCannotOversell() throws Exception {
  var pool=Executors.newFixedThreadPool(8); var gate=new CountDownLatch(1);var jobs=new ArrayList<Future<Boolean>>();
  try {
   for(int i=0;i<20;i++)jobs.add(pool.submit(()->{gate.await();try {tx.executeWithoutResult(s->inventory.reserve(train,date,2));return true;}catch(com.ticket.common.exception.BusinessException e){return false;}}));
   gate.countDown();int success=0;for(var job:jobs)if(job.get(20,TimeUnit.SECONDS))success++;
   assertEquals(2,success);assertEquals(0,inventory.available(train,date,2));
  } finally {pool.shutdownNow();}
 }
 @Test void rollbackDoesNotConsumeInventory() {
  assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{inventory.reserve(train,date,2);throw new IllegalStateException();}));
  assertEquals(2,inventory.available(train,date,2));
 }
 @Test void datesAndSeatClassesAreIndependent() {
  tx.executeWithoutResult(s->inventory.reserve(train,date,2));
  assertEquals(1,inventory.available(train,date,2));assertEquals(2,inventory.available(train,date.plusDays(1),2));assertEquals(1,inventory.available(train,date,1));
 }
 @Test void legacyActiveOrdersAreAccountedFor() {
  db.update("INSERT INTO `order` VALUES(1,?,2,1,0)",date.atTime(12,0));
  tx.executeWithoutResult(s->inventory.reserve(train,date,2));assertEquals(0,inventory.available(train,date,2));
 }
 @Test void releaseRestoresSeatWithinCapacity() {
  tx.executeWithoutResult(s->{inventory.reserve(train,date,2);inventory.release(train,date,2);});
  assertEquals(2,inventory.available(train,date,2));
 }
 @Test void legacyOversellingDoesNotReleasePhantomSeats() {
  for(int i=0;i<3;i++)db.update("INSERT INTO `order` VALUES(1,?,2,1,0)",date.atTime(12,0));
  tx.executeWithoutResult(status->inventory.release(train,date,2));
  assertEquals(0,inventory.available(train,date,2));
 }
}
