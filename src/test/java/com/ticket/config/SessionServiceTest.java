package com.ticket.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SessionServiceTest {
 @Test void rejectsForgedAndRevokedTokens() {
  SessionService service = new SessionService();
  assertNull(service.resolve("1_forged")); assertNull(service.resolve(null));
  String token = service.create(7L);
  assertEquals(7L, service.resolve(token));
  service.revoke(token); assertNull(service.resolve(token));
 }
 @Test void isolatesUsersAndSessions() {
  SessionService service = new SessionService();
  String first = service.create(1L), second = service.create(2L);
  assertNotEquals(first, second); service.revoke(first);
  assertEquals(2L, service.resolve(second));
  assertNull(new SessionService().resolve(second));
 }
}
