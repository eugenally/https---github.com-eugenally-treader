import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  Pagination,
  Stack,
  Switch,
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
import SearchIcon from '@mui/icons-material/Search';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { productApi, toMessage } from '../../api/client';
import ExcelBar from '../../components/ExcelBar';

const EMPTY = {
  productCode: '',
  nameEn: '',
  nameKo: '',
  spec: '',
  hsCode: '',
  unit: 'EA',
  moq: '',
  packageType: '',
  qtyPerCarton: '',
  netWeight: '',
  grossWeight: '',
  cbm: '',
  initialStock: '',
};

const num = (v, d = 0) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: d, maximumFractionDigits: d });

export default function ProductTab({ notify }) {
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [applied, setApplied] = useState('');
  const [page, setPage] = useState(1);

  const [open, setOpen] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async (pageNo, kw) => {
    setLoading(true);
    try {
      setPageData(await productApi.search({ page: pageNo, size: 10, keyword: kw }));
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    load(page, applied);
  }, [load, page, applied]);

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY);
    setOpen(true);
  };

  const openEdit = async (id) => {
    try {
      const d = await productApi.get(id);
      setEditId(id);
      setForm({
        productCode: d.productCode,
        nameEn: d.nameEn ?? '',
        nameKo: d.nameKo ?? '',
        spec: d.spec ?? '',
        hsCode: d.hsCode ?? '',
        unit: d.unit ?? 'EA',
        moq: d.moq ?? '',
        packageType: d.packageType ?? '',
        qtyPerCarton: d.qtyPerCarton ?? '',
        netWeight: d.netWeight ?? '',
        grossWeight: d.grossWeight ?? '',
        cbm: d.cbm ?? '',
        initialStock: '',
      });
      setOpen(true);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const toNumberOrNull = (v) => (v === '' || v == null ? null : Number(v));

  const handleSave = async () => {
    setSaving(true);
    try {
      const body = {
        nameEn: form.nameEn,
        nameKo: form.nameKo || null,
        spec: form.spec || null,
        hsCode: form.hsCode || null,
        unit: form.unit,
        moq: toNumberOrNull(form.moq),
        packageType: form.packageType || null,
        qtyPerCarton: toNumberOrNull(form.qtyPerCarton),
        netWeight: toNumberOrNull(form.netWeight),
        grossWeight: toNumberOrNull(form.grossWeight),
        cbm: toNumberOrNull(form.cbm),
      };
      if (editId) {
        await productApi.update(editId, body);
        notify('제품을 수정했습니다.');
      } else {
        await productApi.create({
          ...body,
          productCode: form.productCode,
          initialStock: toNumberOrNull(form.initialStock),
        });
        notify('제품을 등록했습니다. 재고 행이 함께 만들어졌습니다.');
      }
      setOpen(false);
      load(page, applied);
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (row) => {
    const message = row.deletable
      ? `${row.productCode} 을(를) 삭제할까요?`
      : `${row.productCode} 은(는) 거래·재고 이력이 있어 삭제 대신 비활성 처리됩니다. 계속할까요?`;
    if (!window.confirm(message)) return;
    try {
      const res = await productApi.remove(row.id);
      notify(res.message);
      load(page, applied);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleToggleStatus = async (row) => {
    try {
      await productApi.changeStatus(row.id, row.status !== 'ACTIVE');
      load(page, applied);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const canSave = form.nameEn && form.unit && (editId || form.productCode);

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ marginBottom: 2, alignItems: 'center' }}>
        <TextField
          size="small"
          fullWidth
          placeholder="코드·품명·HS코드로 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              setPage(1);
              setApplied(keyword.trim());
            }
          }}
        />
        <Button
          variant="outlined"
          startIcon={<SearchIcon />}
          onClick={() => {
            setPage(1);
            setApplied(keyword.trim());
          }}
        >
          검색
        </Button>
        <Button variant="contained" startIcon={<AddIcon />} sx={{ whiteSpace: 'nowrap' }} onClick={openCreate}>
          제품 등록
        </Button>
      </Stack>

      <Box sx={{ marginBottom: 2 }}>
        <ExcelBar kind="product" notify={notify} onImported={() => load(page, applied)} />
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
                  <TableCell width={110}>코드</TableCell>
                  <TableCell>품명 / 규격</TableCell>
                  <TableCell width={60}>단위</TableCell>
                  <TableCell width={80} align="right">MOQ</TableCell>
                  <TableCell width={90} align="right">현재고</TableCell>
                  <TableCell width={90} align="right">가용</TableCell>
                  <TableCell width={80} align="center">상태</TableCell>
                  <TableCell width={110} align="center">작업</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {pageData?.content.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ color: '#999', padding: 4 }}>
                      {applied ? '검색 결과가 없습니다.' : '등록된 제품이 없습니다.'}
                    </TableCell>
                  </TableRow>
                )}
                {pageData?.content.map((p) => (
                  <TableRow key={p.id} hover>
                    <TableCell>
                      <strong>{p.productCode}</strong>
                    </TableCell>
                    <TableCell>
                      {p.nameEn}
                      <Typography variant="caption" color="textSecondary" sx={{ display: 'block' }}>
                        {[p.nameKo, p.spec].filter(Boolean).join(' · ')}
                      </Typography>
                    </TableCell>
                    <TableCell>{p.unit}</TableCell>
                    <TableCell align="right">{num(p.moq)}</TableCell>
                    <TableCell align="right">{num(p.onHandQty)}</TableCell>
                    <TableCell align="right" sx={{ color: Number(p.availableQty) < 0 ? '#d32f2f' : 'inherit' }}>
                      {num(p.availableQty)}
                    </TableCell>
                    <TableCell align="center">
                      <Switch size="small" checked={p.status === 'ACTIVE'} onChange={() => handleToggleStatus(p)} />
                    </TableCell>
                    <TableCell align="center">
                      <IconButton size="small" onClick={() => openEdit(p.id)}>
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

        {pageData && pageData.totalPages > 1 && (
          <Box sx={{ display: 'flex', justifyContent: 'center', padding: 2 }}>
            <Pagination count={pageData.totalPages} page={pageData.page} color="primary" onChange={(e, v) => setPage(v)} />
          </Box>
        )}
      </Card>

      {pageData && (
        <Typography variant="caption" color="textSecondary" sx={{ display: 'block', marginTop: 1, textAlign: 'right' }}>
          전체 {pageData.totalElements}건
        </Typography>
      )}

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editId ? '제품 수정' : '제품 등록'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ marginTop: 0 }}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                required
                label="제품 코드"
                value={form.productCode}
                onChange={set('productCode')}
                disabled={Boolean(editId)}
                helperText={editId ? '변경할 수 없습니다' : '영문·숫자·하이픈'}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 5 }}>
              <TextField fullWidth required label="영문 품명" value={form.nameEn} onChange={set('nameEn')} />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField fullWidth required label="단위" value={form.unit} onChange={set('unit')} helperText="EA / KG / M 등" />
            </Grid>
            <Grid size={{ xs: 12, sm: 5 }}>
              <TextField fullWidth label="한글 품명" value={form.nameKo} onChange={set('nameKo')} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField fullWidth label="규격" value={form.spec} onChange={set('spec')} />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField fullWidth label="HS 코드" value={form.hsCode} onChange={set('hsCode')} />
            </Grid>

            <Grid size={12}>
              <Divider sx={{ marginY: 1 }}>
                <Typography variant="caption" color="textSecondary">
                  포장·중량 — 출하 등록 시 중량이 자동 계산됩니다
                </Typography>
              </Divider>
            </Grid>

            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth type="number" label="MOQ" value={form.moq} onChange={set('moq')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth label="포장형태" value={form.packageType} onChange={set('packageType')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth type="number" label="박스당 수량" value={form.qtyPerCarton} onChange={set('qtyPerCarton')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth type="number" label="순중량(kg)" value={form.netWeight} onChange={set('netWeight')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth type="number" label="총중량(kg)" value={form.grossWeight} onChange={set('grossWeight')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth type="number" label="CBM" value={form.cbm} onChange={set('cbm')} />
            </Grid>

            {!editId && (
              <Grid size={{ xs: 12, sm: 4 }}>
                <TextField
                  fullWidth
                  type="number"
                  label="초기 재고"
                  value={form.initialStock}
                  onChange={set('initialStock')}
                  helperText="비우면 0으로 시작합니다"
                />
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
