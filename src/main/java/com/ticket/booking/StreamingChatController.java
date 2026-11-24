package com.ticket.booking;
import com.ticket.ai.agent.TicketBookingTools;
import com.ticket.ai.rag.KnowledgeService;
import com.ticket.common.context.UserContext;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.ChatDTO;
import com.ticket.entity.AiChatRecord;
import com.ticket.mapper.AiChatRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.*;
import dev.langchain4j.service.*;
import dev.langchain4j.service.tool.*;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/ai/chat")
public class StreamingChatController {
 public interface Assistant {
  @dev.langchain4j.service.SystemMessage("你是火车票助手。当前日期由用户消息前缀提供。使用工具查询实时车次和订单，不编造信息。规则问题必须先searchKnowledge，并用[来源ID]引用本次检索片段，没有依据则说明无法确认。订票退票改签工具只准备确认卡片，绝不表示已经执行，必须让用户点击卡片按钮。金额以业务工具为准，系统只提供模拟支付。缺少乘客、日期或车次时先追问。")
  TokenStream chat(@dev.langchain4j.service.UserMessage String message);
 }
 private final StreamingChatLanguageModel model; private final TicketBookingTools tools; private final KnowledgeService knowledge; private final AiChatRecordMapper records; private final ObjectMapper json;
 private final Set<String> active=ConcurrentHashMap.newKeySet();
 private final ExecutorService workers=new ThreadPoolExecutor(4,4,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(32),new ThreadPoolExecutor.AbortPolicy());
 public StreamingChatController(StreamingChatLanguageModel model,TicketBookingTools tools,KnowledgeService knowledge,AiChatRecordMapper records,ObjectMapper json) {this.model=model;this.tools=tools;this.knowledge=knowledge;this.records=records;this.json=json;}
 @jakarta.annotation.PreDestroy public void shutdown(){workers.shutdownNow();}
 @PostMapping(value="/stream",produces="text/event-stream")
 public SseEmitter stream(@RequestBody ChatDTO dto,HttpServletRequest request) {
  Long user=(Long)request.getAttribute("userId");
  if(dto.getSessionId()==null||dto.getSessionId().isBlank()||dto.getSessionId().length()>100||dto.getMessage()==null||dto.getMessage().isBlank()||dto.getMessage().length()>10000) throw new BusinessException("消息或会话参数无效");
  String key=user+":"+dto.getSessionId();
  if(!active.add(key)) throw new BusinessException("当前会话正在回复，请稍后重试");
  SseEmitter emitter=new SseEmitter(120000L); AtomicBoolean closed=new AtomicBoolean();
  Runnable close=()->{closed.set(true);active.remove(key);};
  emitter.onCompletion(close);emitter.onTimeout(close);emitter.onError(e->close.run());
  try { workers.submit(()->{
   try {
    var memory=MessageWindowChatMemory.withMaxMessages(24);
    var history=records.selectList(new LambdaQueryWrapper<AiChatRecord>().eq(AiChatRecord::getUserId,user).eq(AiChatRecord::getSessionId,dto.getSessionId()).orderByDesc(AiChatRecord::getId).last("LIMIT 20"));
    Collections.reverse(history);
    for(var record:history) { if("user".equals(record.getRole()))memory.add(dev.langchain4j.data.message.UserMessage.from(record.getContent()));else memory.add(AiMessage.from(record.getContent())); }
    save(user,dto.getSessionId(),"user",dto.getMessage());
    Map<ToolSpecification,ToolExecutor> executors=new LinkedHashMap<>();
    Map<String,KnowledgeService.Source> sources=new ConcurrentHashMap<>();
    for(var method:TicketBookingTools.class.getMethods()) {
     if(!method.isAnnotationPresent(Tool.class)) continue;
     var delegate=new DefaultToolExecutor(tools,method);
     executors.put(ToolSpecifications.toolSpecificationFrom(method),(call,memoryId)->{
      if(closed.get()) return "请求已停止，未执行操作";
      UserContext.setUserId(user);
      try {
       send(emitter,closed,"progress","正在处理："+method.getName());
       if(method.getName().equals("searchKnowledge")) {
        String query=json.readTree(call.arguments()).path("query").asText();
        var found=knowledge.searchSources(query);for(var source:found)sources.put(source.id(),source);
        send(emitter,closed,"sources",found);return json.writeValueAsString(found);
       }
       return delegate.execute(call,memoryId);
      } catch(Exception e) {return "工具执行失败，请重试："+e.getMessage();}
      finally {UserContext.clear();}
     });
    }
    send(emitter,closed,"progress","正在理解行程需求");
    Assistant assistant=AiServices.builder(Assistant.class).streamingChatLanguageModel(model).chatMemory(memory).tools(executors).build();
    assistant.chat("当前日期："+java.time.LocalDate.now()+"\n"+dto.getMessage())
     .onPartialResponse(token->send(emitter,closed,"delta",token))
     .onCompleteResponse(response->{
      if(closed.get()) return;
      try {
       String text=response.aiMessage().text();
       if(text==null) text="请查看待确认操作卡片。";
       final var known=sources.keySet();
       text=java.util.regex.Pattern.compile("\\[S[a-f0-9]{32}\\]").matcher(text).replaceAll(m->known.contains(m.group().substring(1,m.group().length()-1))?m.group():"[引用未验证]");
       save(user,dto.getSessionId(),"assistant",text);
       send(emitter,closed,"done",text); emitter.complete();
      } catch(Exception e) {send(emitter,closed,"failure","回复保存失败，请刷新检查记录");emitter.complete();}
     })
     .onError(e->{send(emitter,closed,"failure","AI服务暂不可用，请重试；已生成的确认卡片可继续查看");emitter.complete();})
     .start();
   } catch(Exception e) {send(emitter,closed,"failure","请求处理失败，请重试");emitter.complete();}
  }); } catch(RejectedExecutionException e) {active.remove(key);emitter.completeWithError(new BusinessException("服务繁忙，请稍后重试"));}
  return emitter;
 }
 private void save(Long user,String session,String role,String text) {
  AiChatRecord record=new AiChatRecord();record.setUserId(user);record.setSessionId(session);record.setRole(role);record.setContent(text);record.setCreateTime(java.time.LocalDateTime.now());records.insert(record);
 }
 private static void send(SseEmitter emitter,AtomicBoolean closed,String event,Object data) {
  if(closed.get())return;
  try {emitter.send(SseEmitter.event().name(event).data(data));}catch(Exception e){closed.set(true);emitter.complete();}
 }
}
