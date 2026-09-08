import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SendIcon from '@mui/icons-material/Send';
import DeleteIcon from '@mui/icons-material/Delete';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { customerApi, invoiceApi, productApi, quotationApi, toMessage } from '../api/client';

const statusColors = {
  DRAFT: 'default',
  SENT: 'primary',
  ACCEPTED: 'success',
  REJECTED: 'error',
  EXPIRED: 'warning',
  SUPERSEDED: 'default',
};

const money = (v, digits = 2) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });

export default function QuotationPage() {
  const navigate = useNavigate();

  const [quotations, setQuotations] = useState([]);
  const [detail, setDetail] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({
    customerId: '',
    portOfLoading: 'Busan',
    portOfDischarge: '',
  });

  const [itemOpen, setItemOpen] = useState(false);
  const [itemForm, setItemForm] = useState({ productId: '', qty: '', unitPrice: '' });
  const [suggestion, setSuggestion] = useState(null);

  const [piOpen, setPiOpen] = useState(false);
  const [piSaving, setPiSaving] = useState(false);

  const notify = (message, severity = 'success') => setToast({ message, severity });

  const loadList = useCallback(async (selectId) => {
    const list = await quotationApi.list();
    setQuotations(list);
    const targetId = selectId ?? list[0]?.id;
    setDetail(targetId ? await quotationApi.get(targetId) : null);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const [cs, ps] = await Promise.all([customerApi.list(), productApi.list()]);
        setCustomers(cs);
        setProducts(ps);
        await loadList();
      } catch (e) {
        notify(toMessage(e), 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, [loadList]);

  const selectQuotation = async (id) => {
    try {
      setDetail(await quotationApi.get(id));
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleCreate = async () => {
    try {
      const created = await quotationApi.create({
        customerId: Number(createForm.customerId),
        portOfLoading: createForm.portOfLoading || null,
        portOfDischarge: createForm.portOfDischarge || null,
      });
      setCreateOpen(false);
      setCreateForm({ customerId: '', portOfLoading: 'Busan', portOfDischarge: '' });
      await loadList(created.id);
      notify(`견적 ${created.quoteNo} 을(를) 생성했습니다.`);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  /** 제품을 고르면 서버에서 적용 단가를 받아 입력칸을 미리 채운다 */
  const handleProductPick = async (productId) => {
    setItemForm((prev) => ({ ...prev, productId }));
    setSuggestion(null);
    if (!productId || !detail) return;
    try {
      const s = await quotationApi.suggestPrice(detail.id, productId);
      setSuggestion(s);
      if (s.unitPrice != null) {
        setItemForm((prev) => ({ ...prev, productId, unitPrice: String(s.unitPrice) }));
      }
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleAddItem = async () => {
    try {
      const updated = await quotationApi.addItem(detail.id, {
        productId: Number(itemForm.productId),
        qty: Number(itemForm.qty),
        unitPrice: itemForm.unitPrice === '' ? null : Number(itemForm.unitPrice),
      });
      setDetail(updated);
      setQuotations(await quotationApi.list());
      setItemOpen(false);
      setItemForm({ productId: '', qty: '', unitPrice: '' });
      setSuggestion(null);
      notify('품목을 추가했습니다.');
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleRemoveItem = async (itemId) => {
    try {
      setDetail(await quotationApi.removeItem(detail.id, itemId));
      setQuotations(await quotationApi.list());
      notify('품목을 삭제했습니다.');
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleSend = async () => {
    try {
      const updated = await quotationApi.send(detail.id);
      setDetail(updated);
      setQuotations(await quotationApi.list());
      notify('견적을 발송 상태로 바꿨습니다.');
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  /** 견적 단계 선금 PI 발행. 금액은 서버가 견적 총액 × 거래처 선금 비율로 계산한다. */
  const handleIssuePi = async () => {
    setPiSaving(true);
    try {
      const pi = await invoiceApi.issuePi({ quotationId: detail.id });
      setPiOpen(false);
      setDetail(await quotationApi.get(detail.id));
      notify(`선금 PI ${pi.invoiceNo} 을(를) 발행했습니다. 청구액 ${pi.currency} ${money(pi.amount)}`);
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setPiSaving(false);
    }
  };

  const handleConvert = async () => {
    try {
      const order = await quotationApi.convert(detail.id, {});
      notify(`수주 ${order.orderNo} 로 전환했습니다.`);
      navigate(`/sales-order?orderId=${order.id}`);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  if (loading) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', padding: 6 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  const pickedProduct = products.find((p) => p.id === Number(itemForm.productId));

  return (
    <Layout>
      <Box>
        <Box
          sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 3 }}
        >
          <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
            📄 견적 목록
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            새 견적
          </Button>
        </Box>

        {quotations.length === 0 && <Alert severity="info">등록된 견적이 없습니다. 새 견적을 만들어 보세요.</Alert>}

        <Grid container spacing={2} sx={{ marginBottom: 3 }}>
          {quotations.map((q) => (
            <Grid size={{ xs: 12, md: 6, lg: 4 }} key={q.id}>
              <Card
                sx={{
                  padding: 2,
                  cursor: 'pointer',
                  border: detail?.id === q.id ? '2px solid #1976d2' : '1px solid #ddd',
                  backgroundColor: detail?.id === q.id ? '#f0f8ff' : 'white',
                  '&:hover': { boxShadow: 3 },
                }}
                onClick={() => selectQuotation(q.id)}
              >
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                    {q.quoteNo}
                  </Typography>
                  <Chip label={q.status} color={statusColors[q.status]} size="small" />
                </Box>
                <Typography variant="body2" color="textSecondary" sx={{ marginTop: 1 }}>
                  {q.customerName}
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  {q.currency} {money(q.totalAmount)}
                  {q.converted && ' · 수주 전환됨'}
                </Typography>
              </Card>
            </Grid>
          ))}
        </Grid>

        {detail && (
          <Card sx={{ padding: 3 }}>
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 2,
              }}
            >
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                  {detail.quoteNo}
                </Typography>
                <Typography variant="caption" color="textSecondary">
                  {detail.customerName}
                </Typography>
              </Box>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                <Chip label={detail.status} color={statusColors[detail.status]} size="small" />
                {detail.status === 'DRAFT' && (
                  <Button size="small" startIcon={<SendIcon />} onClick={handleSend}>
                    발송
                  </Button>
                )}
                {detail.piIssuable && (
                  <Button
                    size="small"
                    variant="outlined"
                    startIcon={<ReceiptLongIcon />}
                    onClick={() => setPiOpen(true)}
                  >
                    PI 발행
                  </Button>
                )}
                {detail.status === 'SENT' && !detail.converted && (
                  <Button
                    size="small"
                    variant="contained"
                    startIcon={<SwapHorizIcon />}
                    onClick={handleConvert}
                  >
                    수주 전환
                  </Button>
                )}
              </Stack>
            </Box>

            {detail.converted && (
              <Alert severity="success" sx={{ marginBottom: 2 }}>
                이 견적은 이미 수주로 전환되었습니다.{' '}
                <Button size="small" onClick={() => navigate(`/sales-order?orderId=${detail.convertedOrderId}`)}>
                  수주 보기
                </Button>
              </Alert>
            )}

            {detail.piInvoiceNo && (
              <Alert severity="info" sx={{ marginBottom: 2 }}>
                선금 PI <strong>{detail.piInvoiceNo}</strong> 이(가) 발행되어 있습니다.{' '}
                <Button size="small" onClick={() => navigate('/invoice')}>
                  인보이스 보기
                </Button>
              </Alert>
            )}

            <Grid container spacing={2} sx={{ marginBottom: 3 }}>
              {[
                ['Incoterms', detail.incoterms],
                ['통화', detail.currency],
                ['견적일', detail.quoteDate],
                ['유효기한', detail.validUntil],
                ['선적항', detail.portOfLoading],
                ['도착항', detail.portOfDischarge],
              ].map(([label, value]) => (
                <Grid size={{ xs: 12, sm: 6, md: 4 }} key={label}>
                  <Paper sx={{ padding: 2, backgroundColor: '#f9f9f9' }}>
                    <Typography variant="caption" color="textSecondary">
                      {label}
                    </Typography>
                    <Typography variant="body2">{value || '-'}</Typography>
                  </Paper>
                </Grid>
              ))}
            </Grid>

            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
              품목
            </Typography>
            <TableContainer sx={{ marginBottom: 2 }}>
              <Table size="small">
                <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                  <TableRow>
                    <TableCell align="center">No</TableCell>
                    <TableCell>제품</TableCell>
                    <TableCell align="right">수량</TableCell>
                    <TableCell align="right">단가</TableCell>
                    <TableCell align="right">금액</TableCell>
                    <TableCell align="center" width={50} />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {detail.items.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ color: '#999', padding: 3 }}>
                        품목이 없습니다. 아래 버튼으로 제품을 추가하세요.
                      </TableCell>
                    </TableRow>
                  )}
                  {detail.items.map((item) => (
                    <TableRow key={item.itemId}>
                      <TableCell align="center">{item.lineNo}</TableCell>
                      <TableCell>
                        <Typography variant="body2">{item.productName}</Typography>
                        <Typography variant="caption" color="textSecondary">
                          {item.productCode}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        {money(item.qty, 0)} {item.unit}
                      </TableCell>
                      <TableCell align="right">{money(item.unitPrice)}</TableCell>
                      <TableCell align="right">{money(item.amount)}</TableCell>
                      <TableCell align="center">
                        {detail.editable && (
                          <IconButton size="small" onClick={() => handleRemoveItem(item.itemId)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                  <TableRow sx={{ backgroundColor: '#f9f9f9' }}>
                    <TableCell colSpan={4} align="right">
                      <strong>합계 ({detail.currency})</strong>
                    </TableCell>
                    <TableCell align="right">
                      <strong>{money(detail.totalAmount)}</strong>
                    </TableCell>
                    <TableCell />
                  </TableRow>
                  {detail.krwAmount != null && (
                    <TableRow sx={{ backgroundColor: '#fffacd' }}>
                      <TableCell colSpan={4} align="right">
                        <strong>원화 환산</strong>
                      </TableCell>
                      <TableCell align="right">
                        <strong>₩{money(detail.krwAmount, 0)}</strong>
                      </TableCell>
                      <TableCell />
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>

            {detail.editable && (
              <Button variant="outlined" fullWidth startIcon={<AddIcon />} onClick={() => setItemOpen(true)}>
                제품 추가
              </Button>
            )}
          </Card>
        )}

        {/* 새 견적 */}
        <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>새 견적</DialogTitle>
          <DialogContent>
            <FormControl fullWidth margin="normal">
              <InputLabel>거래처</InputLabel>
              <Select
                value={createForm.customerId}
                label="거래처"
                onChange={(e) => setCreateForm((f) => ({ ...f, customerId: e.target.value }))}
              >
                {customers.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    [{c.customerCode}] {c.nameEn} · {c.defaultCurrency} · 유효 {c.quoteValidDays}일
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              fullWidth
              margin="normal"
              label="선적항 (POL)"
              value={createForm.portOfLoading}
              onChange={(e) => setCreateForm((f) => ({ ...f, portOfLoading: e.target.value }))}
            />
            <TextField
              fullWidth
              margin="normal"
              label="도착항 (POD)"
              value={createForm.portOfDischarge}
              onChange={(e) => setCreateForm((f) => ({ ...f, portOfDischarge: e.target.value }))}
            />
            <Alert severity="info" sx={{ marginTop: 2 }}>
              통화·Incoterms·유효기한은 거래처 기본값이 자동 적용됩니다.
            </Alert>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateOpen(false)}>취소</Button>
            <Button variant="contained" disabled={!createForm.customerId} onClick={handleCreate}>
              생성
            </Button>
          </DialogActions>
        </Dialog>

        {/* 선금 PI 발행 */}
        <Dialog open={piOpen} onClose={() => setPiOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>선금 PI 발행</DialogTitle>
          <DialogContent>
            <Alert severity="info" sx={{ marginBottom: 2 }}>
              바이어가 선금을 보내려면 수주 전에 PI 가 필요합니다. 수주 전환 전에도 발행할 수 있고,
              이 PI 는 수주 취소를 막지 않습니다.
            </Alert>
            {detail && (
              <Table size="small">
                <TableBody>
                  <TableRow>
                    <TableCell>견적</TableCell>
                    <TableCell align="right">{detail.quoteNo}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>거래처</TableCell>
                    <TableCell align="right">{detail.customerName}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>견적 총액</TableCell>
                    <TableCell align="right">
                      {detail.currency} {money(detail.totalAmount)}
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>선금 비율</TableCell>
                    <TableCell align="right">{money(detail.advanceRate, 0)}%</TableCell>
                  </TableRow>
                  <TableRow sx={{ backgroundColor: '#fffacd' }}>
                    <TableCell>
                      <strong>PI 청구액</strong>
                    </TableCell>
                    <TableCell align="right">
                      <strong>
                        {detail.currency}{' '}
                        {money((Number(detail.totalAmount) * Number(detail.advanceRate)) / 100)}
                      </strong>
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>결제기한</TableCell>
                    <TableCell align="right">발행일 + 7일 (선적 전 입금)</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setPiOpen(false)}>취소</Button>
            <Button variant="contained" disabled={piSaving} onClick={handleIssuePi}>
              발행
            </Button>
          </DialogActions>
        </Dialog>

        {/* 제품 추가 */}
        <Dialog open={itemOpen} onClose={() => setItemOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>제품 추가</DialogTitle>
          <DialogContent>
            <FormControl fullWidth margin="normal">
              <InputLabel>제품</InputLabel>
              <Select
                value={itemForm.productId}
                label="제품"
                onChange={(e) => handleProductPick(e.target.value)}
              >
                {products.map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    [{p.productCode}] {p.nameEn} · 재고 {money(p.onHandQty, 0)} {p.unit}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            {suggestion && (
              <Alert severity={suggestion.unitPrice == null ? 'warning' : 'info'} sx={{ marginTop: 1 }}>
                {suggestion.unitPrice == null
                  ? '등록된 단가가 없습니다. 단가를 직접 입력하세요.'
                  : `적용 단가 ${money(suggestion.unitPrice)} ${suggestion.currency} (${
                      suggestion.source === 'CUSTOMER' ? '거래처 전용 단가' : '표준 단가'
                    })`}
              </Alert>
            )}

            <TextField
              fullWidth
              margin="normal"
              label={`수량${pickedProduct ? ` (${pickedProduct.unit})` : ''}`}
              type="number"
              value={itemForm.qty}
              onChange={(e) => setItemForm((f) => ({ ...f, qty: e.target.value }))}
              helperText={
                pickedProduct?.moq != null ? `MOQ ${money(pickedProduct.moq, 0)} ${pickedProduct.unit}` : ' '
              }
            />
            <TextField
              fullWidth
              margin="normal"
              label="단가"
              type="number"
              value={itemForm.unitPrice}
              onChange={(e) => setItemForm((f) => ({ ...f, unitPrice: e.target.value }))}
            />
            {itemForm.qty && itemForm.unitPrice && (
              <Typography variant="body2" sx={{ marginTop: 1 }}>
                금액: <strong>{money(Number(itemForm.qty) * Number(itemForm.unitPrice))}</strong>{' '}
                {detail?.currency}
              </Typography>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setItemOpen(false)}>취소</Button>
            <Button
              variant="contained"
              disabled={!itemForm.productId || !itemForm.qty}
              onClick={handleAddItem}
            >
              추가
            </Button>
          </DialogActions>
        </Dialog>

        <Snackbar
          open={Boolean(toast)}
          autoHideDuration={5000}
          onClose={() => setToast(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity={toast?.severity} onClose={() => setToast(null)}>
            {toast?.message}
          </Alert>
        </Snackbar>
      </Box>
    </Layout>
  );
}
