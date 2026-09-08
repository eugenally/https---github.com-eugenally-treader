import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Card,
  Chip,
  CircularProgress,
  Grid,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import BusinessIcon from '@mui/icons-material/Business';
import DescriptionIcon from '@mui/icons-material/Description';
import Layout from '../components/Layout';
import { invoiceApi, orderApi, productApi, quotationApi, toMessage } from '../api/client';

const COLORS = ['#1976d2', '#e64980', '#2f9e44', '#f08c00', '#7048e8', '#0c8599'];

const money = (v, digits = 2) =>
  v == null ? '-' : Number(v).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });

function SummaryCard({ icon: Icon, title, value, unit, color }) {
  return (
    <Card sx={{ padding: 2, backgroundColor: color, color: 'white', height: '100%' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="caption" sx={{ opacity: 0.85 }}>
            {title}
          </Typography>
          <Typography variant="h6" sx={{ fontWeight: 'bold', marginTop: 0.5 }} noWrap>
            {value}
            {unit && (
              <Typography component="span" variant="caption">
                {unit}
              </Typography>
            )}
          </Typography>
        </Box>
        <Icon sx={{ fontSize: 40, opacity: 0.35 }} />
      </Box>
    </Card>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        const [orders, quotations, invoices, products] = await Promise.all([
          orderApi.list(),
          quotationApi.list(),
          invoiceApi.list(),
          productApi.list(),
        ]);
        setData({ orders, quotations, invoices, products });
      } catch (e) {
        setToast({ message: toMessage(e), severity: 'error' });
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const stats = useMemo(() => {
    if (!data) return null;
    const { orders, quotations, invoices, products } = data;

    const totalOrderAmount = orders.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0);
    const receivable = invoices.reduce((sum, i) => sum + Number(i.balance || 0), 0);
    const totalStock = products.reduce((sum, p) => sum + Number(p.onHandQty || 0), 0);

    // 거래처별 수주액
    const byCustomer = {};
    orders.forEach((o) => {
      byCustomer[o.customerName] = (byCustomer[o.customerName] || 0) + Number(o.totalAmount || 0);
    });
    const customerSales = Object.entries(byCustomer)
      .map(([name, sales]) => ({ name, sales }))
      .sort((a, b) => b.sales - a.sales);

    // 수주 상태 분포
    const byStatus = {};
    orders.forEach((o) => {
      byStatus[o.status] = (byStatus[o.status] || 0) + 1;
    });
    const statusDist = Object.entries(byStatus).map(([name, value]) => ({ name, value }));

    // 재고 상위
    const topStock = [...products]
      .sort((a, b) => Number(b.onHandQty || 0) - Number(a.onHandQty || 0))
      .slice(0, 6)
      .map((p) => ({ name: p.productCode, qty: Number(p.onHandQty || 0) }));

    return {
      totalOrderAmount,
      receivable,
      totalStock,
      orderCount: orders.length,
      quotationCount: quotations.length,
      invoiceCount: invoices.length,
      topCustomer: customerSales[0],
      customerSales,
      statusDist,
      topStock,
    };
  }, [data]);

  if (loading) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', padding: 6 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  if (!stats) {
    return (
      <Layout>
        <Alert severity="error">데이터를 불러오지 못했습니다. 백엔드가 실행 중인지 확인하세요.</Alert>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 'bold', marginBottom: 3 }}>
          📊 대시보드
        </Typography>

        <Grid container spacing={2} sx={{ marginBottom: 3 }}>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={AttachMoneyIcon}
              title="총 수주액 (USD 기준)"
              value={`$${money(stats.totalOrderAmount)}`}
              color="#1976d2"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={TrendingUpIcon}
              title="미수금 잔액"
              value={`$${money(stats.receivable)}`}
              color="#e64980"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={Inventory2Icon}
              title="총 재고 수량"
              value={money(stats.totalStock, 0)}
              color="#2f9e44"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={BusinessIcon}
              title="TOP 거래처"
              value={stats.topCustomer?.name ?? '-'}
              unit={stats.topCustomer ? ` $${money(stats.topCustomer.sales, 0)}` : ''}
              color="#f08c00"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={DescriptionIcon}
              title="견적 / 수주"
              value={`${stats.quotationCount} / ${stats.orderCount}`}
              unit=" 건"
              color="#7048e8"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <SummaryCard
              icon={ShoppingCartIcon}
              title="발행 인보이스"
              value={stats.invoiceCount}
              unit=" 건"
              color="#0c8599"
            />
          </Grid>
        </Grid>

        <Grid container spacing={3} sx={{ marginBottom: 3 }}>
          <Grid size={{ xs: 12, md: 7 }}>
            <Card sx={{ padding: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                🏢 거래처별 수주액
              </Typography>
              {stats.customerSales.length === 0 ? (
                <Alert severity="info">수주 데이터가 없습니다.</Alert>
              ) : (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={stats.customerSales} layout="vertical" margin={{ left: 20, right: 30 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" />
                    <YAxis dataKey="name" type="category" width={140} tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(v) => `$${money(v)}`} />
                    <Bar dataKey="sales" fill="#1976d2" name="수주액" />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 5 }}>
            <Card sx={{ padding: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                📦 수주 상태 분포
              </Typography>
              {stats.statusDist.length === 0 ? (
                <Alert severity="info">수주 데이터가 없습니다.</Alert>
              ) : (
                <>
                  <ResponsiveContainer width="100%" height={240}>
                    <PieChart>
                      <Pie
                        data={stats.statusDist}
                        cx="50%"
                        cy="50%"
                        innerRadius={55}
                        outerRadius={95}
                        paddingAngle={4}
                        dataKey="value"
                      >
                        {stats.statusDist.map((entry, index) => (
                          <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(v) => `${v} 건`} />
                    </PieChart>
                  </ResponsiveContainer>
                  <Box sx={{ marginTop: 1 }}>
                    {stats.statusDist.map((item, idx) => (
                      <Box
                        key={item.name}
                        sx={{ display: 'flex', alignItems: 'center', gap: 1, marginBottom: 0.5 }}
                      >
                        <Box
                          sx={{
                            width: 12,
                            height: 12,
                            backgroundColor: COLORS[idx % COLORS.length],
                            borderRadius: '50%',
                          }}
                        />
                        <Typography variant="caption">
                          {item.name} — {item.value}건
                        </Typography>
                      </Box>
                    ))}
                  </Box>
                </>
              )}
            </Card>
          </Grid>
        </Grid>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ padding: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                📊 제품별 재고 (상위 6)
              </Typography>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={stats.topStock}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 10 }} angle={-20} textAnchor="end" height={60} />
                  <YAxis />
                  <Tooltip formatter={(v) => money(v, 0)} />
                  <Legend />
                  <Bar dataKey="qty" fill="#2f9e44" name="재고 수량" />
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ padding: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', marginBottom: 2 }}>
                📋 최근 수주
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead sx={{ backgroundColor: '#f0f0f0' }}>
                    <TableRow>
                      <TableCell>수주번호</TableCell>
                      <TableCell>거래처</TableCell>
                      <TableCell align="right">금액</TableCell>
                      <TableCell>상태</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {data.orders.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} align="center" sx={{ color: '#999', padding: 3 }}>
                          수주가 없습니다.
                        </TableCell>
                      </TableRow>
                    )}
                    {data.orders.slice(0, 6).map((o) => (
                      <TableRow key={o.id}>
                        <TableCell>{o.orderNo}</TableCell>
                        <TableCell>{o.customerName}</TableCell>
                        <TableCell align="right">
                          {o.currency} {money(o.totalAmount)}
                        </TableCell>
                        <TableCell>
                          <Chip label={o.status} size="small" />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Card>
          </Grid>
        </Grid>

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
