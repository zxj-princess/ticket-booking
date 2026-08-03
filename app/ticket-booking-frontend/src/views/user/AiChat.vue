<template>
  <div class="ai-chat-container">
    <aside class="sidebar" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo-section">
          <div class="logo-icon">
            <el-icon :size="24"><ChatDotRound /></el-icon>
          </div>
          <span v-if="!sidebarCollapsed" class="logo-text">AI 出行助手</span>
        </div>
        <el-button
          class="new-chat-btn"
          type="primary"
          :icon="Plus"
          @click="createNewConversation"
          circle
        />
      </div>

      <div v-if="!sidebarCollapsed" class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索会话"
          prefix-icon="Search"
          clearable
          size="small"
        />
      </div>

      <div class="conversation-list">
        <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: conv.id === currentConversationId }"
          @click="selectConversation(conv.id)"
        >
          <div class="conv-content">
            <div class="conv-title">{{ conv.title }}</div>
            <div class="conv-preview">{{ getLastMessagePreview(conv) }}</div>
          </div>
          <el-icon
            v-if="!sidebarCollapsed"
            class="delete-icon"
            :size="16"
            @click.stop="deleteConversation(conv.id)"
          >
            <Delete />
          </el-icon>
        </div>

        <el-empty
          v-if="filteredConversations.length === 0"
          :image-size="80"
          description="暂无会话记录"
        />
      </div>

      <div v-if="!sidebarCollapsed" class="sidebar-footer">
        <div class="collapse-btn" @click="toggleSidebar">
          <el-icon :size="16"><DArrowLeft /></el-icon>
          <span>收起侧栏</span>
        </div>
      </div>
    </aside>

    <main class="chat-main">
      <header class="chat-header">
        <div class="header-left">
          <el-button
            v-if="sidebarCollapsed"
            :icon="DArrowRight"
            circle
            size="small"
            @click="toggleSidebar"
          />
          <span class="chat-title">{{ currentConversation?.title || 'AI 出行助手' }}</span>
        </div>
        <div class="header-right">
          <span v-if="totalTokens > 0" class="token-info">
            <el-icon :size="14"><Coin /></el-icon>
            {{ totalTokens }} tokens
          </span>
        </div>
      </header>

      <div ref="messageContainerRef" class="message-container">
        <div v-if="currentMessages.length === 0" class="welcome-section">
          <div class="welcome-icon">
            <el-icon :size="48" color="#fff"><ChatDotRound /></el-icon>
          </div>
          <h3>你好，我是 AI 出行助手</h3>
          <p>我可以帮你查询车次、解答出行问题、处理退改签等业务</p>
          <div class="suggestion-list">
            <div
              v-for="(suggestion, idx) in suggestions"
              :key="idx"
              class="suggestion-item"
              @click="sendMessage(suggestion)"
            >
              <el-icon :size="16" color="#2563eb"><Promotion /></el-icon>
              <span>{{ suggestion }}</span>
            </div>
          </div>
        </div>

        <template v-else>
          <div
            v-for="(msg, idx) in currentMessages"
            :key="msg.id"
            class="message-wrapper"
            :class="msg.role"
          >
            <div v-if="msg.role === 'user'" class="avatar user-avatar">
              <el-icon :size="20"><User /></el-icon>
            </div>
            <div class="message-body">
              <div class="message-meta">
                <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
              <div v-if="msg.role === 'system'" class="system-message">
                {{ msg.content }}
              </div>
              <div
                v-else-if="msg.role === 'assistant'"
                class="assistant-bubble"
              >
                <div
                  v-if="msg.streaming"
                  class="typing-indicator"
                >
                  <span></span><span></span><span></span>
                </div>
                <div
                  v-show="!msg.streaming"
                  class="markdown-content"
                  v-html="renderMarkdown(msg.content)"
                />
                <div
                  v-if="!msg.streaming && msg.content"
                  class="message-actions"
                >
                  <el-tooltip content="复制" placement="top">
                    <el-icon
                      class="action-icon"
                      :size="16"
                      @click="copyMessage(msg.content)"
                    >
                      <CopyDocument />
                    </el-icon>
                  </el-tooltip>
                  <el-tooltip content="重新生成" placement="top">
                    <el-icon
                      class="action-icon"
                      :size="16"
                      @click="regenerateMessage(idx)"
                    >
                      <RefreshRight />
                    </el-icon>
                  </el-tooltip>
                  <el-tooltip content="点赞" placement="top">
                    <el-icon class="action-icon" :size="16"><Pointer /></el-icon>
                  </el-tooltip>
                </div>
              </div>
              <div v-else class="user-bubble">
                {{ msg.content }}
              </div>
            </div>
            <div v-if="msg.role === 'assistant'" class="avatar ai-avatar">
              <el-icon :size="20" color="#fff"><ChatLineRound /></el-icon>
            </div>
          </div>
        </template>
      </div>

      <footer class="chat-footer">
        <div class="quick-questions" v-if="currentMessages.length > 0">
          <el-tag
            v-for="(q, idx) in quickQuestions"
            :key="idx"
            class="quick-tag"
            effect="plain"
            round
            @click="sendMessage(q)"
          >
            {{ q }}
          </el-tag>
        </div>
        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入你的问题... (Enter 发送，Shift+Enter 换行)"
            resize="none"
            @keydown.enter.exact.prevent="handleSend"
            :disabled="isStreaming"
          />
          <el-button
            class="send-btn"
            type="primary"
            :icon="Promotion"
            circle
            :disabled="!inputText.trim() || isStreaming"
            @click="handleSend"
          />
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import {
  ChatDotRound,
  Plus,
  Delete,
  Search,
  DArrowLeft,
  DArrowRight,
  Coin,
  User,
  ChatLineRound,
  CopyDocument,
  RefreshRight,
  Pointer,
  Promotion
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import dayjs from 'dayjs'
import { sendMessage as sendChatMessage } from '@/api/chat'
import { useUserStore } from '@/stores/user'

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
  highlight: true
})

