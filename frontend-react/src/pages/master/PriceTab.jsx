import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
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
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { customerApi, priceApi, productApi, toMessage } from '../../api/client';
import ExcelBar from '../../components/ExcelBar';

const today = () => new Date().toISOString().slice(0, 10);
const money = (v) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 4 });

export default function PriceTab({ notify }) {
  const [prices, setPrices] = useState([]);
  const [products, setProducts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);

  const [filter, setFilter] = useState({ productId: '', customerId: '' });

  const [open, setOpen] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({
    productId: '',
    customerId: '',
    currency: 'USD',
    unitPrice: '',
    incoterms: '',
    minQty: '',
    validFrom: today(),
    validTo: '',
    remark: '',
    closePrevious: false,
  });
  const [saving, setSaving] = useState(false);

  const load = useCallback(async (f) => {
    setLoading(true);
    try {
      setPrices(await priceApi.list({ productId: f.productId || undefined, customerId: f.customerId || undefined }));
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    (async () => {
      try {
        const [ps, cs] = await Promise.all([productApi.list(), customerApi.list()]);
        setProducts(ps);
        setCustomers(cs);
      } catch (e) {
        notify(toMessage(e), 'error');
      }
    })();
  }, [notify]);

  useEffect(() => {
    load(filter);
  }, [load, filter]);

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const openCreate = () => {
    setEditId(null);
    setForm({
      productId: filter.productId || '',
      customerId: filter.customerId || '',
      currency: 'USD',
      unitPrice: '',
      incoterms: '',
      minQty: '',
      validFrom: today(),
      validTo: '',
      remark: '',
      closePrevious: false,
    });
    setOpen(true);
  };

  const openEdit = (row) => {
    setEditId(row.id);
    setForm({
      productId: row.productId,
      customerId: row.customerId ?? '',
      currency: row.currency,
      unitPrice: row.unitPrice,
      incoterms: row.incoterms ?? '',
      minQty: row.minQty ?? '',
      validFrom: row.validFrom,
      validTo: row.validTo ?? '',
      remark: row.remark ?? '',
      closePrevious: false,
    });
    setOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      if (editId) {
        await priceApi.update(editId, {
          unitPrice: Number(form.unitPrice),
          currency: form.currency,
          incoterms: form.incoterms || null,
          minQty: form.minQty === '' ? null : Number(form.minQty),
          validTo: form.validTo || null,
          remark: form.remark || null,
        });
        notify('단가를 수정했습니다.');
      } else {
        await priceApi.create({
          productId: Number(form.productId),
          customerId: form.customerId === '' ? null : Number(form.customerId),
          currency: form.currency,
          unitPrice: Number(form.unitPrice),
          incoterms: form.incoterms || null,
          minQty: form.minQty === '' ? null : Number(form.minQty),
          validFrom: form.validFrom,
          validTo: form.validTo || null,
          remark: form.remark || null,
          closePrevious: form.closePrevious,
        });
        notify('단가를 등록했습니다.');
      }
      setOpen(false);
      load(filter);
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (row) => {
    if (!window.confirm('이 단가를 삭제할까요?')) return;
    try {
      await priceApi.remove(row.id);
      notify('단가를 삭제했습니다.');
      load(filter);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const canSave = form.productId && form.unitPrice && form.validFrom;

  return (
    <Box>
      <Alert severity="info" sx={{ marginBottom: 2 }}>
        거래처를 비우면 <strong>표준 단가</strong>가 됩니다. 견적 작성 시 거래처 전용 단가가 먼저 적용되고,
        없으면 표준 단가로 내려갑니다.
      </Alert>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ marginBottom: 2, alignItems: 'center' }}>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>제품</InputLabel>
          <Select
            value={filter.productId}
            label="제품"
            onChange={(e) => setFilter((f) => ({ ...f, productId: e.target.value }))}
          >
            <MenuItem value="">전체</MenuItem>
            {products.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                [{p.productCode}] {p.nameEn}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>거래처</InputLabel>
          <Select
            value={filter.customerId}
            label="거래처"
            onChange={(e) => setFilter((f) => ({ ...f, customerId: e.target.value }))}
          >
            <MenuItem value="">전체</MenuItem>
            {customers.map((c) => (
              <MenuItem key={c.id} value={c.id}>
                [{c.customerCode}] {c.nameEn}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Box sx={{ flexGrow: 1 }} />
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          단가 등록
        </Button>
      </Stack>

      <Box sx={{ marginBottom: 2 }}>
        <ExcelBar kind="price" notify={notify} onImported={() => load(filter)} />
      </Box>

      <Card>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', padding: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <TableContainer>
            <Table size="small">
              <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                <TableRow>
                  <TableCell>제품</TableCell>
                  <TableCell width={180}>거래처</TableCell>
                  <TableCell width={70}>통화</TableCell>
                  <TableCell width={100} align="right">단가</TableCell>
                  <TableCell width={90}>Incoterms</TableCell>
                  <TableCell width={110}>시작일</TableCell>
                  <TableCell width={110}>종료일</TableCell>
                  <TableCell width={80} align="center">현재</TableCell>
                  <TableCell width={100} align="center">작업</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {prices.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={9} align="center" sx={{ color: '#999', padding: 4 }}>
                      등록된 단가가 없습니다.
                    </TableCell>
                  </TableRow>
                )}
                {prices.map((p) => (
                  <TableRow key={p.id} hover sx={{ backgroundColor: p.effectiveNow ? '#f1f8e9' : 'inherit' }}>
                    <TableCell>
                      <strong>{p.productCode}</strong>
                      <Typography variant="caption" color="textSecondary" sx={{ display: 'block' }}>
                        {p.productName}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {p.standardPrice ? (
                        <Chip label="표준 단가" size="small" variant="outlined" />
                      ) : (
                        p.customerName
                      )}
                    </TableCell>
                    <TableCell>{p.currency}</TableCell>
                    <TableCell align="right">
                      <strong>{money(p.unitPrice)}</strong>
                    </TableCell>
                    <TableCell>{p.incoterms ?? '-'}</TableCell>
                    <TableCell>{p.validFrom}</TableCell>
                    <TableCell>{p.validTo ?? '무기한'}</TableCell>
                    <TableCell align="center">
                      {p.effectiveNow && <Chip label="적용중" size="small" color="success" />}
                    </TableCell>
                    <TableCell align="center">
                      <IconButton size="small" onClick={() => openEdit(p)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" color="error" onClick={() => handleDelete(p)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editId ? '단가 수정' : '단가 등록'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ marginTop: 0 }}>
            <Grid size={12}>
              <FormControl fullWidth required disabled={Boolean(editId)}>
                <InputLabel>제품</InputLabel>
                <Select value={form.productId} label="제품" onChange={set('productId')}>
                  {products.map((p) => (
                    <MenuItem key={p.id} value={p.id}>
                      [{p.productCode}] {p.nameEn}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={12}>
              <FormControl fullWidth disabled={Boolean(editId)}>
                <InputLabel>거래처</InputLabel>
                <Select value={form.customerId} label="거래처" onChange={set('customerId')}>
                  <MenuItem value="">— 표준 단가 (모든 거래처) —</MenuItem>
                  {customers.map((c) => (
                    <MenuItem key={c.id} value={c.id}>
                      [{c.customerCode}] {c.nameEn}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <TextField fullWidth label="통화" value={form.currency} onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value.toUpperCase() }))} />
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <TextField fullWidth required type="number" label="단가" value={form.unitPrice} onChange={set('unitPrice')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <TextField fullWidth label="Incoterms" value={form.incoterms} onChange={set('incoterms')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <TextField fullWidth type="number" label="최소수량" value={form.minQty} onChange={set('minQty')} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                required
                type="date"
                label="적용 시작일"
                value={form.validFrom}
                onChange={set('validFrom')}
                disabled={Boolean(editId)}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                type="date"
                label="적용 종료일"
                value={form.validTo}
                onChange={set('validTo')}
                slotProps={{ inputLabel: { shrink: true } }}
                helperText="비우면 무기한"
              />
            </Grid>
            <Grid size={12}>
              <TextField fullWidth label="비고" value={form.remark} onChange={set('remark')} />
            </Grid>

            {!editId && (
              <Grid size={12}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={form.closePrevious}
                      onChange={(e) => setForm((f) => ({ ...f, closePrevious: e.target.checked }))}
                    />
                  }
                  label="기간이 겹치면 이전 단가를 자동으로 마감"
                />
                <Typography variant="caption" color="textSecondary" sx={{ display: 'block' }}>
                  가격 인상처럼 "오늘부터 새 단가" 인 경우에 켭니다. 이전 단가는 시작일 하루 전으로 닫힙니다.
                  끄면 기간이 겹칠 때 오류로 알려 줍니다.
                </Typography>
              </Grid>
            )}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>취소</Button>
          <Button variant="contained" disabled={!canSave || saving} onClick={handleSave}>
            저장
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
