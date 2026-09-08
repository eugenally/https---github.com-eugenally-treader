import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Checkbox,
  CircularProgress,
  Container,
  FormControlLabel,
  Link,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { toMessage } from '../api/client';

const SAVED_ID_KEY = 'treader.savedLoginId';

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();

  const savedId = localStorage.getItem(SAVED_ID_KEY) ?? '';
  const [loginId, setLoginId] = useState(savedId);
  const [password, setPassword] = useState('');
  const [rememberId, setRememberId] = useState(Boolean(savedId));
  const [error, setError] = useState(searchParams.get('expired') ? '세션이 만료되었습니다. 다시 로그인해 주세요.' : '');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');

    if (!loginId || !password) {
      setError('아이디와 비밀번호를 입력해 주세요.');
      return;
    }

    setLoading(true);
    try {
      const me = await login(loginId, password);

      // 아이디 저장 (D3-04). 비밀번호는 절대 저장하지 않는다.
      if (rememberId) {
        localStorage.setItem(SAVED_ID_KEY, loginId);
      } else {
        localStorage.removeItem(SAVED_ID_KEY);
      }

      // 임시 비밀번호로 들어왔으면 변경을 먼저 시킨다
      navigate(me.passwordTemporary ? '/profile?forcePassword=1' : '/dashboard');
    } catch (e) {
      setError(toMessage(e));
      setPassword('');
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
        justifyContent: 'center',
        backgroundColor: '#f5f5f5',
        paddingY: 4,
      }}
    >
      <Container maxWidth="xs">
        <Card sx={{ padding: 4, boxShadow: 3 }}>
          <Box sx={{ textAlign: 'center', marginBottom: 3 }}>
            <Typography variant="h4" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
              🚢 TreaderAPP
            </Typography>
            <Typography variant="body2" color="textSecondary">
              수출 오더 관리 시스템
            </Typography>
          </Box>

          {error && (
            <Alert severity="error" sx={{ marginBottom: 2 }}>
              {error}
            </Alert>
          )}

          <form onSubmit={handleLogin}>
            <TextField
              fullWidth
              label="아이디"
              margin="normal"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              autoFocus={!savedId}
            />
            <TextField
              fullWidth
              label="비밀번호"
              type="password"
              margin="normal"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoFocus={Boolean(savedId)}
            />
            <FormControlLabel
              control={
                <Checkbox checked={rememberId} onChange={(e) => setRememberId(e.target.checked)} />
              }
              label="아이디 저장"
              sx={{ marginTop: 1, marginBottom: 2 }}
            />
            <Button
              fullWidth
              variant="contained"
              size="large"
              type="submit"
              disabled={loading}
              startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}
            >
              로그인
            </Button>
          </form>

          <Stack
            direction="row"
            spacing={2}
            sx={{ marginTop: 3, justifyContent: 'center', alignItems: 'center' }}
          >
            <Link component="button" type="button" variant="body2" onClick={() => navigate('/signup')}>
              회원가입
            </Link>
            <Typography variant="body2" color="textSecondary">
              ·
            </Typography>
            <Link
              component="button"
              type="button"
              variant="body2"
              onClick={() => navigate('/find-password')}
            >
              비밀번호 찾기
            </Link>
          </Stack>
        </Card>
      </Container>
    </Box>
  );
}
