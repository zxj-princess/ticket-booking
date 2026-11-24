package com.ticket.booking;
import org.junit.jupiter.api.Test;
import com.ticket.ai.agent.TicketBookingTools;
import com.ticket.ai.rag.KnowledgeService;
import com.ticket.mapper.AiChatRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.data.message.AiMessage;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.ArrayList;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
class StreamingChatControllerTest {
 @Test void emitsIncrementalTokensAndPersistsFinalResponse() throws Exception {
  var model=mock(StreamingChatLanguageModel.class);
  doAnswer(call->{
   StreamingChatResponseHandler handler=call.getArgument(1);
   handler.onPartialResponse("Hello ");handler.onPartialResponse("traveler");
   handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("Hello traveler")).build());return null;
  }).when(model).chat(any(ChatRequest.class),any(StreamingChatResponseHandler.class));
  var records=mock(AiChatRecordMapper.class);when(records.selectList(any())).thenReturn(new ArrayList<>());
  var controller=new StreamingChatController(model,mock(TicketBookingTools.class),mock(KnowledgeService.class),records,new ObjectMapper());
  try {
   var mvc=MockMvcBuilders.standaloneSetup(controller).build();
   var started=mvc.perform(post("/ai/chat/stream").requestAttr("userId",1L).contentType(MediaType.APPLICATION_JSON).content("{\"sessionId\":\"session-a\",\"message\":\"hello\"}")).andReturn();
   started.getAsyncResult(5000);
   String body=mvc.perform(asyncDispatch(started)).andReturn().getResponse().getContentAsString();
   assertTrue(body.contains("event:delta"),body);assertTrue(body.contains("event:done"),body);assertTrue(body.contains("Hello traveler"),body);
   verify(records,times(2)).insert(any(com.ticket.entity.AiChatRecord.class));
  } finally {controller.shutdown();}
 }
}