const userStore = useUserStore()

const sidebarCollapsed = ref(false)
const searchKeyword = ref('')
const inputText = ref('')
const isStreaming = ref(false)
const totalTokens = ref(0)
const messageContainerRef = ref(null)

const suggestions = [
  '北京到上海的高铁',
  '退票规则是什么？',
  '学生票怎么买？',
  '如何改签车票？'
]

const quickQuestions = [
  '北京到上海的高铁',
  '退票规则',
  '学生票购买',
  '改签流程',
  '丢失车票怎么办？'
]

const conversations = ref([])
const currentConversationId = ref(null)

const currentConversation = computed(() => {
  return conversations.value.find(c => c.id === currentConversationId.value)
})

const currentMessages = computed(() => {
  return currentConversation.value?.messages || []
})

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  const kw = searchKeyword.value.toLowerCase()
  return conversations.value.filter(conv =>
    conv.title.toLowerCase().includes(kw) ||
    conv.messages.some(m => m.content.toLowerCase().includes(kw))
  )
})

const generateId = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7)

const createNewConversation = () => {
  const conv = {
    id: generateId(),
    title: '新对话',
    messages: [],
    createdAt: Date.now()
  }
  conversations.value.unshift(conv)
  currentConversationId.value = conv.id
  saveToLocal()
}

const selectConversation = (id) => {
  currentConversationId.value = id
}

const deleteConversation = (id) => {
  ElMessageBox.confirm('确定删除该会话吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConversationId.value === id) {
      currentConversationId.value = conversations.value[0]?.id || null
    }
    saveToLocal()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const getLastMessagePreview = (conv) => {
  const msgs = conv.messages
  if (msgs.length === 0) return '暂无消息'
  const lastMsg = msgs[msgs.length - 1]
  return lastMsg.content.slice(0, 30) || '...'
}

const formatTime = (timestamp) => {
  return dayjs(timestamp).format('HH:mm')
}

const renderMarkdown = (content) => {
  if (!content) return ''
  return md.render(content)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainerRef.value) {
      messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight
    }
  })
}

