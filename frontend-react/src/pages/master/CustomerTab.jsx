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
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { customerApi, toMessage } from '../../api/client';

const EMPTY = {
  customerCode: '',
  nameEn: '',
  nameKo: '',
  countryCode: '',
  bizRegNo: '',
  address: '',
  defaultCurrency: 'USD',
  defaultIncoterms: 'FOB',
  paymentTermsCode: 'TT_30',
  paymentDays: 30,
  advanceRate: 30,
  quoteValidDays: 30,
  creditLimit: '',
  remark: '',
  contacts: [],
};

export default function CustomerTab({ notify }) {
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
      setPageData(await customerApi.search({ page: pageNo, size: 10, keyword: kw }));
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
      const d = await customerApi.get(id);
      setEditId(id);
      setForm({
        ...d,
        creditLimit: d.creditLimit ?? '',
        contacts: d.contacts ?? [],
      });
      setOpen(true);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const body = {
        ...form,
        creditLimit: form.creditLimit === '' ? null : Number(form.creditLimit),
        paymentDays: form.paymentDays === '' ? null : Number(form.paymentDays),
        advanceRate: form.advanceRate === '' ? null : Number(form.advanceRate),
        quoteValidDays: form.quoteValidDays === '' ? null : Number(form.quoteValidDays),
        contacts: form.contacts.map((c) => ({
          id: typeof c.id === 'number' ? c.id : null,
          name: c.name,
          email: c.email || null,
          phone: c.phone || null,
          position: c.position || null,
          main: Boolean(c.main),
        })),
      };
      if (editId) {
        delete body.customerCode;
        await customerApi.update(editId, body);
        notify('거래처를 수정했습니다.');
      } else {
        await customerApi.create(body);
        notify('거래처를 등록했습니다.');
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
      ? `${row.nameEn} 을(를) 삭제할까요?`
      : `${row.nameEn} 은(는) 거래 이력이 있어 삭제 대신 비활성 처리됩니다. 계속할까요?`;
    if (!window.confirm(message)) return;
    try {
      const res = await customerApi.remove(row.id);
      notify(res.message);
      load(page, applied);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleToggleStatus = async (row) => {
    try {
      await customerApi.changeStatus(row.id, row.status !== 'ACTIVE');
      load(page, applied);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  // 담당자 편집
  const addContact = () =>
    setForm((f) => ({
      ...f,
      contacts: [...f.contacts, { id: `new-${Date.now()}`, name: '', email: '', phone: '', position: '', main: f.contacts.length === 0 }],
    }));
  const updateContact = (idx, field, value) =>
    setForm((f) => ({
      ...f,
      contacts: f.contacts.map((c, i) => (i === idx ? { ...c, [field]: value } : c)),
    }));
  const removeContact = (idx) =>
    setForm((f) => ({ ...f, contacts: f.contacts.filter((_, i) => i !== idx) }));
  const setMainContact = (idx) =>
    setForm((f) => ({ ...f, contacts: f.contacts.map((c, i) => ({ ...c, main: i === idx })) }));

  const canSave = form.nameEn && form.countryCode && (editId || form.customerCode);

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ marginBottom: 2, alignItems: 'center' }}>
        <TextField
          size="small"
          fullWidth
          placeholder="코드·영문명·한글명으로 검색"
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
          거래처 등록
        </Button>
      </Stack>

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
                  <TableCell width={90}>코드</TableCell>
                  <TableCell>거래처명</TableCell>
                  <TableCell width={60}>국가</TableCell>
                  <TableCell width={70}>통화</TableCell>
                  <TableCell width={80}>Incoterms</TableCell>
                  <TableCell width={80} align="right">선금율</TableCell>
                  <TableCell width={80} align="right">유효일수</TableCell>
                  <TableCell width={100}>대표담당자</TableCell>
                  <TableCell width={80} align="center">상태</TableCell>
                  <TableCell width={110} align="center">작업</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {pageData?.content.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={10} align="center" sx={{ color: '#999', padding: 4 }}>
                      {applied ? '검색 결과가 없습니다.' : '등록된 거래처가 없습니다.'}
                    </TableCell>
                  </TableRow>
                )}
                {pageData?.content.map((c) => (
                  <TableRow key={c.id} hover>
                    <TableCell>
                      <strong>{c.customerCode}</strong>
                    </TableCell>
                    <TableCell>
                      {c.nameEn}
                      {c.nameKo && (
                        <Typography variant="caption" color="textSecondary" sx={{ display: 'block' }}>
                          {c.nameKo}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>{c.countryCode}</TableCell>
                    <TableCell>{c.defaultCurrency}</TableCell>
                    <TableCell>{c.defaultIncoterms ?? '-'}</TableCell>
                    <TableCell align="right">{c.advanceRate != null ? `${c.advanceRate}%` : '-'}</TableCell>
                    <TableCell align="right">{c.quoteValidDays}일</TableCell>
                    <TableCell>{c.mainContactName ?? '-'}</TableCell>
                    <TableCell align="center">
                      <Switch
                        size="small"
                        checked={c.status === 'ACTIVE'}
                        onChange={() => handleToggleStatus(c)}
                      />
                    </TableCell>
                    <TableCell align="center">
                      <IconButton size="small" onClick={() => openEdit(c.id)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" color="error" onClick={() => handleDelete(c)}>
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
        <DialogTitle>{editId ? '거래처 수정' : '거래처 등록'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ marginTop: 0 }}>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                fullWidth
                required
                label="거래처 코드"
                value={form.customerCode}
                onChange={(e) => setForm((f) => ({ ...f, customerCode: e.target.value.toUpperCase() }))}
                disabled={Boolean(editId)}
                helperText={editId ? '변경할 수 없습니다' : '대문자·숫자 2~8자'}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField fullWidth required label="영문 상호" value={form.nameEn} onChange={set('nameEn')} />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                fullWidth
                required
                label="국가코드"
                value={form.countryCode}
                onChange={(e) => setForm((f) => ({ ...f, countryCode: e.target.value.toUpperCase() }))}
                helperText="ISO 2자 (예: VN)"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField fullWidth label="한글 상호" value={form.nameKo ?? ''} onChange={set('nameKo')} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField fullWidth label="사업자번호" value={form.bizRegNo ?? ''} onChange={set('bizRegNo')} />
            </Grid>
            <Grid size={12}>
              <TextField fullWidth label="주소" value={form.address ?? ''} onChange={set('address')} />
            </Grid>

            <Grid size={12}>
              <Divider sx={{ marginY: 1 }}>
                <Typography variant="caption" color="textSecondary">
                  거래 기본값 — 견적 작성 시 자동으로 채워집니다
                </Typography>
              </Divider>
            </Grid>

            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField
                fullWidth
                label="통화"
                value={form.defaultCurrency ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, defaultCurrency: e.target.value.toUpperCase() }))}
              />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth label="Incoterms" value={form.defaultIncoterms ?? ''} onChange={set('defaultIncoterms')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth label="결제조건" value={form.paymentTermsCode ?? ''} onChange={set('paymentTermsCode')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth type="number" label="결제일수" value={form.paymentDays ?? ''} onChange={set('paymentDays')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField
                fullWidth
                type="number"
                label="선금율(%)"
                value={form.advanceRate ?? ''}
                onChange={set('advanceRate')}
                helperText="0이면 PI 불가"
              />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField fullWidth type="number" label="견적유효일" value={form.quoteValidDays ?? ''} onChange={set('quoteValidDays')} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField fullWidth type="number" label="여신한도" value={form.creditLimit ?? ''} onChange={set('creditLimit')} />
            </Grid>
            <Grid size={{ xs: 12, sm: 8 }}>
              <TextField fullWidth label="비고" value={form.remark ?? ''} onChange={set('remark')} />
            </Grid>

            <Grid size={12}>
              <Divider sx={{ marginY: 1 }}>
                <Typography variant="caption" color="textSecondary">
                  담당자
                </Typography>
              </Divider>
            </Grid>

            <Grid size={12}>
              {form.contacts.length === 0 && (
                <Alert severity="info" sx={{ marginBottom: 1 }}>
                  담당자를 추가하면 첫 번째가 대표 연락처가 됩니다.
                </Alert>
              )}
              {form.contacts.map((c, idx) => (
                <Stack key={c.id ?? idx} direction="row" spacing={1} sx={{ marginBottom: 1, alignItems: 'center' }}>
                  <IconButton size="small" onClick={() => setMainContact(idx)} title="대표 연락처로 지정">
                    {c.main ? <StarIcon fontSize="small" sx={{ color: '#f08c00' }} /> : <StarBorderIcon fontSize="small" />}
                  </IconButton>
                  <TextField size="small" label="이름" value={c.name} onChange={(e) => updateContact(idx, 'name', e.target.value)} sx={{ width: 130 }} />
                  <TextField size="small" label="이메일" value={c.email ?? ''} onChange={(e) => updateContact(idx, 'email', e.target.value)} sx={{ flexGrow: 1 }} />
                  <TextField size="small" label="전화" value={c.phone ?? ''} onChange={(e) => updateContact(idx, 'phone', e.target.value)} sx={{ width: 140 }} />
                  <TextField size="small" label="직책" value={c.position ?? ''} onChange={(e) => updateContact(idx, 'position', e.target.value)} sx={{ width: 120 }} />
                  <IconButton size="small" color="error" onClick={() => removeContact(idx)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Stack>
              ))}
              <Button size="small" startIcon={<PersonAddIcon />} onClick={addContact}>
                담당자 추가
              </Button>
            </Grid>
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
