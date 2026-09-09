# 🚢 TreaderAPP - 수출 오더 관리 시스템

> **수출 영업 한 사이클을 디지털화하는 포트폴리오 프로젝트**
> 
> 견적(Quotation) → 수주(Sales Order) → 출하(Shipment) → 인보이스(Invoice) → 입금 추적

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![React](https://img.shields.io/badge/React-18+-blue)
![MariaDB](https://img.shields.io/badge/MariaDB-10.6-blue)
![Java](https://img.shields.io/badge/Java-21-orange)

---

## 📊 프로젝트 구조

```
treader/
├── src/main/java/com/edu/bootstring/
│   ├── auth/              # M1: 인증/인가 (JWT, 이메일 검증)
│   ├── board/             # M2: 게시판 (자유/Q&A/자료실)
│   ├── customer/          # M3: 거래처·담당자 관리
│   ├── product/           # M3: 제품·단가·재고 관리
│   ├── quotation/         # M4: 견적 작성·변환
│   ├── order/             # M4: 수주 관리
│   ├── shipment/          # M4: 출하 관리
│   ├── invoice/           # M4: 인보이스·입금 추적
│   ├── document/          # M5: PDF·Excel 생성
│   ├── statistics/        # M6: 통계 (윈도우함수·PIVOT)
│   ├── exchange/          # M7: 환율 배치 (Korea Exim Bank API)
│   ├── notification/      # M7: 알림 시스템 (배치 기반)
│   └── global/            # 전역 설정·예외처리
├── frontend-react/        # React SPA (Vite)
├── src/main/resources/db/
│   ├── oracle_ddl.sql     # 25개 테이블 DDL (MariaDB. 파일명은 마이그레이션 이전 이름)
│   ├── sample-data.sql    # 테스트 데이터
│   └── 03-statistics-queries.sql # 통계 쿼리 집합
├── docker-compose.yml     # MailHog (MariaDB 는 --profile mariadb)
└── DEVELOPMENT_GUIDE.md   # 상세 개발 노트
```

---

## 🚀 빠른 시작

### 1️⃣ 사전 요구사항

- **Java 21** (Spring Boot 4.1.1 필요)
- **MariaDB 10.6+** (로컬 설치 또는 docker compose 프로파일)
- **Docker & Docker Compose** (MailHog)
- **Node.js 18+** (React 프론트엔드)

### 2️⃣ 데이터베이스 준비

기본값은 **로컬에 설치된 MariaDB** 를 쓴다. 스키마와 샘플 데이터를 넣는다.

```bash
mysql -h 127.0.0.1 -u root -p -e "CREATE DATABASE IF NOT EXISTS treader_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'treader_user'@'%' IDENTIFIED BY '1234'; GRANT ALL PRIVILEGES ON treader_db.* TO 'treader_user'@'%';"
mysql -h 127.0.0.1 -u treader_user -p1234 treader_db < src/main/resources/db/oracle_ddl.sql
mysql -h 127.0.0.1 -u treader_user -p1234 treader_db < src/main/resources/db/sample-data.sql
```

MailHog(메일 확인용)를 띄운다.

```bash
docker compose up -d
# MailHog: localhost:8025 (웹UI) / SMTP 1025
```

> DB 도 컨테이너로 쓰려면 `docker compose --profile mariadb up -d`.
> 단, 로컬 MariaDB 가 3306 을 쓰고 있으면 먼저 멈추거나 `.env` 의 `MARIADB_PORT` 를 바꿀 것.
> 둘이 같이 뜨면 한쪽은 IPv4, 다른 쪽은 IPv6 에 붙어 "테이블이 안 보이는" 상태가 된다.

### 3️⃣ 백엔드 실행

```bash
./gradlew bootRun
# 서버: http://localhost:8080  (React 화면도 여기서 함께 서빙된다)
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 4️⃣ 프론트엔드 (개발 모드)

```bash
cd frontend-react
npm ci
npm run dev
# http://localhost:5173  (/api 는 8080 으로 프록시된다)
```

프로덕션 빌드는 `src/main/resources/static` 으로 나가 WAR 에 포함된다.

```bash
cd frontend-react && npm run build && cd ..
./gradlew bootWar
```

### 5️⃣ 테스트 로그인

| 역할 | ID | 비밀번호 |
|------|----|----|
| 관리자 | admin | password |

샘플 데이터에는 `admin` 하나만 들어 있다. 영업 계정은 회원가입으로 만든다.

---

## 📚 핵심 기능

### ✅ M1 인증 & 권한 관리
- 회원가입 + 이메일 인증 (MailHog)
- JWT 토큰 기반 인증 (30분 유효)
- 역할별 API 접근 제어 (MEMBER, ADMIN)

### ✅ M2 게시판
- 3종 게시판 (자유/Q&A/자료실) + 댓글 + 좋아요
- 비회원 읽기 지원 (자유게시판)
- 파일 첨부 (다형 참조)

### ✅ M3 마스터 데이터
- 거래처 (담당자 1:N) CRUD
- 제품 + 단가 이력 + 재고 관리
- 엑셀 일괄 가져오기/내보내기

### ✅ M4 주요 업무 흐름
- **견적(Quotation)** → 수주로 변환
- **수주(Sales Order)** → 출하 대기 항목 추출
- **출하(Shipment)** → 인보이스 변환
- **인보이스** → 입금 추적 (CI/PI 구분)
- **상태 전이** + 비관적 락으로 동시성 보호

### ✅ M5 파일 & 문서
- **PDF 생성**: 견적서 / Packing List / 인보이스
- **Excel 처리**: 제품·단가 템플릿 + 대량 등록 (100행 검증)
- **다형 첨부**: 업무문서 + 시스템 자동 생성 문서 혼합

### ✅ M6 통계 & 대시보드
- **월별 매출** (LAG 윈도우 함수로 YoY 비교)
- **거래처/제품 TOP 5** (RANK 윈도우 함수)
- **미수금 Aging** (RATIO_TO_REPORT로 백분율)
- Recharts로 차트 시각화

### ✅ M7 환율 & 알림 배치
- **Korea Exim Bank API** 연동 (USD/EUR/CNY/JPY)
- **4가지 배치 작업** (Asia/Seoul 타임존):
  - 11:30 평일: 환율 수집
  - 00:10 매일: 만료 견적 처리
  - 09:00 매일: D-7 납기 알림
  - 09:10 매일: 연체 송장 처리
- **실시간 알림**: 배지 + 팝오버 UI

---

## 🔐 보안 & 동시성

| 이슈 | 해결책 |
|------|--------|
| 동시 재고 차감 | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |
| 동시 출하 확정 | 수주 행 락 + 재고 트리거 |
| JWT 토큰 탈취 | 짧은 유효 시간 (30분) + HTTPS 권장 |
| 권한 없는 접근 | 메서드 레벨 `@PreAuthorize` |

---

## 📊 데이터 모델 (20개 테이블)

### 핵심 엔티티
- **MEMBER**: 회원 (역할: MEMBER/ADMIN)
- **CUSTOMER**: 거래처
- **CONTACT**: 담당자 (CUSTOMER 1:N)
- **PRODUCT**: 제품
- **PRICE**: 단가 이력 (기간 겹침 방지)
- **QUOTATION** → **SALES_ORDER** → **SHIPMENT** → **INVOICE**
- **SHIPMENT_ITEM** / **SALES_ORDER_ITEM**: 상세
- **PAYMENT**: 입금 기록
- **NOTIFICATION**: 알림 (배치 기반)
- **EXCHANGE_RATE**: 환율 (일자별)

### 감사 컬럼
모든 주요 테이블에 `CREATED_BY`, `CREATED_AT`, `UPDATED_BY`, `UPDATED_AT`

---

## 🛠️ 개발 가이드

### API 명세
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **5개 카테고리**: Auth / Orders / Masters / Documents / Statistics
- 총 **68개 엔드포인트**

### 테스트
```bash
./gradlew test
# 단위 테스트 + 통합 테스트
```

### 빌드 & 배포
```bash
./gradlew clean build
# WAR 파일: build/libs/treader-0.0.1-SNAPSHOT.war
```

### 상세 개발 노트
➡️ [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) 참고
- 78개 핵심 결정사항 기록
- 버그 수정 로그
- 미구현 항목 (리프레시 토큰 등)

---

## 📈 성능 & 규모

| 항목 | 수치 |
|------|------|
| 통합 검증 | 159건 전부 통과 |
| 엑셀 대량 등록 | 최대 100행 |
| PDF 생성 | 3종류 (견적서/PL/인보이스) |
| 배치 작업 | 4가지 (환율/만료/알림/연체) |
| DB 쿼리 | Native SQL (윈도우함수·PIVOT) |

---

## 🔍 주요 기술 결정

### 왜 비관적 락인가?
출하 시 재고 차감이 원자적이어야 하므로 `SELECT FOR UPDATE` 사용.

### 왜 Native SQL인가?
윈도우함수(LAG, RANK)와 피벗 집계는 JPA 로 표현할 수 없다.
MariaDB 전환 시 Oracle 의 RATIO_TO_REPORT·PIVOT 은 윈도우 SUM 과 조건부 집계로 바꿨다.

### 왜 배치 기반 알림인가?
RealTime 알림(WebSocket)의 복잡성을 피하고, 비즈니스 의도(정각 체크)를 명확히.

### 왜 JWT인가?
무상태 REST API를 위해 토큰 기반 인증 선택. 리프레시 토큰은 범위 외.

---

## 📌 마일스톤 체크리스트

- [x] M0: 뼈대 (Gradle, WAR, 전역 예외, Swagger, Docker)
- [x] M1: 인증 (회원가입, 이메일, JWT)
- [x] M2: 게시판 (3종 + 댓글 + 첨부)
- [x] M3: 마스터 (거래처, 제품, 단가, 재고)
- [x] M4: 수주 흐름 (견적→수주→출하→인보이스)
- [x] M5: 파일/문서 (Excel 대량등록, PDF 3종)
- [x] M6: 통계 (윈도우함수, PIVOT, Recharts)
- [x] M7: 환율/알림 (Korea Exim Bank, 배치 4가지)
- [x] M8: 마무리 (README, ERD, Swagger, 정리)

---

## 🤝 기여 & 피드백

이 프로젝트는 **포트폴리오 목적**으로 만들어졌습니다.

기술 질문 및 개선 제안: [Issues](https://github.com/your-repo/issues)

---

## 📄 라이선스

Private Portfolio Project

---

**마지막 업데이트**: 2026-09-09 | **상태**: ✅ M0~M8 완료 | **DB**: MariaDB로 전환

