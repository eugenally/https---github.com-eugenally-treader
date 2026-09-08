import React, { useEffect, useState } from 'react';
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
  IconButton,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ThumbUpIcon from '@mui/icons-material/ThumbUp';
import ThumbUpOffAltIcon from '@mui/icons-material/ThumbUpOffAlt';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import DownloadIcon from '@mui/icons-material/Download';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { useNavigate, useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { boardApi, toMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const formatSize = (bytes) => {
  if (bytes == null) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
};

export default function PostDetailPage() {
  const { postId } = useParams();
  const navigate = useNavigate();
  const { member } = useAuth();

  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const [commentInput, setCommentInput] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [editingComment, setEditingComment] = useState(null);
  const [editingText, setEditingText] = useState('');

  const [guestDialog, setGuestDialog] = useState(null);   // 'edit' | 'delete'
  const [guestPwd, setGuestPwd] = useState('');

  const notify = (message, severity = 'success') => setToast({ message, severity });

  useEffect(() => {
    (async () => {
      try {
        setPost(await boardApi.post(postId));
      } catch (e) {
        notify(toMessage(e), 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, [postId]);

  const handleLike = async () => {
    try {
      const result = await boardApi.toggleLike(postId);
      setPost((p) => ({ ...p, likedByMe: result.liked, likeCnt: result.likeCnt }));
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleAddComment = async () => {
    try {
      setPost(await boardApi.addComment(postId, {
        content: commentInput,
        parentId: replyTo?.id ?? null,
      }));
      setCommentInput('');
      setReplyTo(null);
      notify('댓글을 등록했습니다.');
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleUpdateComment = async () => {
    try {
      setPost(await boardApi.updateComment(editingComment.id, { content: editingText }));
      setEditingComment(null);
      notify('댓글을 수정했습니다.');
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleDeleteComment = async (commentId) => {
    try {
      setPost(await boardApi.deleteComment(commentId));
      notify('댓글을 삭제했습니다.');
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleAnswered = async () => {
    try {
      setPost(await boardApi.markAnswered(postId, !post.answered));
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  /** 비회원 글이면 비밀번호를 먼저 확인하고, 회원 글이면 바로 진행한다 */
  const startEdit = () => {
    if (post.guestPost && member?.role !== 'ADMIN') {
      setGuestDialog('edit');
      return;
    }
    navigate(`/board/write?edit=${postId}`);
  };

  const startDelete = () => {
    if (post.guestPost && member?.role !== 'ADMIN') {
      setGuestDialog('delete');
      return;
    }
    if (window.confirm('이 글을 삭제할까요?')) {
      doDelete(null);
    }
  };

  const doDelete = async (pwd) => {
    try {
      await boardApi.remove(postId, pwd);
      notify('글을 삭제했습니다.');
      navigate(`/board?board=${post.boardCode}`);
    } catch (e) {
      notify(toMessage(e), 'error');
    }
  };

  const handleGuestConfirm = async () => {
    try {
      await boardApi.verifyGuestPassword(postId, guestPwd);
      const mode = guestDialog;
      setGuestDialog(null);
      if (mode === 'edit') {
        navigate(`/board/write?edit=${postId}&pwd=${encodeURIComponent(guestPwd)}`);
      } else {
        await doDelete(guestPwd);
      }
      setGuestPwd('');
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

  if (!post) {
    return (
      <Layout>
        <Alert severity="error">글을 불러오지 못했습니다.</Alert>
        <Button sx={{ marginTop: 2 }} onClick={() => navigate('/board')}>
          목록으로
        </Button>
      </Layout>
    );
  }

  const rootComments = post.comments.filter((c) => !c.parentId);
  const repliesOf = (id) => post.comments.filter((c) => c.parentId === id);
  const isStaff = member && member.role !== 'CUSTOMER';

  return (
    <Layout>
      <Box>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(`/board?board=${post.boardCode}`)}>
          {post.boardName} 목록
        </Button>

        <Card sx={{ padding: 3, marginTop: 2 }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', marginBottom: 1 }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold', flexGrow: 1 }}>
              {post.title}
            </Typography>
            {post.answered && <Chip label="답변완료" size="small" color="success" />}
          </Stack>

          <Stack
            direction="row"
            spacing={2}
            sx={{ color: '#666', fontSize: 13, flexWrap: 'wrap', gap: 1, marginBottom: 2 }}
          >
            <span>
              {post.writer}
              {post.guestPost && ' (비회원)'}
            </span>
            <span>{post.createdAt?.replace('T', ' ').slice(0, 16)}</span>
            <span>조회 {post.viewCnt}</span>
            <span>좋아요 {post.likeCnt}</span>
          </Stack>

          <Divider />

          <Typography
            variant="body1"
            sx={{ whiteSpace: 'pre-wrap', paddingY: 3, minHeight: 120 }}
          >
            {post.content}
          </Typography>

          {post.files.length > 0 && (
            <>
              <Divider sx={{ marginBottom: 2 }} />
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', marginBottom: 1 }}>
                첨부파일 ({post.files.length})
              </Typography>
              {post.files.map((f) => (
                <Box key={f.id} sx={{ marginBottom: 2 }}>
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Button
                      size="small"
                      startIcon={<DownloadIcon />}
                      onClick={() => boardApi.downloadFile(f.id, f.origName)}
                    >
                      {f.origName}
                    </Button>
                    <Typography variant="caption" color="textSecondary">
                      {formatSize(f.fileSize)} · {f.downloadCnt}회 다운로드
                    </Typography>
                  </Stack>
                  {/* 이미지·영상·오디오는 화면에서 바로 보여준다 */}
                  {f.mediaType === 'IMAGE' && (
                    <Box
                      component="img"
                      src={f.downloadUrl}
                      alt={f.origName}
                      sx={{ maxWidth: '100%', marginTop: 1, border: '1px solid #ddd', borderRadius: 1 }}
                    />
                  )}
                  {f.mediaType === 'VIDEO' && (
                    <Box component="video" src={f.downloadUrl} controls sx={{ maxWidth: '100%', marginTop: 1 }} />
                  )}
                  {f.mediaType === 'AUDIO' && (
                    <Box component="audio" src={f.downloadUrl} controls sx={{ marginTop: 1 }} />
                  )}
                </Box>
              ))}
            </>
          )}

          <Divider sx={{ marginY: 2 }} />

          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
            {post.likeAllowed && (
              <Button
                variant={post.likedByMe ? 'contained' : 'outlined'}
                startIcon={post.likedByMe ? <ThumbUpIcon /> : <ThumbUpOffAltIcon />}
                onClick={handleLike}
              >
                좋아요 {post.likeCnt}
              </Button>
            )}
            <Box sx={{ flexGrow: 1 }} />
            {isStaff && post.boardCode === 'QNA' && (
              <Button startIcon={<CheckCircleIcon />} onClick={handleAnswered}>
                {post.answered ? '답변완료 해제' : '답변완료 표시'}
              </Button>
            )}
            {post.editable && (
              <>
                <Button startIcon={<EditIcon />} onClick={startEdit}>
                  수정
                </Button>
                <Button color="error" startIcon={<DeleteIcon />} onClick={startDelete}>
                  삭제
                </Button>
              </>
            )}
          </Stack>
        </Card>

        {post.commentAllowed && (
          <Card sx={{ padding: 3, marginTop: 2 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
              댓글 {post.commentCnt}
            </Typography>

            {rootComments.length === 0 && (
              <Typography variant="body2" color="textSecondary" sx={{ paddingY: 2 }}>
                첫 댓글을 남겨 보세요.
              </Typography>
            )}

            {rootComments.map((c) => (
              <Box key={c.id}>
                <CommentRow
                  comment={c}
                  editing={editingComment?.id === c.id}
                  editingText={editingText}
                  setEditingText={setEditingText}
                  onStartEdit={() => {
                    setEditingComment(c);
                    setEditingText(c.content);
                  }}
                  onCancelEdit={() => setEditingComment(null)}
                  onSaveEdit={handleUpdateComment}
                  onDelete={() => handleDeleteComment(c.id)}
                  onReply={() => setReplyTo(c)}
                  canReply={Boolean(member)}
                />
                {repliesOf(c.id).map((r) => (
                  <Box key={r.id} sx={{ paddingLeft: 4 }}>
                    <CommentRow
                      comment={r}
                      isReply
                      editing={editingComment?.id === r.id}
                      editingText={editingText}
                      setEditingText={setEditingText}
                      onStartEdit={() => {
                        setEditingComment(r);
                        setEditingText(r.content);
                      }}
                      onCancelEdit={() => setEditingComment(null)}
                      onSaveEdit={handleUpdateComment}
                      onDelete={() => handleDeleteComment(r.id)}
                    />
                  </Box>
                ))}
              </Box>
            ))}

            {member ? (
              <Box sx={{ marginTop: 2 }}>
                {replyTo && (
                  <Alert severity="info" sx={{ marginBottom: 1 }} onClose={() => setReplyTo(null)}>
                    <strong>{replyTo.writer}</strong> 님에게 답글을 답니다.
                  </Alert>
                )}
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  placeholder="댓글을 입력하세요"
                  value={commentInput}
                  onChange={(e) => setCommentInput(e.target.value)}
                />
                <Stack direction="row" sx={{ justifyContent: 'flex-end', marginTop: 1 }}>
                  <Button variant="contained" disabled={!commentInput.trim()} onClick={handleAddComment}>
                    {replyTo ? '답글 등록' : '댓글 등록'}
                  </Button>
                </Stack>
              </Box>
            ) : (
              <Alert severity="info" sx={{ marginTop: 2 }}>
                댓글은 로그인 후 쓸 수 있습니다.
              </Alert>
            )}
          </Card>
        )}

        {/* 비회원 비밀번호 확인 */}
        <Dialog open={Boolean(guestDialog)} onClose={() => setGuestDialog(null)} maxWidth="xs" fullWidth>
          <DialogTitle>비밀번호 확인</DialogTitle>
          <DialogContent>
            <Alert severity="info" sx={{ marginBottom: 2 }}>
              비회원 글은 작성 시 정한 비밀번호를 입력해야 {guestDialog === 'edit' ? '수정' : '삭제'}할 수 있습니다.
            </Alert>
            <TextField
              fullWidth
              autoFocus
              type="password"
              label="비밀번호"
              value={guestPwd}
              onChange={(e) => setGuestPwd(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && guestPwd && handleGuestConfirm()}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setGuestDialog(null)}>취소</Button>
            <Button variant="contained" disabled={!guestPwd} onClick={handleGuestConfirm}>
              확인
            </Button>
          </DialogActions>
        </Dialog>

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

function CommentRow({
  comment, isReply, editing, editingText, setEditingText,
  onStartEdit, onCancelEdit, onSaveEdit, onDelete, onReply, canReply,
}) {
  return (
    <Box
      sx={{
        paddingY: 1.5,
        borderTop: '1px solid #eee',
        opacity: comment.deleted ? 0.5 : 1,
      }}
    >
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', marginBottom: 0.5 }}>
        {isReply && <Typography variant="caption" color="textSecondary">↳</Typography>}
        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
          {comment.writer ?? '(삭제됨)'}
        </Typography>
        <Typography variant="caption" color="textSecondary">
          {comment.createdAt?.replace('T', ' ').slice(0, 16)}
          {comment.updatedAt && !comment.deleted && ' (수정됨)'}
        </Typography>
      </Stack>

      {editing ? (
        <Box>
          <TextField
            fullWidth
            multiline
            rows={2}
            size="small"
            value={editingText}
            onChange={(e) => setEditingText(e.target.value)}
          />
          <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end', marginTop: 1 }}>
            <Button size="small" onClick={onCancelEdit}>
              취소
            </Button>
            <Button size="small" variant="contained" onClick={onSaveEdit}>
              저장
            </Button>
          </Stack>
        </Box>
      ) : (
        <>
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
            {comment.content}
          </Typography>
          {!comment.deleted && (
            <Stack direction="row" spacing={0.5} sx={{ marginTop: 0.5 }}>
              {onReply && canReply && (
                <Button size="small" onClick={onReply}>
                  답글
                </Button>
              )}
              {comment.editable && (
                <>
                  <IconButton size="small" onClick={onStartEdit}>
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={onDelete}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </>
              )}
            </Stack>
          )}
        </>
      )}
    </Box>
  );
}
