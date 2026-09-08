import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  IconButton,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import DownloadIcon from '@mui/icons-material/Download';
import DeleteIcon from '@mui/icons-material/Delete';
import { attachmentApi, toMessage } from '../api/client';

const formatSize = (bytes) => {
  if (bytes == null) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
};

/**
 * 업무 문서 첨부 영역.
 * 견적·수주·출하·인보이스가 {@code refType} 만 바꿔 같은 컴포넌트를 쓴다 (다형 참조).
 */
export default function AttachmentPanel({ refType, refId, notify, title = '첨부파일' }) {
  const [files, setFiles] = useState([]);
  const [docType, setDocType] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!refId) return;
    try {
      setFiles(await attachmentApi.list(refType, refId));
    } catch (e) {
      notify?.(toMessage(e), 'error');
    }
  }, [refType, refId, notify]);

  useEffect(() => {
    load();
  }, [load]);

  const handleUpload = async (e) => {
    const selected = Array.from(e.target.files ?? []);
    e.target.value = '';
    if (selected.length === 0) return;

    setBusy(true);
    try {
      await attachmentApi.upload(refType, refId, docType, selected);
      setDocType('');
      await load();
      notify?.(`${selected.length}건을 첨부했습니다.`);
    } catch (e) {
      notify?.(toMessage(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async (file) => {
    if (!window.confirm(`${file.origName} 을(를) 삭제할까요?`)) return;
    try {
      await attachmentApi.remove(file.id);
      await load();
      notify?.('첨부를 삭제했습니다.');
    } catch (e) {
      notify?.(toMessage(e), 'error');
    }
  };

  return (
    <Card sx={{ padding: 3 }}>
      <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
        <AttachFileIcon sx={{ marginRight: 1, verticalAlign: 'middle' }} />
        {title} ({files.length})
      </Typography>

      {files.length === 0 && (
        <Alert severity="info" sx={{ marginBottom: 2 }}>
          첨부된 파일이 없습니다. B/L, P/O, 신용장 사본 등을 여기에 보관하세요.
        </Alert>
      )}

      {files.map((f) => (
        <Stack
          key={f.id}
          direction="row"
          spacing={1}
          sx={{ alignItems: 'center', paddingY: 1, borderBottom: '1px solid #eee' }}
        >
          {f.docType && <Chip label={f.docType} size="small" color="primary" variant="outlined" />}
          <Typography variant="body2" sx={{ flexGrow: 1 }}>
            {f.origName}
          </Typography>
          <Typography variant="caption" color="textSecondary">
            {formatSize(f.fileSize)}
            {f.createdBy && ` · ${f.createdBy}`}
            {f.createdAt && ` · ${f.createdAt.slice(0, 10)}`}
          </Typography>
          <IconButton size="small" onClick={() => attachmentApi.download(f.id, f.origName)}>
            <DownloadIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" color="error" onClick={() => handleDelete(f)}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      ))}

      <Stack direction="row" spacing={1} sx={{ marginTop: 2, alignItems: 'center' }}>
        <TextField
          size="small"
          label="서류 종류"
          placeholder="BL / PO / LC"
          value={docType}
          onChange={(e) => setDocType(e.target.value.toUpperCase())}
          sx={{ width: 160 }}
        />
        <Button variant="outlined" component="label" startIcon={<AttachFileIcon />} disabled={busy}>
          파일 첨부
          <input hidden multiple type="file" onChange={handleUpload} />
        </Button>
        <Box sx={{ flexGrow: 1 }} />
        <Typography variant="caption" color="textSecondary">
          파일당 최대 20MB
        </Typography>
      </Stack>
    </Card>
  );
}
