package com.ticket.service.impl;
import com.ticket.entity.User;
import com.ticket.mapper.*;
import com.ticket.config.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class UserServiceImplTest {
 @Test void registrationPreservesOrders() {
  UserServiceImpl service = new UserServiceImpl();
  UserMapper users = mock(UserMapper.class); OrderMapper orders = mock(OrderMapper.class);
  ReflectionTestUtils.setField(service, "userMapper", users);
  ReflectionTestUtils.setField(service, "orderMapper", orders);
  User user = new User(); user.setUsername("alice"); user.setPassword("test-secret");
  service.register(user);
  assertTrue(new BCryptPasswordEncoder().matches("test-secret", user.getPassword()));
  verifyNoInteractions(orders); assertEquals(0, user.getRole());
 }
 @Test void loginPreservesDataAndMigratesPassword() {
  UserServiceImpl service = new UserServiceImpl(); UserMapper users = mock(UserMapper.class);
  PassengerMapper passengers = mock(PassengerMapper.class); AiChatRecordMapper records = mock(AiChatRecordMapper.class);
  SessionService sessions = new SessionService();
  ReflectionTestUtils.setField(service, "userMapper", users);
  ReflectionTestUtils.setField(service, "passengerMapper", passengers);
  ReflectionTestUtils.setField(service, "aiChatRecordMapper", records);
  ReflectionTestUtils.setField(service, "sessions", sessions);
  User user = new User(); user.setId(4L); user.setStatus(1);
  user.setPassword("5f4dcc3b5aa765d61d8327deb882cf99");
  when(users.selectOne(any())).thenReturn(user);
  assertEquals(4L, sessions.resolve(service.login("alice", "password")));
  assertTrue(new BCryptPasswordEncoder().matches("password", user.getPassword()));
  verifyNoInteractions(passengers, records);
 }
}
