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
import SaveIcon from '@mui/icons-material/Save';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import DeleteIcon from '@mui/icons-material/Delete';
import DownloadIcon from '@mui/icons-material/Download';
import { useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { documentApi, orderApi, shipmentApi, toMessage } from '../api/client';

const statusColors = {
  PLANNED: 'default',
  PACKED: 'warning',
  SHIPPED: 'success',
  ARRIVED: 'info',
  CANCELLED: 'error',
};

const money = (v, digits = 2) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });

const today = () => new Date().toISOString().slice(0, 10);

export default function ShipmentPage() {
  const [searchParams] = useSearchParams();

  const [orders, setOrders] = useState([]);
  const [shipments, setShipments] = useState([]);
  const [orderId, setOrderId] = useState('');
  const [pendingItems, setPendingItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const [form, setForm] = useState({
    transportMode: 'OCEAN',
    etd: today(),
    containerType: '20HQ',
    containerNo: '',
    vesselName: '',
  });
  const [qtyMap, setQtyMap] = useState({});

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [confirmForm, setConfirmForm] = useState({ blNo: '', awbNo: '', shipDate: today() });

  const notify = (message, severity = 'success') => setToast({ message, severity });

  const loadShipments = useCallback(async () => {
    setShipments(await shipmentApi.list());
  }, []);

  const loadPending = useCallback(async (id) => {
    if (!id) {
      setPendingItems([]);
      return;
    }
    const items = await orderApi.pendingItems(id);
    setPendingItems(items);
    // 기본값으로 잔량 전부를 채워둔다. 부분 출하하려면 사용자가 줄이면 된다.
    setQtyMap(Object.fromEntries(items.map((i) => [i.orderItemId, String(i.pendingQty)])));
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const [orderList] = await Promise.all([orderApi.list(), loadShipments()]);
        setOrders(orderList);

        const requested = searchParams.get('orderId');
        const initial = requested ? Number(requested) : orderList[0]?.id;
        if (initial) {
          setOrderId(initial);
          await loadPending(initial);
        }
      } catch (e) {
        notify(toMessage(e), 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, [loadPending, loadShipments, searchParams]);

  const handleOrderChange = async (id) => {
    setOrderId(id);
    try {
      await loadPending(id);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleCreate = async () => {
    const items = pendingItems
      .map((i) => ({ orderItemId: i.orderItemId, qty: Number(qtyMap[i.orderItemId] || 0), meta: i }))
      .filter((i) => i.qty > 0);

    if (items.length === 0) {
      notify('출하할 수량을 입력하세요.', 'warning');
      return;
    }

    const over = items.find((i) => i.qty > Number(i.meta.pendingQty));
    if (over) {
      notify(`${over.meta.productCode}: 출하 수량이 잔량 ${over.meta.pendingQty} 을 초과합니다.`, 'error');
      return;
    }

    setSaving(true);
    try {
      const created = await shipmentApi.create({
        orderId: Number(orderId),
        transportMode: form.transportMode,
        etd: form.etd || null,
        containerType: form.containerType || null,
        containerNo: form.containerNo || null,
        vesselName: form.vesselName || null,
        items: items.map((i) => ({
          orderItemId: i.orderItemId,
          qty: i.qty,
          netWeight: i.meta.netWeightPerUnit != null ? i.qty * Number(i.meta.netWeightPerUnit) : null,
          grossWeight: i.meta.grossWeightPerUnit != null ? i.qty * Number(i.meta.grossWeightPerUnit) : null,
        })),
      });
      await Promise.all([loadShipments(), loadPending(orderId)]);
      notify(`출하 ${created.shipmentNo} 을(를) 등록했습니다. (PLANNED — 재고는 아직 차감되지 않음)`);
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setSaving(false);
    }
  };

  const openConfirm = (shipment) => {
    setConfirmTarget(shipment);
    setConfirmForm({ blNo: '', awbNo: '', shipDate: shipment.etd || today() });
  };

  const handleConfirm = async () => {
    try {
      const result = await shipmentApi.confirm(confirmTarget.id, {
        blNo: confirmForm.blNo || null,
        awbNo: confirmForm.awbNo || null,
        shipDate: confirmForm.shipDate || null,
      });
      setConfirmTarget(null);
      await Promise.all([loadShipments(), loadPending(orderId)]);
      notify(`출하 ${result.shipmentNo} 확정 완료. 재고가 차감되었습니다.`);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleDelete = async (shipment) => {
    try {
      await shipmentApi.remove(shipment.id);
      await Promise.all([loadShipments(), loadPending(orderId)]);
      notify(`출하 ${shipment.shipmentNo} 을(를) 삭제했습니다.`);
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

  const isAir = form.transportMode === 'AIR';

  return (
    <Layout>
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 3 }}>
          🚚 출하 등록
        </Typography>

        <Card sx={{ padding: 3, marginBottom: 3 }}>
          <FormControl fullWidth sx={{ marginBottom: 2 }}>
            <InputLabel>수주 선택</InputLabel>
            <Select value={orderId} label="수주 선택" onChange={(e) => handleOrderChange(e.target.value)}>
              {orders.map((o) => (
                <MenuItem key={o.id} value={o.id}>
                  {o.orderNo} · {o.customerName} · {o.status}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {pendingItems.length === 0 ? (
            <Alert severity="success">이 수주는 미출하 잔량이 없습니다.</Alert>
          ) : (
            <>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
                출하 기본정보
              </Typography>
              <Grid container spacing={2} sx={{ marginBottom: 3 }}>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <FormControl fullWidth>
                    <InputLabel>운송 수단</InputLabel>
                    <Select
                      value={form.transportMode}
                      label="운송 수단"
                      onChange={(e) => setForm((f) => ({ ...f, transportMode: e.target.value }))}
                    >
                      <MenuItem value="OCEAN">해상 (Ocean)</MenuItem>
                      <MenuItem value="AIR">항공 (Air)</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    label="예상 선적일 (ETD)"
                    type="date"
                    value={form.etd}
                    onChange={(e) => setForm((f) => ({ ...f, etd: e.target.value }))}
                    slotProps={{ inputLabel: { shrink: true } }}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  {isAir ? (
                    <TextField
                      fullWidth
                      label="항공편명"
                      value={form.vesselName}
                      onChange={(e) => setForm((f) => ({ ...f, vesselName: e.target.value }))}
                    />
                  ) : (
                    <FormControl fullWidth>
                      <InputLabel>컨테이너 타입</InputLabel>
                      <Select
                        value={form.containerType}
                        label="컨테이너 타입"
                        onChange={(e) => setForm((f) => ({ ...f, containerType: e.target.value }))}
                      >
                        <MenuItem value="20HQ">20HQ</MenuItem>
                        <MenuItem value="40HQ">40HQ</MenuItem>
                        <MenuItem value="40HC">40HC</MenuItem>
                        <MenuItem value="LCL">LCL</MenuItem>
                      </Select>
                    </FormControl>
                  )}
                </Grid>
              </Grid>

              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
                출하 품목 (미출하 잔량만 표시)
              </Typography>
              <TableContainer sx={{ marginBottom: 2 }}>
                <Table size="small">
                  <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                    <TableRow>
                      <TableCell align="center">No</TableCell>
                      <TableCell>제품</TableCell>
                      <TableCell align="right">주문량</TableCell>
                      <TableCell align="right">기출하</TableCell>
                      <TableCell align="right">잔량</TableCell>
                      <TableCell align="right">현재고</TableCell>
                      <TableCell align="right">금회 출하량</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {pendingItems.map((item) => {
                      const qty = Number(qtyMap[item.orderItemId] || 0);
                      const overPending = qty > Number(item.pendingQty);
                      const overStock = item.onHandQty != null && qty > Number(item.onHandQty);
                      return (
                        <TableRow key={item.orderItemId}>
                          <TableCell align="center">{item.lineNo}</TableCell>
                          <TableCell>
                            <Typography variant="body2">{item.productName}</Typography>
                            <Typography variant="caption" color="textSecondary">
                              {item.productCode}
                            </Typography>
                          </TableCell>
                          <TableCell align="right">{money(item.orderedQty, 0)}</TableCell>
                          <TableCell align="right">{money(item.shippedQty, 0)}</TableCell>
                          <TableCell align="right" sx={{ color: '#d32f2f', fontWeight: 'bold' }}>
                            {money(item.pendingQty, 0)}
                          </TableCell>
                          <TableCell align="right" sx={{ color: overStock ? '#d32f2f' : 'inherit' }}>
                            {money(item.onHandQty, 0)}
                          </TableCell>
                          <TableCell align="right">
                            <TextField
                              type="number"
                              size="small"
                              value={qtyMap[item.orderItemId] ?? ''}
                              error={overPending}
                              helperText={
                                overPending ? '잔량 초과' : overStock ? '재고 부족 가능' : ' '
                              }
                              onChange={(e) =>
                                setQtyMap((m) => ({ ...m, [item.orderItemId]: e.target.value }))
                              }
                              sx={{ width: 120 }}
                            />
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>

              <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  startIcon={<SaveIcon />}
                  disabled={saving}
                  onClick={handleCreate}
                >
                  출하 등록 (PLANNED)
                </Button>
              </Stack>
              <Alert severity="info" sx={{ marginTop: 2 }}>
                등록 시점에는 재고가 움직이지 않습니다. 아래 목록에서 <strong>확정</strong>해야
                <code> SELECT FOR UPDATE </code>로 행을 잠그고 실물 재고를 차감합니다.
              </Alert>
            </>
          )}
        </Card>

        <Card sx={{ padding: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
            출하 목록
          </Typography>
          <TableContainer>
            <Table size="small">
              <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                <TableRow>
                  <TableCell>출하번호</TableCell>
                  <TableCell>수주</TableCell>
                  <TableCell>거래처</TableCell>
                  <TableCell>상태</TableCell>
                  <TableCell>ETD</TableCell>
                  <TableCell>B/L No.</TableCell>
                  <TableCell align="right">중량(kg)</TableCell>
                  <TableCell align="center">작업</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {shipments.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ color: '#999', padding: 3 }}>
                      등록된 출하가 없습니다.
                    </TableCell>
                  </TableRow>
                )}
                {shipments.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell>{s.shipmentNo}</TableCell>
                    <TableCell>{s.orderNo}</TableCell>
                    <TableCell>{s.customerName}</TableCell>
                    <TableCell>
                      <Chip label={s.status} size="small" color={statusColors[s.status]} />
                    </TableCell>
                    <TableCell>{s.etd || '-'}</TableCell>
                    <TableCell>{s.blNo || '-'}</TableCell>
                    <TableCell align="right">{money(s.totalGrossWeight, 1)}</TableCell>
                    <TableCell align="center">
                      {s.status === 'PLANNED' ? (
                        <Stack direction="row" spacing={1} sx={{ justifyContent: 'center' }}>
                          <Button
                            size="small"
                            variant="contained"
                            startIcon={<CheckCircleIcon />}
                            onClick={() => openConfirm(s)}
                          >
                            확정
                          </Button>
                          <Button
                            size="small"
                            color="error"
                            startIcon={<DeleteIcon />}
                            onClick={() => handleDelete(s)}
                          >
                            삭제
                          </Button>
                        </Stack>
                      ) : (
                        <Stack direction="row" spacing={1} sx={{ justifyContent: 'center', alignItems: 'center' }}>
                          <Typography variant="caption" color="textSecondary">
                            {s.invoiced ? 'CI 발행됨' : '확정됨'}
                          </Typography>
                          <Button
                            size="small"
                            startIcon={<DownloadIcon />}
                            onClick={() =>
                              documentApi
                                .packingListPdf(s.id, s.shipmentNo)
                                .catch((e) => notify(toMessage(e), 'error'))
                            }
                          >
                            PL
                          </Button>
                        </Stack>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>

        {/* 출하 확정 */}
        <Dialog open={Boolean(confirmTarget)} onClose={() => setConfirmTarget(null)} maxWidth="sm" fullWidth>
          <DialogTitle>출하 확정 — {confirmTarget?.shipmentNo}</DialogTitle>
          <DialogContent>
            <Alert severity="warning" sx={{ marginBottom: 2 }}>
              확정하면 실물 재고가 차감되고 되돌릴 수 없습니다.
            </Alert>
            {confirmTarget?.transportMode === 'AIR' ? (
              <TextField
                fullWidth
                margin="normal"
                label="AWB No. (항공운송장 번호)"
                value={confirmForm.awbNo}
                onChange={(e) => setConfirmForm((f) => ({ ...f, awbNo: e.target.value }))}
                helperText="항공 출하는 AWB 번호가 있어야 확정됩니다."
              />
            ) : (
              <TextField
                fullWidth
                margin="normal"
                label="B/L No. (선하증권 번호)"
                value={confirmForm.blNo}
                onChange={(e) => setConfirmForm((f) => ({ ...f, blNo: e.target.value }))}
                helperText="해상 출하는 B/L 번호가 있어야 확정됩니다. CI 결제기한의 기산일이 됩니다."
              />
            )}
            <TextField
              fullWidth
              margin="normal"
              label="선적일"
              type="date"
              value={confirmForm.shipDate}
              onChange={(e) => setConfirmForm((f) => ({ ...f, shipDate: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setConfirmTarget(null)}>취소</Button>
            <Button
              variant="contained"
              disabled={confirmTarget?.transportMode === 'AIR' ? !confirmForm.awbNo : !confirmForm.blNo}
              onClick={handleConfirm}
            >
              확정 (재고 차감)
            </Button>
          </DialogActions>
        </Dialog>

        <Snackbar
          open={Boolean(toast)}
          autoHideDuration={6000}
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
