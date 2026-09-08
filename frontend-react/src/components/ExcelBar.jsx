import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
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
  Typography,
} from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import TableViewIcon from '@mui/icons-material/TableView';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import { excelApi, toMessage } from '../api/client';

/**
 * 엑셀 양식 다운로드 · 대량등록 · 목록 내보내기 버튼 묶음.
 * 제품 탭과 단가 탭이 같은 형태를 쓰므로 컴포넌트로 뺐다.
 */
export default function ExcelBar({ kind, notify, onImported }) {
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);

  const isProduct = kind === 'product';
  const label = isProduct ? '제품' : '단가';

  const handleTemplate = async () => {
    try {
      await (isProduct ? excelApi.productTemplate() : excelApi.priceTemplate());
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleExport = async () => {
    try {
      await (isProduct ? excelApi.exportProducts() : excelApi.exportPrices());
      notify(`${label} 목록을 내려받았습니다.`);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleImport = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';   // 같은 파일을 다시 골라도 change 가 뜨도록 비운다
    if (!file) return;

    setBusy(true);
    try {
      const res = await (isProduct ? excelApi.importProducts(file) : excelApi.importPrices(file));
      setResult(res);
      onImported?.();
      if (res.failCount === 0) {
        notify(`${res.successCount}건을 등록했습니다.` + (res.skippedCount ? ` (${res.skippedCount}건은 이미 있어 건너뜀)` : ''));
      }
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
        <Button size="small" startIcon={<DownloadIcon />} onClick={handleTemplate}>
          양식 다운로드
        </Button>
        <Button size="small" component="label" startIcon={<UploadFileIcon />} disabled={busy}>
          대량등록
          <input hidden type="file" accept=".xlsx" onChange={handleImport} />
        </Button>
        <Button size="small" startIcon={<TableViewIcon />} onClick={handleExport}>
          목록 내보내기
        </Button>
      </Stack>

      {/* 업로드 결과 — 부분 성공이면 실패 행을 사유와 함께 보여준다 */}
      <Dialog open={Boolean(result)} onClose={() => setResult(null)} maxWidth="md" fullWidth>
        <DialogTitle>{label} 대량등록 결과</DialogTitle>
        <DialogContent>
          <Stack direction="row" spacing={2} sx={{ marginBottom: 2, flexWrap: 'wrap', gap: 1 }}>
            <Chip label={`전체 ${result?.totalRows ?? 0}행`} />
            <Chip label={`성공 ${result?.successCount ?? 0}건`} color="success" />
            {result?.skippedCount > 0 && (
              <Chip label={`건너뜀 ${result.skippedCount}건`} color="default" />
            )}
            {result?.failCount > 0 && (
              <Chip label={`실패 ${result.failCount}건`} color="error" />
            )}
          </Stack>

          {result?.failCount === 0 ? (
            <Alert severity="success">모든 행이 정상 처리되었습니다.</Alert>
          ) : (
            <>
              <Alert severity="warning" icon={<ReportProblemIcon />} sx={{ marginBottom: 2 }}>
                성공한 {result?.successCount}건은 이미 저장되었습니다.
                아래 행만 원본 파일에서 고쳐 다시 올리시면 됩니다.
              </Alert>
              <TableContainer sx={{ maxHeight: 320 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell width={100}>엑셀 행</TableCell>
                      <TableCell width={160}>키</TableCell>
                      <TableCell>실패 사유</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {result?.errors.map((err, i) => (
                      <TableRow key={i}>
                        <TableCell>{err.rowNum}</TableCell>
                        <TableCell>{err.key ?? '-'}</TableCell>
                        <TableCell sx={{ color: '#d32f2f' }}>{err.reason}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
        </DialogContent>
        <DialogActions>
          {result?.failCount > 0 && (
            <Button
              startIcon={<DownloadIcon />}
              onClick={() => excelApi.exportErrors(result.errors).catch((e) => notify(toMessage(e), 'error'))}
            >
              오류 목록 엑셀로 받기
            </Button>
          )}
          <Box sx={{ flexGrow: 1 }} />
          <Button variant="contained" onClick={() => setResult(null)}>
            닫기
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
