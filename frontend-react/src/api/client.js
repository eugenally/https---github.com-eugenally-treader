import axios from 'axios';

/**
 * 백엔드 REST API 클라이언트.
 * Vite dev 서버가 /api 를 localhost:8080 으로 프록시하므로 baseURL 은 상대경로로 둔다.
 */
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

const TOKEN_KEY = 'treader.accessToken';
const MEMBER_KEY = 'treader.member';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  getMember: () => {
    try {
      const raw = localStorage.getItem(MEMBER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },
  set: (token, member) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(MEMBER_KEY, JSON.stringify(member));
  },
  setMember: (member) => localStorage.setItem(MEMBER_KEY, JSON.stringify(member)),
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(MEMBER_KEY);
  },
};

/** 모든 요청에 Bearer 토큰을 붙인다 */
client.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 토큰이 만료되면(30분) 서버가 401 을 준다. 저장된 토큰을 버리고 로그인 화면으로 보낸다.
 * 로그인·가입 요청 자체의 401 은 화면이 직접 메시지를 보여야 하므로 건드리지 않는다.
 */
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url ?? '';
    const isAuthCall = url.startsWith('/auth/');

    if (status === 401 && !isAuthCall) {
      tokenStore.clear();
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login?expired=1';
      }
    }
    return Promise.reject(error);
  }
);

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

export const authApi = {
  checkLoginId: (loginId) =>
    client.get('/auth/check-login-id', { params: { loginId } }).then((r) => r.data),
  checkEmail: (email) => client.get('/auth/check-email', { params: { email } }).then((r) => r.data),
  signUp: (body) => client.post('/auth/signup', body).then((r) => r.data),
  verifyEmail: (token) =>
    client.post('/auth/verify-email', null, { params: { token } }).then((r) => r.data),
  resendVerification: (email) =>
    client.post('/auth/resend-verification', null, { params: { email } }).then((r) => r.data),
  login: async (body) => {
    const data = await client.post('/auth/login', body).then((r) => r.data);
    tokenStore.set(data.accessToken, data.member);
    return data;
  },
  logout: async () => {
    try {
      await client.post('/auth/logout');
    } finally {
      // 서버가 실패해도 로컬 토큰은 반드시 버린다
      tokenStore.clear();
    }
  },
  findPassword: (body) => client.post('/auth/find-password', body).then((r) => r.data),
};

export const memberApi = {
  me: () => client.get('/members/me').then((r) => r.data),
  updateMe: async (body) => {
    const data = await client.put('/members/me', body).then((r) => r.data);
    tokenStore.setMember(data);
    return data;
  },
  changePassword: (body) => client.put('/members/me/password', body).then((r) => r.data),
  list: () => client.get('/members').then((r) => r.data),
};

