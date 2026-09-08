import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Grid,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import DeleteIcon from '@mui/icons-material/Delete';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { boardApi, toMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function PostFormPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { member } = useAuth();

  const editId = searchParams.get('edit');
  const boardCode = searchParams.get('board') ?? 'FREE';
  const passedPwd = searchParams.get('pwd') ?? '';
  const isEdit = Boolean(editId);

  const [board, setBoard] = useState(null);
  const [form, setForm] = useState({ title: '', content: '', guestName: '', guestPwd: passedPwd });
  const [existingFiles, setExistingFiles] = useState([]);
  const [newFiles, setNewFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const notify = (message, severity = 'success') => setToast({ message, severity });

  useEffect(() => {
    (async () => {
      try {
        const boards = await boardApi.boards();

        if (isEdit) {
          const post = await boardApi.post(editId);
          setBoard(boards.find((b) => b.boardCode === post.boardCode));
          setForm((f) => ({ ...f, title: post.title, content: post.content }));
          setExistingFiles(post.files);
        } else {
          setBoard(boards.find((b) => b.boardCode === boardCode) ?? boards[0]);
        }
      } catch (e) {
        notify(toMessage(e), 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, [editId, boardCode, isEdit]);

  const handleSubmit = async () => {
    setSaving(true);
    try {
      let post;
      if (isEdit) {
        post = await boardApi.update(editId, {
          title: form.title,
          content: form.content,
          guestPwd: form.guestPwd || null,
        });
      } else {
        post = await boardApi.create(board.boardCode, {
          title: form.title,
          content: form.content,
          guestName: member ? null : form.guestName,
          guestPwd: member ? null : form.guestPwd,
        });
      }

      if (newFiles.length > 0) {
        await boardApi.uploadFiles(post.id, newFiles);
      }

      notify(isEdit ? '글을 수정했습니다.' : '글을 등록했습니다.');
      navigate(`/board/post/${post.id}`);
    } catch (e) {
      notify(toMessage(e), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteExistingFile = async (fileId) => {
    try {
      await boardApi.deleteFile(fileId);
      setExistingFiles((files) => files.filter((f) => f.id !== fileId));
      notify('첨부파일을 삭제했습니다.');
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

  const needsGuestFields = !member && !isEdit;
  const canSubmit =
    form.title.trim() &&
    form.content.trim() &&
    (!needsGuestFields || (form.guestName.trim() && form.guestPwd.length >= 4));

  return (
    <Layout>
      <Box>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>
          돌아가기
        </Button>

        <Card sx={{ padding: 3, marginTop: 2 }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', marginBottom: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              {isEdit ? '글 수정' : '글쓰기'}
            </Typography>
            {board && <Chip label={board.boardName} size="small" color="primary" />}
          </Stack>

          {needsGuestFields && (
            <Alert severity="info" sx={{ marginBottom: 2 }}>
              비회원으로 글을 씁니다. 나중에 수정·삭제하려면 여기서 정한 비밀번호가 필요합니다.
            </Alert>
          )}

          {needsGuestFields && (
            <Grid container spacing={2} sx={{ marginBottom: 1 }}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  required
                  label="이름"
                  value={form.guestName}
                  onChange={(e) => setForm((f) => ({ ...f, guestName: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  required
                  type="password"
                  label="비밀번호"
                  value={form.guestPwd}
                  onChange={(e) => setForm((f) => ({ ...f, guestPwd: e.target.value }))}
                  helperText="4자 이상"
                />
              </Grid>
            </Grid>
          )}

          <TextField
            fullWidth
            required
            label="제목"
            margin="normal"
            value={form.title}
            onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
          />
          <TextField
            fullWidth
            required
            multiline
            rows={12}
            label="내용"
            margin="normal"
            value={form.content}
            onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
          />

          {board?.fileAllowed && (
            <Box sx={{ marginTop: 2 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
                첨부파일
              </Typography>

              {existingFiles.map((f) => (
                <Stack key={f.id} direction="row" spacing={1} sx={{ alignItems: 'center', marginBottom: 0.5 }}>
                  <AttachFileIcon fontSize="small" sx={{ color: '#888' }} />
                  <Typography variant="body2" sx={{ flexGrow: 1 }}>
                    {f.origName}
                  </Typography>
                  <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleDeleteExistingFile(f.id)}>
                    삭제
                  </Button>
                </Stack>
              ))}

              <Button variant="outlined" component="label" startIcon={<AttachFileIcon />} sx={{ marginTop: 1 }}>
                파일 선택
                <input
                  hidden
                  multiple
                  type="file"
                  onChange={(e) => setNewFiles(Array.from(e.target.files ?? []))}
                />
              </Button>
              {newFiles.length > 0 && (
                <Typography variant="caption" sx={{ display: 'block', marginTop: 1 }}>
                  선택됨: {newFiles.map((f) => f.name).join(', ')}
                </Typography>
              )}
              <Typography variant="caption" color="textSecondary" sx={{ display: 'block', marginTop: 0.5 }}>
                파일당 최대 10MB
              </Typography>
            </Box>
          )}

          <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end', marginTop: 3 }}>
            <Button onClick={() => navigate(-1)}>취소</Button>
            <Button
              variant="contained"
              startIcon={<SaveIcon />}
              disabled={!canSubmit || saving}
              onClick={handleSubmit}
            >
              {isEdit ? '수정' : '등록'}
            </Button>
          </Stack>
        </Card>

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