const addMessage = (message) => {
  const conv = currentConversation.value
  if (!conv) return
  conv.messages.push(message)
  if (conv.messages.length === 1 && message.role === 'user') {
    conv.title = message.content.slice(0, 20) || '新对话'
  }
  scrollToBottom()
}

const sendMessage = async (text) => {
  if (!text.trim() || isStreaming.value) return

  if (!currentConversationId.value) {
    createNewConversation()
  }

  const userMessage = {
    id: generateId(),
    role: 'user',
    content: text.trim(),
    timestamp: Date.now()
  }
  addMessage(userMessage)
  inputText.value = ''

  await callAIAgent(text.trim())
  saveToLocal()
}

const handleSend = () => {
  if (inputText.value.trim() && !isStreaming.value) {
    sendMessage(inputText.value)
  }
}

const callAIAgent = async (userText) => {
  isStreaming.value = true

  const thinkingMessage = {
    id: generateId(),
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
    streaming: true
  }
  addMessage(thinkingMessage)

  try {
    const sessionId = currentConversationId.value
    const res = await sendChatMessage({
      sessionId: sessionId,
      message: userText
    })

    if (res && res.data) {
      const replyContent = res.data.content || '抱歉，AI助手暂时无法回复。'
      const conv = currentConversation.value
      const lastMsg = conv.messages[conv.messages.length - 1]

      // 模拟流式打字效果
      for (let i = 0; i < replyContent.length; i++) {
        lastMsg.content += replyContent[i]
        totalTokens.value += 1
        scrollToBottom()
        await new Promise(resolve => setTimeout(resolve, 15))
      }

      lastMsg.streaming = false
    }
  } catch (error) {
    console.error('AI调用失败:', error)
    const conv = currentConversation.value
    const lastMsg = conv.messages[conv.messages.length - 1]
    lastMsg.content = '抱歉，AI助手暂时无法回复，请稍后再试。'
    lastMsg.streaming = false
    ElMessage.error('AI服务调用失败')
  }

  saveToLocal()
  isStreaming.value = false
}

const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('复制成功')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = content
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('复制成功')
  }
}

const regenerateMessage = async (idx) => {
  const conv = currentConversation.value
  if (!conv) return

  const assistantMsg = conv.messages[idx]
  const userMsg = conv.messages.slice(0, idx).reverse().find(m => m.role === 'user')

  if (!userMsg) return

  conv.messages.splice(idx)
  totalTokens.value -= assistantMsg.content.length

  await callAIAgent(userMsg.content)
  saveToLocal()
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const saveToLocal = () => {
  try {
    const data = {
      conversations: conversations.value,
      currentId: currentConversationId.value,
      totalTokens: totalTokens.value
    }
    localStorage.setItem('ai-chat-data', JSON.stringify(data))
  } catch {}
}

const loadFromLocal = () => {
  try {
    const saved = localStorage.getItem('ai-chat-data')
    if (saved) {
      const data = JSON.parse(saved)
      conversations.value = data.conversations || []
      currentConversationId.value = data.currentId || null
      totalTokens.value = data.totalTokens || 0
    }
  } catch {}
}

watch(currentConversationId, () => {
  saveToLocal()
})

watch(conversations, () => {
  scrollToBottom()
}, { deep: true })

onMounted(() => {
  loadFromLocal()
  if (!currentConversationId.value) {
    createNewConversation()
  }
})
</script>

<style lang="scss" scoped>
.ai-chat-container {
  display: flex;
  height: calc(100vh - 120px);
  max-width: 1400px;
  margin: 0 auto;
  background: var(--bg-soft);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-light);
}

