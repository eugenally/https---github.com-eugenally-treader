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
import SearchIcon from '@mui/icons-material/Search';
import TuneIcon from '@mui/icons-material/Tune';
import { productApi, toMessage } from '../../api/client';

const num = (v, d = 0) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: d, maximumFractionDigits: d });

export default function StockTab({ notify }) {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [applied, setApplied] = useState('');

  const [target, setTarget] = useState(null);
  const [countedQty, setCountedQty] = useState('');
  const [reason, setReason] = useState('');

  const load = useCallback(async (kw) => {
    setLoading(true);
    try {
      setStocks(await productApi.stocks(kw));
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    load(applied);
  }, [load, applied]);

  const openAdjust = (row) => {
    setTarget(row);
    setCountedQty(String(row.onHandQty ?? 0));
    setReason('');
  };

  const handleAdjust = async () => {
    try {
      const result = await productApi.adjustStock(target.productId, {
        countedQty: Number(countedQty),
        reason: reason || null,
      });
      setTarget(null);
      load(applied);
      notify(`${result.productCode} 재고를 ${num(result.onHandQty)} 로 조정했습니다.`);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const diff = target ? Number(countedQty || 0) - Number(target.onHandQty ?? 0) : 0;

  return (
    <Box>
      <Alert severity="info" sx={{ marginBottom: 2 }}>
        <strong>가용 = 현재고 − 할당</strong>. 할당은 수주 확정 시 잡히고, 현재고는 출하 확정 때 빠집니다.
        가용이 음수면 백오더입니다.
      </Alert>

      <Stack direction="row" spacing={1} sx={{ marginBottom: 2 }}>
        <TextField
          size="small"
          fullWidth
          placeholder="제품 코드·품명으로 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && setApplied(keyword.trim())}
        />
        <Button variant="outlined" startIcon={<SearchIcon />} onClick={() => setApplied(keyword.trim())}>
          검색
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
                  <TableCell width={120}>코드</TableCell>
                  <TableCell>품명</TableCell>
                  <TableCell width={60}>단위</TableCell>
                  <TableCell width={100} align="right">현재고</TableCell>
                  <TableCell width={100} align="right">할당</TableCell>
                  <TableCell width={100} align="right">가용</TableCell>
                  <TableCell width={100} align="right">안전재고</TableCell>
                  <TableCell width={100} align="center">조정</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {stocks.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ color: '#999', padding: 4 }}>
                      제품이 없습니다.
                    </TableCell>
                  </TableRow>
                )}
                {stocks.map((s) => (
                  <TableRow key={s.productId} hover>
                    <TableCell>
                      <strong>{s.productCode}</strong>
                    </TableCell>
                    <TableCell>{s.productName}</TableCell>
                    <TableCell>{s.unit}</TableCell>
                    <TableCell align="right">{num(s.onHandQty)}</TableCell>
                    <TableCell align="right" sx={{ color: Number(s.allocatedQty) > 0 ? '#f08c00' : 'inherit' }}>
                      {num(s.allocatedQty)}
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={0.5} sx={{ justifyContent: 'flex-end', alignItems: 'center' }}>
                        <strong style={{ color: Number(s.availableQty) < 0 ? '#d32f2f' : 'inherit' }}>
                          {num(s.availableQty)}
                        </strong>
                        {s.belowSafety && <Chip label="부족" size="small" color="warning" />}
                      </Stack>
                    </TableCell>
                    <TableCell align="right">{num(s.safetyQty)}</TableCell>
                    <TableCell align="center">
                      <Button size="small" startIcon={<TuneIcon />} onClick={() => openAdjust(s)}>
                        실사
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <Dialog open={Boolean(target)} onClose={() => setTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>재고 실사 조정</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ marginBottom: 2 }}>
            차이가 아니라 <strong>실제로 센 수량</strong>을 입력하세요. 조정분은 재고 이력에 남습니다.
          </Alert>
          {target && (
            <>
              <Typography variant="body2" sx={{ marginBottom: 1 }}>
                <strong>{target.productCode}</strong> · {target.productName}
              </Typography>
              <Typography variant="body2" color="textSecondary" sx={{ marginBottom: 2 }}>
                장부 재고: {num(target.onHandQty)} {target.unit}
              </Typography>
            </>
          )}
          <TextField
            fullWidth
            autoFocus
            type="number"
            label="실사 수량"
            value={countedQty}
            onChange={(e) => setCountedQty(e.target.value)}
          />
          {countedQty !== '' && diff !== 0 && (
            <Alert severity={diff > 0 ? 'success' : 'error'} sx={{ marginTop: 1 }}>
              차이: {diff > 0 ? '+' : ''}{num(diff)} {target?.unit}
            </Alert>
          )}
          <TextField
            fullWidth
            label="사유"
            margin="normal"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="예: 월말 실사 차이"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTarget(null)}>취소</Button>
          <Button variant="contained" disabled={countedQty === '' || Number(countedQty) < 0} onClick={handleAdjust}>
            조정
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
