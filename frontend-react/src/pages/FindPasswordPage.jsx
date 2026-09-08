import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CircularProgress,
  Container,
  Link,
  TextField,
  Typography,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { authApi, toMessage } from '../api/client';

export default function FindPasswordPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ loginId: '', email: '' });
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await authApi.findPassword(form);
      setResult(res.message ?? '임시 비밀번호를 이메일로 보냈습니다.');
    } catch (e) {
      setError(toMessage(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        backgroundColor: '#f5f5f5',
        paddingY: 4,
      }}
    >
      <Container maxWidth="xs">
        <Card sx={{ padding: 4 }}>
          <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
            비밀번호 찾기
          </Typography>
          <Typography variant="body2" color="textSecondary" sx={{ marginBottom: 3 }}>
            가입한 아이디와 이메일이 모두 일치해야 임시 비밀번호를 보내 드립니다.
          </Typography>

          {error && (
            <Alert severity="error" sx={{ marginBottom: 2 }}>
              {error}
            </Alert>
          )}

          {result ? (
            <>
              <Alert severity="success">{result}</Alert>
              <Alert severity="warning" sx={{ marginTop: 2 }}>
                임시 비밀번호로 로그인한 뒤 반드시 비밀번호를 변경해 주세요.
              </Alert>
              <Button variant="contained" fullWidth sx={{ marginTop: 3 }} onClick={() => navigate('/login')}>
                로그인 화면으로
              </Button>
            </>
          ) : (
            <form onSubmit={handleSubmit}>
              <TextField
                fullWidth
                required
                label="아이디"
                margin="normal"
                value={form.loginId}
                onChange={(e) => setForm((f) => ({ ...f, loginId: e.target.value }))}
              />
              <TextField
                fullWidth
                required
                type="email"
                label="가입 이메일"
                margin="normal"
                value={form.email}
                onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
              />
              <Button
                fullWidth
                variant="contained"
                size="large"
                type="submit"
                sx={{ marginTop: 2 }}
                disabled={!form.loginId || !form.email || loading}
                startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}
              >
                임시 비밀번호 받기
              </Button>
            </form>
          )}

          <Box sx={{ textAlign: 'center', marginTop: 3 }}>
            <Link component="button" type="button" variant="body2" onClick={() => navigate('/login')}>
              로그인으로 돌아가기
            </Link>
          </Box>
        </Card>
      </Container>
    </Box>
  );
}
