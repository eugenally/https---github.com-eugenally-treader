import axios from 'axios';

/**
 * 백엔드 REST API 클라이언트.
 * Vite dev 서버가 /api 를 localhost:8080 으로 프록시하므로 baseURL 은 상대경로로 둔다.
 */
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

/**
 * 서버가 내려주는 ErrorResponse 에서 사람이 읽을 메시지를 뽑는다.
 * 형식이 다르거나 네트워크 오류면 최선의 문구로 대체한다.
 */
export function toMessage(error) {
  const data = error?.response?.data;
  if (data?.message) return data.message;
  if (typeof data === 'string' && data.trim()) return data;
  if (error?.message) return error.message;
  return '알 수 없는 오류가 발생했습니다.';
}

export const customerApi = {
  list: () => client.get('/customers').then((r) => r.data),
};

export const productApi = {
  list: () => client.get('/products').then((r) => r.data),
};

export const quotationApi = {
  list: () => client.get('/quotations').then((r) => r.data),
  get: (id) => client.get(`/quotations/${id}`).then((r) => r.data),
  create: (body) => client.post('/quotations', body).then((r) => r.data),
  addItem: (id, body) => client.post(`/quotations/${id}/items`, body).then((r) => r.data),
  removeItem: (id, itemId) => client.delete(`/quotations/${id}/items/${itemId}`).then((r) => r.data),
  send: (id) => client.post(`/quotations/${id}/send`).then((r) => r.data),
  suggestPrice: (id, productId) =>
    client.get(`/quotations/${id}/price-suggestion`, { params: { productId } }).then((r) => r.data),
  convert: (id, body) => client.post(`/quotations/${id}/convert`, body ?? {}).then((r) => r.data),
};

export const orderApi = {
  list: () => client.get('/orders').then((r) => r.data),
  get: (id) => client.get(`/orders/${id}`).then((r) => r.data),
  pendingItems: (id) => client.get(`/orders/${id}/pending-items`).then((r) => r.data),
};

export const shipmentApi = {
  list: () => client.get('/shipments').then((r) => r.data),
  get: (id) => client.get(`/shipments/${id}`).then((r) => r.data),
  create: (body) => client.post('/shipments', body).then((r) => r.data),
  confirm: (id, body) => client.post(`/shipments/${id}/confirm`, body ?? {}).then((r) => r.data),
  remove: (id) => client.delete(`/shipments/${id}`).then((r) => r.data),
};

export const invoiceApi = {
  list: () => client.get('/invoices').then((r) => r.data),
  issueCi: (body) => client.post('/invoices/ci', body).then((r) => r.data),
  issuePi: (body) => client.post('/invoices/pi', body).then((r) => r.data),
  registerPayment: (id, body) => client.post(`/invoices/${id}/payments`, body).then((r) => r.data),

  /**
   * PDF 다운로드. 서버가 바이너리를 내려주므로 blob 으로 받아 임시 링크로 저장시킨다.
   */
  downloadPdf: async (id, fileName) => {
    const response = await client.get(`/invoices/${id}/pdf`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName ?? `invoice-${id}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};

export default client;
