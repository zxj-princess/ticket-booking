package com.ticket.booking;
import org.springframework.web.bind.annotation.*;
import com.ticket.common.result.R;
@RestController
@RequestMapping("/ai/actions")
public class ActionController {
 private final ActionService service;
 public ActionController(ActionService service) {this.service=service;}
 @GetMapping public R<?> list() {return R.success(service.list());}
 @PostMapping("/{id}/confirm") public R<?> confirm(@PathVariable String id) {return R.success(service.confirm(id));}
 @PostMapping("/{id}/cancel") public R<?> cancel(@PathVariable String id) {service.cancel(id);return R.success();}
}