.sidebar {
  width: 260px;
  background: linear-gradient(180deg, var(--primary-blue-bg) 0%, #fff 100%);
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  flex-shrink: 0;
  border-right: 1px solid var(--border-blue);

  &.sidebar-collapsed {
    width: 60px;

    .logo-text,
    .search-box,
    .conv-content,
    .sidebar-footer span {
      display: none;
    }

    .sidebar-header {
      justify-content: center;
    }
  }
}

.sidebar-header {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-blue);
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  color: var(--primary-blue-deep);
}

.new-chat-btn {
  background: var(--gradient-button) !important;
  border: none;
  box-shadow: 0 2px 6px rgba(249, 115, 22, 0.3);

  &:hover {
    transform: scale(1.05);
  }
}

.search-box {
  padding: 12px 16px;
}

:deep(.search-box .el-input__wrapper) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--border-blue) inset;

  &:hover,
  &.is-focus {
    box-shadow: 0 0 0 1px var(--primary-blue) inset, 0 0 0 3px rgba(37,99,235,0.08) !important;
  }
}

:deep(.search-box .el-input__inner) {
  color: var(--text-primary);

  &::placeholder {
    color: var(--text-muted);
  }
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--primary-blue-light);
    border-radius: 2px;
    opacity: 0.5;
  }
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.3s ease;

  &:hover {
    background: var(--primary-blue-bg);
  }

  &.active {
    background: var(--primary-blue-lighter);
    border: 1px solid var(--border-blue);
  }
}

.conv-content {
  flex: 1;
  min-width: 0;
}

.conv-title {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-primary);
}

.conv-preview {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.delete-icon {
  color: var(--text-muted);
  opacity: 0;
  transition: opacity 0.2s ease, color 0.2s ease;
  margin-left: 8px;
  flex-shrink: 0;

  &:hover {
    color: var(--brand-red);
  }
}

.conversation-item:hover .delete-icon {
  opacity: 1;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border-blue);
}

.collapse-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 13px;
  transition: color 0.2s ease;

  &:hover {
    color: var(--primary-blue-deep);
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-card);
}

.chat-header {
  padding: 14px 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-blue-deep);
}

.token-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted);
  background: var(--primary-orange-bg);
  padding: 4px 10px;
  border-radius: 12px;
  border: 1px solid var(--primary-orange-light);
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
  background: linear-gradient(180deg, var(--primary-blue-bg) 0%, var(--bg-soft) 20%);

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--primary-blue-light);
    border-radius: 3px;
    opacity: 0.5;

    &:hover {
      background: var(--primary-blue);
    }
  }
}

.welcome-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.3);
}

.welcome-section h3 {
  font-size: 20px;
  color: var(--primary-blue-deep);
  margin: 0 0 8px;
  font-weight: 700;
}

.welcome-section p {
  color: var(--text-secondary);
  margin: 0 0 28px;
  font-size: 14px;
}

.suggestion-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  width: 100%;
  max-width: 600px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  background: var(--bg-card);
  border: 1px solid var(--border-blue);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: var(--text-secondary);
  text-align: left;

  &:hover {
    border-color: var(--primary-blue);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
    transform: translateY(-2px);
    background: var(--primary-blue-bg);
  }
}

