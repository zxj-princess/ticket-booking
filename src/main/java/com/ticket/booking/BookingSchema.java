package com.ticket.booking;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
public class BookingSchema {
 private final JdbcTemplate db;
 public BookingSchema(JdbcTemplate db) { this.db=db; }
 @PostConstruct public void initialize() {
  db.execute("CREATE TABLE IF NOT EXISTS ticket_knowledge_segment (id VARCHAR(64) PRIMARY KEY)");
  db.execute("CREATE TABLE IF NOT EXISTS ticket_inventory (train_id BIGINT NOT NULL, travel_date DATE NOT NULL, seat_type INT NOT NULL, capacity INT NOT NULL, available INT NOT NULL, PRIMARY KEY(train_id,travel_date,seat_type))");
  db.execute("CREATE TABLE IF NOT EXISTS ticket_event (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_number VARCHAR(80) NOT NULL, event_type VARCHAR(40) NOT NULL, amount DECIMAL(12,2), detail VARCHAR(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
  db.execute("CREATE TABLE IF NOT EXISTS ticket_action (id VARCHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, session_id VARCHAR(100), action_type VARCHAR(20) NOT NULL, payload TEXT NOT NULL, summary TEXT NOT NULL, status VARCHAR(20) NOT NULL, expires_at TIMESTAMP NOT NULL, result TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
 }
}
