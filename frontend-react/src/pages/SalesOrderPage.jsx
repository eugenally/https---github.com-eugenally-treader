import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Grid,
  LinearProgress,
  Paper,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { orderApi, toMessage } from '../api/client';

const statusColors = {
  CONFIRMED: 'info',
  IN_PRODUCTION: 'warning',
  PARTIALLY_SHIPPED: 'primary',
  SHIPPED: 'success',
  CLOSED: 'success',
  CANCELLED: 'default',
};

const shipmentColors = {
  PLANNED: 'default',
  PACKED: 'warning',
  SHIPPED: 'success',
  ARRIVED: 'info',
  CANCELLED: 'error',
};

const money = (v, digits = 2) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });

export default function SalesOrderPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [orders, setOrders] = useState([]);
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const notify = (message, severity = 'success') => setToast({ message, severity });

  const load = useCallback(async (selectId) => {
    const list = await orderApi.list();
    setOrders(list);
    const targetId = selectId ?? list[0]?.id;
    setDetail(targetId ? await orderApi.get(targetId) : null);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const requested = searchParams.get('orderId');
        await load(requested ? Number(requested) : undefined);
      } catch (e) {
        notify(toMessage(e), 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, [load, searchParams]);

  const selectOrder = async (id) => {
    try {
      setDetail(await orderApi.get(id));
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

  const hasPending = detail?.items.some((i) => Number(i.pendingQty) > 0);

  return (
    <Layout>
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
          📦 수주 목록
        </Typography>

        {orders.length === 0 && (
          <Alert severity="info">
            등록된 수주가 없습니다. 견적 화면에서 발송된 견적을 수주로 전환하세요.
          </Alert>
        )}

        <Grid container spacing={2} sx={{ marginBottom: 3 }}>
          {orders.map((o) => (
            <Grid size={{ xs: 12, md: 6, lg: 4 }} key={o.id}>
              <Card
                sx={{
                  padding: 2,
                  cursor: 'pointer',
                  border: detail?.id === o.id ? '2px solid #1976d2' : '1px solid #ddd',
                  backgroundColor: detail?.id === o.id ? '#f0f8ff' : 'white',
                  '&:hover': { boxShadow: 2 },
                }}
                onClick={() => selectOrder(o.id)}
              >
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                    {o.orderNo}
                  </Typography>
                  <Chip label={o.status} size="small" color={statusColors[o.status]} />
                </Box>
                <Typography variant="caption" color="textSecondary" sx={{ display: 'block', marginTop: 1 }}>
                  {o.customerName}
                </Typography>
                <Typography variant="caption" color="textSecondary" sx={{ display: 'block' }}>
                  {o.currency} {money(o.totalAmount)}
                </Typography>
              </Card>
            </Grid>
          ))}
        </Grid>

        {detail && (
          <>
            <Card sx={{ padding: 3, marginBottom: 3 }}>
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
                    {detail.orderNo}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {detail.customerName}
                    {detail.quoteNo && ` · 견적 ${detail.quoteNo}`}
                  </Typography>
                </Box>
                <Chip label={detail.status} color={statusColors[detail.status]} />
              </Box>

              <Grid container spacing={2} sx={{ marginBottom: 3 }}>
                {[
                  ['주문일', detail.orderDate],
                  ['납기일', detail.requiredDate],
                  ['Incoterms', detail.incoterms],
                  ['통화', detail.currency],
                  ['선적항', detail.portOfLoading],
                  ['도착항', detail.portOfDischarge],
                  ['PO No.', detail.poNo],
                  ['취소 가능', detail.cancellable ? '가능' : '불가 (출하·인보이스 존재)'],
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

              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                품목별 진행 현황
              </Typography>
              {detail.items.map((item) => (
                <Box
                  key={item.itemId}
                  sx={{ marginBottom: 2, padding: 2, backgroundColor: '#fafafa', borderRadius: 1 }}
                >
                  <Box
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginBottom: 1,
                    }}
                  >
                    <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                      {item.productName} ({item.productCode})
                    </Typography>
                    <Chip
                      label={`${item.progressPercent}% 출하`}
                      size="small"
                      color={item.progressPercent === 100 ? 'success' : 'primary'}
                    />
                  </Box>
                  <Grid container spacing={1}>
                    <Grid size={{ xs: 6, sm: 3 }}>
                      <Typography variant="caption" color="textSecondary">
                        주문량: <strong>{money(item.orderedQty, 0)}</strong> {item.unit}
                      </Typography>
                    </Grid>
                    <Grid size={{ xs: 6, sm: 3 }}>
                      <Typography variant="caption" color="textSecondary">
                        기출하: <strong>{money(item.shippedQty, 0)}</strong>
                      </Typography>
                    </Grid>
                    <Grid size={{ xs: 6, sm: 3 }}>
                      <Typography variant="caption" color="textSecondary">
                        잔량: <strong>{money(item.pendingQty, 0)}</strong>
                      </Typography>
                    </Grid>
                    <Grid size={{ xs: 6, sm: 3 }}>
                      <Typography variant="caption" color="textSecondary">
                        금액: <strong>{money(item.amount)}</strong>
                      </Typography>
                    </Grid>
                  </Grid>
                  <LinearProgress
                    variant="determinate"
                    value={item.progressPercent}
                    sx={{ marginTop: 1, height: 6, borderRadius: 1 }}
                  />
                </Box>
              ))}

              <Box sx={{ padding: 2, backgroundColor: '#fffacd', borderRadius: 1, marginTop: 2 }}>
                <Typography variant="body2" sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <strong>총 주문액</strong>
                  <strong>
                    {detail.currency} {money(detail.totalAmount)}
                  </strong>
                </Typography>
              </Box>
            </Card>

            <Card sx={{ padding: 3 }}>
              <Typography variant="h6" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                <LocalShippingIcon sx={{ marginRight: 1, verticalAlign: 'middle' }} />
                출하 이력
              </Typography>

              {detail.shipments.length === 0 ? (
                <Alert severity="info">아직 출하 이력이 없습니다.</Alert>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                      <TableRow>
                        <TableCell>출하번호</TableCell>
                        <TableCell>상태</TableCell>
                        <TableCell>운송</TableCell>
                        <TableCell>ETD</TableCell>
                        <TableCell>선적일</TableCell>
                        <TableCell>B/L No.</TableCell>
                        <TableCell align="right">총중량(kg)</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {detail.shipments.map((s) => (
                        <TableRow key={s.shipmentId}>
                          <TableCell>{s.shipmentNo}</TableCell>
                          <TableCell>
                            <Chip label={s.status} size="small" color={shipmentColors[s.status]} />
                          </TableCell>
                          <TableCell>{s.transportMode}</TableCell>
                          <TableCell>{s.etd || '-'}</TableCell>
                          <TableCell>{s.shipDate || '-'}</TableCell>
                          <TableCell>{s.blNo || '-'}</TableCell>
                          <TableCell align="right">{money(s.totalGrossWeight, 1)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Card>

            <Stack direction="row" spacing={2} sx={{ marginTop: 3, justifyContent: 'flex-end' }}>
              <Button
                variant="outlined"
                startIcon={<ReceiptLongIcon />}
                onClick={() => navigate('/invoice')}
              >
                인보이스 보기
              </Button>
              <Button
                variant="contained"
                startIcon={<LocalShippingIcon />}
                disabled={!hasPending}
                onClick={() => navigate(`/shipment?orderId=${detail.id}`)}
              >
                {hasPending ? '출하 등록' : '잔량 없음'}
              </Button>
            </Stack>
          </>
        )}

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
