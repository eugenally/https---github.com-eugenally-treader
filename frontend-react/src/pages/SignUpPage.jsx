import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CircularProgress,
  Container,
  Grid,
  InputAdornment,
  Link,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { useNavigate } from 'react-router-dom';
import { authApi, toMessage } from '../api/client';

const EMPTY = {
  loginId: '',
  password: '',
  passwordConfirm: '',
  name: '',
  email: '',
  phone: '',
  dept: '',
  position: '',
};

export default function SignUpPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY);
  const [checked, setChecked] = useState({ loginId: null, email: null });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(null);

  const set = (field) => (e) => {
    setForm((f) => ({ ...f, [field]: e.target.value }));
    // 값이 바뀌면 이전 중복확인 결과는 무효다
    if (field === 'loginId' || field === 'email') {
      setChecked((c) => ({ ...c, [field]: null }));
    }
  };

  const check = async (field) => {
    setError('');
    try {
      const result =
        field === 'loginId'
          ? await authApi.checkLoginId(form.loginId)
          : await authApi.checkEmail(form.email);
      setChecked((c) => ({ ...c, [field]: result }));
    } catch (e) {
      setError(toMessage(e));
    }
  };

  const passwordMismatch =
    form.passwordConfirm.length > 0 && form.password !== form.passwordConfirm;

  const canSubmit =
    form.loginId &&
    form.password &&
    form.passwordConfirm &&
    !passwordMismatch &&
    form.name &&
    form.email &&
    form.phone &&
    checked.loginId?.available === true &&
    checked.email?.available === true;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const member = await authApi.signUp(form);
      setDone(member);
    } catch (e) {
      setError(toMessage(e));
    } finally {
      setLoading(false);
    }
  };

  if (done) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', backgroundColor: '#f5f5f5' }}>
        <Container maxWidth="sm">
          <Card sx={{ padding: 4, textAlign: 'center' }}>
            <CheckCircleIcon sx={{ fontSize: 56, color: '#2f9e44' }} />
            <Typography variant="h5" sx={{ fontWeight: 'bold', marginTop: 2 }}>
              가입 신청이 접수되었습니다
            </Typography>
            <Alert severity="info" sx={{ marginTop: 3, textAlign: 'left' }}>
              <strong>{done.email}</strong> 으로 인증 메일을 보냈습니다.
              <br />
              메일의 링크를 눌러 인증을 마쳐야 로그인할 수 있습니다.
              <br />
              링크는 24시간 동안 유효합니다.
            </Alert>
            <Typography variant="caption" color="textSecondary" sx={{ display: 'block', marginTop: 2 }}>
              개발 환경에서는 MailHog(http://localhost:8025)에서 메일을 확인할 수 있습니다.
            </Typography>
            <Button variant="contained" fullWidth sx={{ marginTop: 3 }} onClick={() => navigate('/login')}>
              로그인 화면으로
            </Button>
          </Card>
        </Container>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: '#f5f5f5', paddingY: 4 }}>
      <Container maxWidth="sm">
        <Card sx={{ padding: 4 }}>
          <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
            회원가입
          </Typography>
          <Typography variant="body2" color="textSecondary" sx={{ marginBottom: 3 }}>
            가입 후 이메일 인증을 마쳐야 로그인할 수 있습니다.
          </Typography>

          {error && (
            <Alert severity="error" sx={{ marginBottom: 2 }}>
              {error}
            </Alert>
          )}

          <form onSubmit={handleSubmit}>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
              <TextField
                fullWidth
                required
                label="아이디"
                value={form.loginId}
                onChange={set('loginId')}
                error={checked.loginId?.available === false}
                helperText={checked.loginId?.message ?? '영문·숫자·밑줄 4~20자'}
                slotProps={{
                  input: checked.loginId?.available
                    ? {
                        endAdornment: (
                          <InputAdornment position="end">
                            <CheckCircleIcon fontSize="small" sx={{ color: '#2f9e44' }} />
                          </InputAdornment>
                        ),
                      }
                    : undefined,
                }}
              />
              <Button
                variant="outlined"
                sx={{ marginTop: 1, whiteSpace: 'nowrap', minWidth: 96 }}
                disabled={!form.loginId}
                onClick={() => check('loginId')}
              >
                중복확인
              </Button>
            </Stack>

            <TextField
              fullWidth
              required
              type="password"
              label="비밀번호"
              margin="normal"
              value={form.password}
              onChange={set('password')}
              helperText="영문과 숫자를 포함해 8자 이상"
            />
            <TextField
              fullWidth
              required
              type="password"
              label="비밀번호 확인"
              margin="normal"
              value={form.passwordConfirm}
              onChange={set('passwordConfirm')}
              error={passwordMismatch}
              helperText={passwordMismatch ? '비밀번호가 일치하지 않습니다.' : ' '}
            />

            <TextField
              fullWidth
              required
              label="이름"
              margin="normal"
              value={form.name}
              onChange={set('name')}
            />

            <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start', marginTop: 1 }}>
              <TextField
                fullWidth
                required
                type="email"
                label="이메일"
                value={form.email}
                onChange={set('email')}
                error={checked.email?.available === false}
                helperText={checked.email?.message ?? '인증 메일을 받을 주소'}
                slotProps={{
                  input: checked.email?.available
                    ? {
                        endAdornment: (
                          <InputAdornment position="end">
                            <CheckCircleIcon fontSize="small" sx={{ color: '#2f9e44' }} />
                          </InputAdornment>
                        ),
                      }
                    : undefined,
                }}
              />
              <Button
                variant="outlined"
                sx={{ marginTop: 1, whiteSpace: 'nowrap', minWidth: 96 }}
                disabled={!form.email}
                onClick={() => check('email')}
              >
                중복확인
              </Button>
            </Stack>

            <TextField
              fullWidth
              required
              label="전화번호"
              margin="normal"
              value={form.phone}
              onChange={set('phone')}
              placeholder="010-1234-5678"
            />

            <Grid container spacing={2} sx={{ marginTop: 0.5 }}>
              <Grid size={6}>
                <TextField fullWidth label="부서 (선택)" value={form.dept} onChange={set('dept')} />
              </Grid>
              <Grid size={6}>
                <TextField
                  fullWidth
                  label="직급 (선택)"
                  value={form.position}
                  onChange={set('position')}
                />
              </Grid>
            </Grid>

            <Button
              fullWidth
              variant="contained"
              size="large"
              type="submit"
              sx={{ marginTop: 3 }}
              disabled={!canSubmit || loading}
              startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}
            >
              가입하기
            </Button>
            {!canSubmit && (
              <Typography variant="caption" color="textSecondary" sx={{ display: 'block', marginTop: 1 }}>
                아이디와 이메일 중복확인을 마쳐야 가입할 수 있습니다.
              </Typography>
            )}
          </form>

          <Box sx={{ textAlign: 'center', marginTop: 3 }}>
            <Link component="button" type="button" variant="body2" onClick={() => navigate('/login')}>
              이미 계정이 있으신가요? 로그인
            </Link>
          </Box>
        </Card>
      </Container>
    </Box>
  );
}