export const boardApi = {
  boards: () => client.get('/boards').then((r) => r.data),
  posts: (boardCode, { page = 1, type, keyword } = {}) =>
    client
      .get(`/boards/${boardCode}/posts`, { params: { page, type, keyword: keyword || undefined } })
      .then((r) => r.data),
  post: (postId) => client.get(`/boards/posts/${postId}`).then((r) => r.data),
  create: (boardCode, body) => client.post(`/boards/${boardCode}/posts`, body).then((r) => r.data),
  update: (postId, body) => client.put(`/boards/posts/${postId}`, body).then((r) => r.data),
  remove: (postId, guestPwd) =>
    client.delete(`/boards/posts/${postId}`, { params: { guestPwd: guestPwd || undefined } }),
  verifyGuestPassword: (postId, guestPwd) =>
    client.post(`/boards/posts/${postId}/verify-password`, { guestPwd }).then((r) => r.data),
  markAnswered: (postId, value) =>
    client.post(`/boards/posts/${postId}/answered`, null, { params: { value } }).then((r) => r.data),

  addComment: (postId, body) =>
    client.post(`/boards/posts/${postId}/comments`, body).then((r) => r.data),
  updateComment: (commentId, body) =>
    client.put(`/boards/comments/${commentId}`, body).then((r) => r.data),
  deleteComment: (commentId) => client.delete(`/boards/comments/${commentId}`).then((r) => r.data),

  toggleLike: (postId) => client.post(`/boards/posts/${postId}/like`).then((r) => r.data),

  uploadFiles: (postId, files) => {
    const form = new FormData();
    files.forEach((f) => form.append('files', f));
    // Content-Type 은 브라우저가 boundary 와 함께 직접 정해야 한다. 여기서 지정하면 깨진다.
    return client
      .post(`/boards/posts/${postId}/files`, form, { headers: { 'Content-Type': undefined } })
      .then((r) => r.data);
  },
  deleteFile: (fileId) => client.delete(`/boards/files/${fileId}`),
  downloadFile: async (fileId, fileName) => {
    const response = await client.get(`/boards/files/${fileId}`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName ?? `file-${fileId}`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};

export const customerApi = {
  /** 선택 드롭다운용 — 활성 거래처 전체 (페이징 없음) */
  list: () => client.get('/customers/active').then((r) => r.data),
  search: ({ page = 1, size = 10, keyword, status } = {}) =>
    client
      .get('/customers', { params: { page, size, keyword: keyword || undefined, status: status || undefined } })
      .then((r) => r.data),
  get: (id) => client.get(`/customers/${id}`).then((r) => r.data),
  create: (body) => client.post('/customers', body).then((r) => r.data),
  update: (id, body) => client.put(`/customers/${id}`, body).then((r) => r.data),
  remove: (id) => client.delete(`/customers/${id}`).then((r) => r.data),
  changeStatus: (id, active) =>
    client.post(`/customers/${id}/status`, null, { params: { active } }).then((r) => r.data),
};

export const productApi = {
  /** 선택 드롭다운용 — 활성 제품 전체 (페이징 없음) */
  list: () => client.get('/products/active').then((r) => r.data),
  search: ({ page = 1, size = 10, keyword, status } = {}) =>
    client
      .get('/products', { params: { page, size, keyword: keyword || undefined, status: status || undefined } })
      .then((r) => r.data),
  get: (id) => client.get(`/products/${id}`).then((r) => r.data),
  create: (body) => client.post('/products', body).then((r) => r.data),
  update: (id, body) => client.put(`/products/${id}`, body).then((r) => r.data),
  remove: (id) => client.delete(`/products/${id}`).then((r) => r.data),
  changeStatus: (id, active) =>
    client.post(`/products/${id}/status`, null, { params: { active } }).then((r) => r.data),

  stocks: (keyword) =>
    client.get('/products/stocks', { params: { keyword: keyword || undefined } }).then((r) => r.data),
  adjustStock: (id, body) =>
    client.post(`/products/${id}/stock-adjust`, body).then((r) => r.data),
};

/** 바이너리 응답을 파일로 저장시킨다. PDF·엑셀이 공유한다. */
async function downloadBinary(url, fileName, config = {}) {
  const response = await client.get(url, { ...config, responseType: 'blob' });
  const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}

export const excelApi = {
  productTemplate: () => downloadBinary('/excel/products/template', '제품등록양식.xlsx'),
  priceTemplate: () => downloadBinary('/excel/prices/template', '단가등록양식.xlsx'),
  exportProducts: () => downloadBinary('/excel/products/export', '제품목록.xlsx'),
  exportPrices: () => downloadBinary('/excel/prices/export', '단가목록.xlsx'),

  importProducts: (file) => uploadSingle('/excel/products/import', file),
  importPrices: (file) => uploadSingle('/excel/prices/import', file),

  /** 실패 행만 엑셀로 받아 그 행만 고쳐 재업로드할 수 있게 한다 */
  exportErrors: async (errors) => {
    const form = new FormData();
    form.append(
      'errors',
      new Blob([JSON.stringify(errors)], { type: 'application/json' }),
      'errors.json'
    );
    const response = await client.post('/excel/errors/export', form, {
      headers: { 'Content-Type': undefined },
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.download = '업로드오류.xlsx';
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};

function uploadSingle(url, file) {
  const form = new FormData();
  form.append('file', file);
  // Content-Type 은 브라우저가 boundary 와 함께 정해야 한다. 여기서 지정하면 깨진다.
  return client.post(url, form, { headers: { 'Content-Type': undefined } }).then((r) => r.data);
}

export const attachmentApi = {
  list: (refType, refId) =>
    client.get('/attachments', { params: { refType, refId } }).then((r) => r.data),
  upload: (refType, refId, docType, files) => {
    const form = new FormData();
    files.forEach((f) => form.append('files', f));
    return client
      .post('/attachments', form, {
        params: { refType, refId, docType: docType || undefined },
        headers: { 'Content-Type': undefined },
      })
      .then((r) => r.data);
  },
  download: (id, fileName) => downloadBinary(`/attachments/${id}`, fileName),
  remove: (id) => client.delete(`/attachments/${id}`),
};

export const documentApi = {
  quotationPdf: (id, quoteNo) => downloadBinary(`/quotations/${id}/pdf`, `${quoteNo}.pdf`),
  packingListPdf: (id, shipmentNo) =>
    downloadBinary(`/shipments/${id}/packing-list`, `PL-${shipmentNo}.pdf`),
  invoicePdf: (id, invoiceNo) => downloadBinary(`/invoices/${id}/pdf`, `${invoiceNo}.pdf`),
};

export const priceApi = {
  list: ({ productId, customerId } = {}) =>
    client
      .get('/prices', { params: { productId: productId || undefined, customerId: customerId || undefined } })
      .then((r) => r.data),
  resolve: ({ productId, customerId, baseDate }) =>
    client
      .get('/prices/resolve', { params: { productId, customerId: customerId || undefined, baseDate } })
      .then((r) => r.data),
  create: (body) => client.post('/prices', body).then((r) => r.data),
  update: (id, body) => client.put(`/prices/${id}`, body).then((r) => r.data),
  remove: (id) => client.delete(`/prices/${id}`),
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
