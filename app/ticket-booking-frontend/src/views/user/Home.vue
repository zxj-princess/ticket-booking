<template>
  <div class="travel-home">
    <header class="welcome"><span class="eyebrow">智能出行 · 从这里开始</span><h1>下一站，去哪里？</h1><p>查找车次，比较行程，或者让 AI 帮你安排。每一笔订单都由你确认。</p></header>
    <div class="entry-grid">
      <section class="search-panel"><h2>查询车票</h2><p class="muted">支持未来 15 天车次，余票以下单时校验为准</p>
        <el-form label-position="top" @submit.prevent="search">
          <div class="station-grid"><el-form-item label="出发站"><el-select v-model="form.fromStation" filterable placeholder="选择出发站" :loading="loading"><el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.name" /></el-select></el-form-item>
          <el-button circle aria-label="交换出发到达站" @click="swap">⇄</el-button>
          <el-form-item label="到达站"><el-select v-model="form.toStation" filterable placeholder="选择到达站" :loading="loading"><el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.name" /></el-select></el-form-item></div>
          <el-form-item label="出发日期"><el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" :disabled-date="disabledDate" style="width:100%" /></el-form-item>
          <el-alert v-if="failed" title="站点加载失败" type="error" :closable="false"><el-button text @click="loadStations">重新加载</el-button></el-alert>
          <el-button type="primary" size="large" native-type="submit" class="wide">查询可用车次</el-button>
        </el-form>
      </section>
      <section class="assistant-panel"><span class="eyebrow">AI 行程助手</span><h2>一句话，开始规划</h2><p>告诉我出发地、目的地和时间。我会查询车次，解释规则，并在订票或退改签前向你确认。</p>
        <el-input v-model="prompt" type="textarea" :rows="3" placeholder="例如：帮我查明天北京到上海的高铁" maxlength="1000" />
        <el-button type="primary" plain size="large" @click="ask">进入智能助手</el-button>
        <div class="examples"><button v-for="text in examples" :key="text" @click="prompt = text">{{ text }}</button></div>
        <small>支付、退款和改签差额为演示模拟，不产生真实交易。</small>
      </section>
    </div>
    <section class="journey-links"><router-link to="/my-orders"><strong>我的行程</strong><span>支付、取消、退改签与处理记录 →</span></router-link><router-link to="/profile"><strong>常用乘客</strong><span>出发前，先完善乘客信息 →</span></router-link></section>
    <section v-if="recent.length" class="recent"><h2>最近查询</h2><el-button v-for="(item, i) in recent" :key="i" @click="reuse(item)">{{ item.fromStation }} → {{ item.toStation }}</el-button></section>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { listStations } from '@/api/station'
import { ElMessage } from 'element-plus'
const router = useRouter(), stations = ref([]), loading = ref(false), failed = ref(false), recent = ref([]), prompt = ref('')
const form = reactive({ fromStation: '', toStation: '', date: dayjs().add(1, 'day').format('YYYY-MM-DD') })
const examples = ['明天北京到上海的高铁', '退票手续费如何计算？', '如何办理改签？']
const disabledDate = date => dayjs(date).isBefore(dayjs().startOf('day')) || dayjs(date).isAfter(dayjs().add(15, 'day').endOf('day'))
function swap() { [form.fromStation, form.toStation] = [form.toStation, form.fromStation] }
async function loadStations() { loading.value = true; failed.value = false; try { const res = await listStations(); stations.value = res.data || [] } catch { failed.value = true } finally { loading.value = false } }
function search() {
  if (!form.fromStation || !form.toStation || !form.date || form.fromStation === form.toStation) return ElMessage.warning('请选择不同的出发站、到达站和有效日期')
  recent.value = [{ ...form }, ...recent.value.filter(x => x.fromStation !== form.fromStation || x.toStation !== form.toStation)].slice(0, 4)
  localStorage.setItem('recent-routes', JSON.stringify(recent.value))
  router.push({ path: '/search', query: { from: form.fromStation, to: form.toStation, date: form.date } })
}
function ask() { router.push({ path: '/ai-chat', query: prompt.value.trim() ? { prompt: prompt.value.trim() } : {} }) }
function reuse(item) { form.fromStation = item.fromStation; form.toStation = item.toStation }
onMounted(() => { loadStations(); try { recent.value = JSON.parse(localStorage.getItem('recent-routes') || '[]') } catch {} })
</script>
<style scoped>
.travel-home { max-width: 1120px; margin: 0 auto; padding: 40px 24px 64px; }
.welcome { margin-bottom: 32px; }.eyebrow { color: #3056a0; font-size: 13px; font-weight: 600; letter-spacing: 1px; }
h1 { font-size: 34px; letter-spacing: -1px; margin: 12px 0; color: #172c49; }h2 { font-size: 21px; margin: 0 0 10px; }p { line-height: 1.8; color: #526078; }.muted { font-size: 13px; margin-bottom: 24px; }
.entry-grid { display: grid; grid-template-columns: 1.15fr 1fr; gap: 24px; }.search-panel, .assistant-panel { padding: 30px; border: 1px solid #dce3ee; border-radius: 16px; background: white; }
.assistant-panel { background: #edf3fc; display: flex; flex-direction: column; gap: 14px; }.assistant-panel h2 { margin: 0; }.assistant-panel p { margin: 0; }.assistant-panel small { color: #526078; line-height: 1.7; }
.station-grid { display: grid; grid-template-columns: 1fr 32px 1fr; align-items: center; gap: 12px; }.wide { width: 100%; margin-top: 12px; }
.examples { display: flex; flex-wrap: wrap; gap: 8px; }.examples button { border: 1px solid #cdd9eb; border-radius: 8px; padding: 7px 10px; background: white; color: #3056a0; cursor: pointer; }
.journey-links { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin: 28px 0; }.journey-links a { display: flex; flex-direction: column; gap: 8px; padding: 22px 0; border-bottom: 1px solid #dce3ee; text-decoration: none; color: #172c49; }.journey-links span { font-size: 13px; color: #526078; }.recent { margin-top: 32px; }.recent .el-button { margin: 8px 8px 0 0; }
@media(max-width:768px) { .travel-home { padding: 24px 16px; } .entry-grid,.journey-links { grid-template-columns: 1fr; gap: 16px; }.search-panel,.assistant-panel { padding: 20px; }h1 { font-size: 28px; }.station-grid { grid-template-columns: 1fr; gap: 0; }.station-grid>.el-button { margin-bottom: 16px; } }
</style>
