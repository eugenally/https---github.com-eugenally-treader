import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Snackbar,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { boardApi, toMessage } from '../api/client';

const SEARCH_TYPES = [
  { value: 'ALL', label: '제목+내용' },
  { value: 'TITLE', label: '제목' },
  { value: 'CONTENT', label: '내용' },
  { value: 'WRITER', label: '작성자' },
];

export default function BoardListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [boards, setBoards] = useState([]);
  const [board, setBoard] = useState(null);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const [searchType, setSearchType] = useState('ALL');
  const [keywordInput, setKeywordInput] = useState('');
  const [applied, setApplied] = useState({ type: 'ALL', keyword: '' });

  const boardCode = searchParams.get('board') ?? 'FREE';
  const page = Number(searchParams.get('page') ?? 1);

  const loadPosts = useCallback(
    async (code, pageNo, filter) => {
      const data = await boardApi.posts(code, {
        page: pageNo,
        type: filter.keyword ? filter.type : undefined,
        keyword: filter.keyword || undefined,
      });
      setPageData(data);
    },
    []
  );

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const list = await boardApi.boards();
        setBoards(list);

        const current = list.find((b) => b.boardCode === boardCode) ?? list[0];
        setBoard(current);
        if (current) {
          await loadPosts(current.boardCode, page, applied);
        }
      } catch (e) {
        setToast({ message: toMessage(e), severity: 'error' });
      } finally {
        setLoading(false);
      }
    })();
  }, [boardCode, page, applied, loadPosts]);

  const changeBoard = (code) => {
    setKeywordInput('');
    setApplied({ type: 'ALL', keyword: '' });
    setSearchParams({ board: code, page: '1' });
  };

  const handleSearch = () => {
    setApplied({ type: searchType, keyword: keywordInput.trim() });
    setSearchParams({ board: boardCode, page: '1' });
  };

  if (loading && !pageData) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', padding: 6 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
          📋 게시판
        </Typography>

        <Card sx={{ marginBottom: 2 }}>
          <Tabs
            value={board?.boardCode ?? false}
            onChange={(e, v) => changeBoard(v)}
            variant="scrollable"
            scrollButtons="auto"
          >
            {boards.map((b) => (
              <Tab
                key={b.boardCode}
                value={b.boardCode}
                label={`${b.boardName} (${b.postCount})`}
              />
            ))}
          </Tabs>
        </Card>

        {board && (
          <>
            <Card sx={{ padding: 2, marginBottom: 2 }}>
              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1}
                sx={{ alignItems: 'center' }}
              >
                <FormControl size="small" sx={{ minWidth: 130 }}>
                  <InputLabel>검색 범위</InputLabel>
                  <Select
                    value={searchType}
                    label="검색 범위"
                    onChange={(e) => setSearchType(e.target.value)}
                  >
                    {SEARCH_TYPES.map((t) => (
                      <MenuItem key={t.value} value={t.value}>
                        {t.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <TextField
                  size="small"
                  fullWidth
                  placeholder="검색어를 입력하세요"
                  value={keywordInput}
                  onChange={(e) => setKeywordInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                />
                <Button variant="outlined" startIcon={<SearchIcon />} onClick={handleSearch}>
                  검색
                </Button>
                {board.canWrite && (
                  <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    sx={{ whiteSpace: 'nowrap' }}
                    onClick={() => navigate(`/board/write?board=${board.boardCode}`)}
                  >
                    글쓰기
                  </Button>
                )}
              </Stack>
              {board.guestWriteAllowed && (
                <Typography variant="caption" color="textSecondary" sx={{ display: 'block', marginTop: 1 }}>
                  이 게시판은 비회원도 글을 쓸 수 있습니다. 수정·삭제하려면 작성 시 정한 비밀번호가 필요합니다.
                </Typography>
              )}
            </Card>

            <Card>
              <TableContainer>
                <Table size="small">
                  <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                    <TableRow>
                      <TableCell width={70} align="center">
                        번호
                      </TableCell>
                      <TableCell>제목</TableCell>
                      <TableCell width={110}>작성자</TableCell>
                      <TableCell width={110}>작성일</TableCell>
                      <TableCell width={60} align="center">
                        조회
                      </TableCell>
                      <TableCell width={60} align="center">
                        좋아요
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {pageData?.content.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={6} align="center" sx={{ color: '#999', padding: 4 }}>
                          {applied.keyword ? '검색 결과가 없습니다.' : '등록된 글이 없습니다.'}
                        </TableCell>
                      </TableRow>
                    )}
                    {pageData?.content.map((p) => (
                      <TableRow
                        key={p.id}
                        hover
                        sx={{ cursor: 'pointer' }}
                        onClick={() => navigate(`/board/post/${p.id}`)}
                      >
                        <TableCell align="center">{p.id}</TableCell>
                        <TableCell>
                          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                            <span>{p.title}</span>
                            {p.commentCnt > 0 && (
                              <Typography variant="caption" sx={{ color: '#1976d2' }}>
                                [{p.commentCnt}]
                              </Typography>
                            )}
                            {p.fileCount > 0 && (
                              <AttachFileIcon sx={{ fontSize: 14, color: '#888' }} />
                            )}
                            {p.answered && <Chip label="답변완료" size="small" color="success" />}
                          </Stack>
                        </TableCell>
                        <TableCell>
                          {p.writer}
                          {p.guestPost && (
                            <Typography variant="caption" color="textSecondary">
                              {' '}
                              (비회원)
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>{p.createdAt?.slice(0, 10)}</TableCell>
                        <TableCell align="center">{p.viewCnt}</TableCell>
                        <TableCell align="center">{p.likeCnt}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              {pageData && pageData.totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', padding: 2 }}>
                  <Pagination
                    count={pageData.totalPages}
                    page={pageData.page}
                    color="primary"
                    onChange={(e, value) =>
                      setSearchParams({ board: boardCode, page: String(value) })
                    }
                  />
                </Box>
              )}
            </Card>

            {pageData && (
              <Typography
                variant="caption"
                color="textSecondary"
                sx={{ display: 'block', marginTop: 1, textAlign: 'right' }}
              >
                전체 {pageData.totalElements}건 · {pageData.page}/{pageData.totalPages || 1} 쪽
              </Typography>
            )}
          </>
        )}

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
