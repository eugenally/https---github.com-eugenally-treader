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
import DownloadIcon from '@mui/icons-material/Download';
import PaymentsIcon from '@mui/icons-material/Payments';
import AddIcon from '@mui/icons-material/Add';
import Layout from '../components/Layout';
import { invoiceApi, shipmentApi, toMessage } from '../api/client';

const statusColors = {
  ISSUED: 'primary',
  PARTIALLY_PAID: 'warning',
  PAID: 'success',
  OVERDUE: 'error',
  CANCELLED: 'default',
};

const money = (v, digits = 2) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });

const today = () => new Date().toISOString().slice(0, 10);

export default function InvoicePage() {
  const [invoices, setInvoices] = useState([]);
  const [shipments, setShipments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(null);
  const [toast, setToast] = useState(null);

  const [issueOpen, setIssueOpen] = useState(false);
  const [issueShipmentId, setIssueShipmentId] = useState('');

  const [payTarget, setPayTarget] = useState(null);
  const [payForm, setPayForm] = useState({ amount: '', paidDate: today(), method: 'TT', bankRef: '' });

  const notify = (message, severity = 'success') => setToast({ message, severity });

  const load = useCallback(async () => {
    const [inv, sh] = await Promise.all([invoiceApi.list(), shipmentApi.list()]);
    setInvoices(inv);
    setShipments(sh);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        await load();
      } catch (e) {
        notify(toMessage(e), 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, [load]);

  const handleDownload = async (invoice) => {
    setDownloading(invoice.id);
    try {
      await invoiceApi.downloadPdf(invoice.id, `${invoice.invoiceNo}.pdf`);
      notify(`${invoice.invoiceNo}.pdf 를 내려받았습니다.`);
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setDownloading(null);
    }
  };

  const handleIssueCi = async () => {
    try {
      const created = await invoiceApi.issueCi({ shipmentId: Number(issueShipmentId) });
      setIssueOpen(false);
      setIssueShipmentId('');
      await load();
      notify(`CI ${created.invoiceNo} 을(를) 발행했습니다.`);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const openPayment = (invoice) => {
    setPayTarget(invoice);
    setPayForm({ amount: String(invoice.balance), paidDate: today(), method: 'TT', bankRef: '' });
  };

  const handlePayment = async () => {
    try {
      const updated = await invoiceApi.registerPayment(payTarget.id, {
        amount: Number(payForm.amount),
        paidDate: payForm.paidDate || null,
        method: payForm.method || null,
        bankRef: payForm.bankRef || null,
      });
      setPayTarget(null);
      await load();
      notify(`입금 등록 완료. 상태가 ${updated.status} 로 바뀌었습니다.`);
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

  // CI 는 확정된 출하 1건당 1장이므로, 아직 발행 안 된 SHIPPED 출하만 후보로 보여준다
  const ciCandidates = shipments.filter(
    (s) => (s.status === 'SHIPPED' || s.status === 'ARRIVED') && !s.invoiced
  );

  return (
    <Layout>
      <Box>
        <Box
          sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 3 }}
        >
          <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
            🧾 인보이스 목록
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            disabled={ciCandidates.length === 0}
            onClick={() => setIssueOpen(true)}
          >
            CI 발행 ({ciCandidates.length})
          </Button>
        </Box>

        {invoices.length === 0 && (
          <Alert severity="info">
            발행된 인보이스가 없습니다. 출하를 확정한 뒤 CI 를 발행하세요.
          </Alert>
        )}

        {invoices.map((inv) => (
          <Card sx={{ padding: 3, marginBottom: 3 }} key={inv.id}>
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
                  {inv.invoiceNo}
                </Typography>
                <Typography variant="caption" color="textSecondary">
                  {inv.invoiceType === 'CI' ? 'Commercial Invoice' : 'Proforma Invoice'}
                  {inv.orderNo && ` · ${inv.orderNo}`}
                  {inv.shipmentNo && ` · ${inv.shipmentNo}`}
                </Typography>
              </Box>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                <Chip label={inv.status} color={statusColors[inv.status]} />
                {inv.status !== 'PAID' && inv.status !== 'CANCELLED' && (
                  <Button size="small" startIcon={<PaymentsIcon />} onClick={() => openPayment(inv)}>
                    입금 등록
                  </Button>
                )}
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={
                    downloading === inv.id ? <CircularProgress size={16} /> : <DownloadIcon />
                  }
                  disabled={downloading === inv.id}
                  onClick={() => handleDownload(inv)}
                >
                  PDF
                </Button>
              </Stack>
            </Box>

            <Grid container spacing={2} sx={{ marginBottom: 2 }}>
              {[
                ['거래처', inv.customerName],
                ['발행일', inv.issueDate],
                ['결제기한', inv.dueDate],
                [
                  '적용 환율',
                  inv.exchangeRate != null
                    ? `${money(inv.exchangeRate)} (${inv.rateBaseDate} 기준)`
                    : '미적용',
                ],
              ].map(([label, value]) => (
                <Grid size={{ xs: 12, sm: 6, md: 3 }} key={label}>
                  <Paper sx={{ padding: 2, backgroundColor: '#f9f9f9' }}>
                    <Typography variant="caption" color="textSecondary">
                      {label}
                    </Typography>
                    <Typography variant="body2">{value || '-'}</Typography>
                  </Paper>
                </Grid>
              ))}
            </Grid>

            <TableContainer>
              <Table size="small">
                <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                  <TableRow>
                    <TableCell>구분</TableCell>
                    <TableCell align="right">금액 ({inv.currency})</TableCell>
                    <TableCell align="right">원화 환산 (KRW)</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow>
                    <TableCell>청구액</TableCell>
                    <TableCell align="right">{money(inv.amount)}</TableCell>
                    <TableCell align="right">
                      {inv.krwAmount != null ? `₩${money(inv.krwAmount, 0)}` : '-'}
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>입금액</TableCell>
                    <TableCell align="right" sx={{ color: '#2f9e44' }}>
                      {money(inv.paidAmount)}
                    </TableCell>
                    <TableCell align="right">-</TableCell>
                  </TableRow>
                  <TableRow sx={{ backgroundColor: '#fffacd' }}>
                    <TableCell>
                      <strong>미수 잔액</strong>
                    </TableCell>
                    <TableCell align="right">
                      <strong>{money(inv.balance)}</strong>
                    </TableCell>
                    <TableCell align="right">-</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </TableContainer>

            {inv.payments.length > 0 && (
              <Box sx={{ marginTop: 2 }}>
                <Typography variant="caption" color="textSecondary">
                  입금 내역
                </Typography>
                {inv.payments.map((p) => (
                  <Typography key={p.id} variant="body2" sx={{ marginTop: 0.5 }}>
                    {p.paidDate} · {money(p.amount)} {inv.currency} · {p.method || '-'}
                    {p.bankRef && ` · ${p.bankRef}`}
                  </Typography>
                ))}
              </Box>
            )}
          </Card>
        ))}

        {/* CI 발행 */}
        <Dialog open={issueOpen} onClose={() => setIssueOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>CI 발행</DialogTitle>
          <DialogContent>
            <Alert severity="info" sx={{ marginBottom: 2 }}>
              출하 1건당 CI 1장입니다. 금액은 실제 선적 수량 × 수주 단가로 계산되고, 결제기한은 B/L
              date + 신용일수로 잡힙니다.
            </Alert>
            <FormControl fullWidth margin="normal">
              <InputLabel>확정된 출하</InputLabel>
              <Select
                value={issueShipmentId}
                label="확정된 출하"
                onChange={(e) => setIssueShipmentId(e.target.value)}
              >
                {ciCandidates.map((s) => (
                  <MenuItem key={s.id} value={s.id}>
                    {s.shipmentNo} · {s.orderNo} · {s.customerName} · B/L {s.blNo || '-'}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setIssueOpen(false)}>취소</Button>
            <Button variant="contained" disabled={!issueShipmentId} onClick={handleIssueCi}>
              발행
            </Button>
          </DialogActions>
        </Dialog>

        {/* 입금 등록 */}
        <Dialog open={Boolean(payTarget)} onClose={() => setPayTarget(null)} maxWidth="sm" fullWidth>
          <DialogTitle>입금 등록 — {payTarget?.invoiceNo}</DialogTitle>
          <DialogContent>
            <Alert severity="info" sx={{ marginBottom: 2 }}>
              미수 잔액: {payTarget?.currency} {money(payTarget?.balance)}
            </Alert>
            <TextField
              fullWidth
              margin="normal"
              label="입금액"
              type="number"
              value={payForm.amount}
              onChange={(e) => setPayForm((f) => ({ ...f, amount: e.target.value }))}
            />
            <TextField
              fullWidth
              margin="normal"
              label="입금일"
              type="date"
              value={payForm.paidDate}
              onChange={(e) => setPayForm((f) => ({ ...f, paidDate: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <FormControl fullWidth margin="normal">
              <InputLabel>결제 수단</InputLabel>
              <Select
                value={payForm.method}
                label="결제 수단"
                onChange={(e) => setPayForm((f) => ({ ...f, method: e.target.value }))}
              >
                <MenuItem value="TT">T/T 송금</MenuItem>
                <MenuItem value="LC">L/C 네고</MenuItem>
                <MenuItem value="CHECK">수표</MenuItem>
              </Select>
            </FormControl>
            <TextField
              fullWidth
              margin="normal"
              label="은행 참조번호"
              value={payForm.bankRef}
              onChange={(e) => setPayForm((f) => ({ ...f, bankRef: e.target.value }))}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setPayTarget(null)}>취소</Button>
            <Button variant="contained" disabled={!payForm.amount} onClick={handlePayment}>
              등록
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
