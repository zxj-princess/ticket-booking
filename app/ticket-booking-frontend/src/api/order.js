import request from '@/utils/request'

export function createOrder(data) {
  return request.post('/order/book', data)
}

export function payOrder(orderNumber) {
  return request.post(`/order/pay/${orderNumber}`)
}

export function getOrderByNumber(orderNumber) {
  return request.get(`/order/${orderNumber}`)
}

export function getOrderById(id) {
  return request.get(`/order/id/${id}`)
}

// 智能选择查询方式
export function getOrder(param) {
  if (!param) return getOrderByNumber('')
  // 如果是纯数字，使用ID查询
  if (/^\d+$/.test(String(param))) {
    return getOrderById(param)
  }
  // 否则使用订单号查询
  return getOrderByNumber(param)
}

// 兼容别名
export { getOrder as getOrderByIdOrNumber }

export function getMyOrders(params) {
  return request.get('/order/my', { params })
}

export function listAllOrders(params) {
  return request.get('/order/list', { params })
}

// 兼容别名
export const listOrders = listAllOrders

export function refundOrder(orderNumber) {
  return request.post(`/order/refund/${orderNumber}`)
}

export function changeOrder(orderNumber, data) {
  return request.post(`/order/change/${orderNumber}`, data)
}

export function cancelOrder(id) {
  return request.post(`/order/cancel/${id}`)
}

export const getOrderEvents = number => request.get(`/order/events/${number}`)

export const prepareRefund = number => request.post(`/order/prepare-refund/${number}`)
export const prepareChange = (number, data) => request.post(`/order/prepare-change/${number}`, data)