.message-wrapper {
  display: flex;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;

  &.user {
    flex-direction: row-reverse;

    .message-body {
      align-items: flex-end;
    }
  }

  &.assistant {
    flex-direction: row;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar {
  background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
  color: #fff;
  margin-left: 12px;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
}

.ai-avatar {
  background: linear-gradient(135deg, var(--primary-orange) 0%, var(--primary-orange-light) 100%);
  color: var(--primary-orange-deep);
  margin-right: 12px;
  box-shadow: 0 2px 6px rgba(249, 115, 22, 0.3);
}

.message-body {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-meta {
  margin-bottom: 4px;
  padding: 0 4px;
}

.message-time {
  font-size: 12px;
  color: var(--text-muted);
}

/* 用户气泡 - 蓝色系 */
.user-bubble {
  background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
  color: #fff;
  padding: 12px 16px;
  border-radius: 16px 4px 16px 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

/* 机器人气泡 - 白色卡片 */
.assistant-bubble {
  background: var(--bg-card);
  border: 1px solid var(--border-blue);
  border-radius: 4px 16px 16px 16px;
  padding: 14px 18px;
  position: relative;
  min-width: 100px;
  box-shadow: var(--shadow-card);
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;

  span {
    width: 8px;
    height: 8px;
    background: var(--primary-blue);
    border-radius: 50%;
    animation: typing 1.4s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }

  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

.markdown-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  word-break: break-word;

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 12px 0 8px;
    color: var(--primary-blue-deep);
    font-weight: 600;
  }

  :deep(h1) {
    font-size: 20px;
  }

  :deep(h2) {
    font-size: 18px;
  }

  :deep(h3) {
    font-size: 16px;
  }

  :deep(p) {
    margin: 8px 0;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 24px;
    margin: 8px 0;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 12px 0;
    font-size: 13px;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid var(--border-blue);
    padding: 8px 12px;
    text-align: left;
  }

  :deep(th) {
    background: var(--primary-blue-bg);
    font-weight: 600;
    color: var(--primary-blue-deep);
  }

  :deep(blockquote) {
    border-left: 4px solid var(--primary-blue);
    margin: 12px 0;
    padding: 8px 16px;
    background: var(--primary-blue-bg);
    color: var(--text-secondary);
    border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  }

  :deep(code) {
    background: var(--primary-blue-bg);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    color: var(--primary-blue-deep);
  }

  :deep(pre) {
    background: #1e293b;
    color: #e2e8f0;
    padding: 16px;
    border-radius: var(--radius-sm);
    overflow-x: auto;
    margin: 12px 0;

    code {
      background: transparent;
      color: inherit;
      padding: 0;
      font-size: 13px;
      line-height: 1.5;
    }
  }

  :deep(a) {
    color: var(--primary-blue);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(hr) {
    border: none;
    border-top: 1px solid var(--border-light);
    margin: 16px 0;
  }

  :deep(strong) {
    color: var(--primary-blue-deep);
    font-weight: 600;
  }
}

.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-light);
}

.action-icon {
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.2s ease;

  &:hover {
    color: var(--primary-blue-deep);
  }
}

.system-message {
  background: var(--primary-blue-bg);
  color: var(--text-secondary);
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  text-align: center;
  border: 1px solid var(--border-blue);
}

.chat-footer {
  background: var(--bg-card);
  border-top: 1px solid var(--border-light);
  padding: 12px 20px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.quick-tag {
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s ease;
  border-color: var(--border-blue) !important;
  color: var(--primary-blue) !important;

  &:hover {
    border-color: var(--primary-blue) !important;
    background: var(--primary-blue-bg) !important;
  }
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;

  :deep(.el-textarea__inner) {
    border-radius: var(--radius-sm);
    padding: 10px 14px;
    font-size: 14px;
    line-height: 1.5;
    resize: none;
    box-shadow: 0 0 0 1px var(--border-medium) inset;

    &:focus {
      box-shadow: 0 0 0 1px var(--primary-blue) inset, 0 0 0 3px rgba(37,99,235,0.1) !important;
    }
  }
}

.send-btn {
  height: 40px;
  width: 40px;
  flex-shrink: 0;
  background: var(--gradient-button) !important;
  box-shadow: 0 2px 6px rgba(249, 115, 22, 0.3);
}

@media (max-width: 768px) {
  .ai-chat-container {
    flex-direction: column;
    height: calc(100vh - 100px);
    border-radius: 0;
  }

  .sidebar {
    width: 100%;
    height: auto;
    max-height: 50px;
    transition: max-height 0.3s ease;

    &:not(.sidebar-collapsed) {
      max-height: 300px;
    }
  }

  .message-body {
    max-width: 85%;
  }

  .suggestion-list {
    grid-template-columns: 1fr;
  }

  .quick-questions {
    display: none;
  }
}
</style>
