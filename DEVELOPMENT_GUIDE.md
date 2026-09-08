# 수출 오더 관리 시스템 (TreaderAPP) — 통합 개발 가이드

> **한 줄 정의**
> 견적(Quotation) → 수주(Sales Order) → 출하(Shipment) → 인보이스(Invoice)로 이어지는
> 수출 영업 실무 한 사이클을 Spring Boot 3 + Oracle + React로 구현하는 포트폴리오 프로젝트.

**최종 수정**: 2026-09-07 | **상태**: 설계 완료, 스프린트 0 대기 중

---

## 📋 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [핵심 원칙 3가지](#핵심-원칙-3가지)
3. [기술 스택](#기술-스택)
4. [개발 순서 (마일스톤)](#개발-순서-마일스톤)
5. [업무 흐름 상세](#업무-흐름-상세)
6. [데이터 모델](#데이터-모델)
7. [상태 전이와 동시성](#상태-전이와-동시성)
8. [핵심 결정사항 (78개)](#핵심-결정사항-78개)

---

## 프로젝트 개요

### 목표

수출 영업 담당자가 **견적을 내고 → 수주로 확정하고 → 출하하고 → 인보이스를 발행해 대금을 회수**하기까지의
한 사이클을 시스템으로 관리한다.

기술 목표는 하나의 업무 흐름 안에 아래를 전부 태우는 것:

| 영역 | 태울 기술 |
|---|---|
| **인증/인가** | Spring Security 6 + JWT, 역할별 화면 분기 |
| **데이터** | JPA 연관관계(1:N, N:M), 검색·페이징·정렬 |
| **트랜잭션** | 상태 전이, 재고 차감 시 비관적 락 |
| **파일** | 업로드/다운로드, Excel(POI) 대량등록, PDF 인보이스 |
| **외부 연동** | 한국수출입은행 환율 API + @Scheduled 배치 |
| **통계** | Oracle 윈도우 함수 / PIVOT + Recharts |
| **실시간** | WebSocket 또는 메일 알림 |

### 범위 (In Scope)

- ✅ 회원 관리 (가입·이메일 인증·로그인·정보수정·비밀번호 찾기)
- ✅ 게시판 3종 (자유 / Q&A / 자료실) + 댓글 + 좋아요 + 조회수
- ✅ 마스터: 거래처, 제품, 단가 이력
- ✅ 트랜잭션: 견적, 수주, 출하, 인보이스, 입금
- ✅ 재고: 단순 수량 관리(창고 1개 가정) + 출하 시 차감
- ✅ 환율: 일별 자동 수집, 인보이스 원화 환산
- ✅ 통계 대시보드: 월별 / 거래처별 / 제품별 매출
- ✅ 알림: 납기 임박, 결제 기한 초과

### 범위 (Out of Scope) — 중요

범위를 못 자르면 완성을 못 한다. 아래는 **명시적으로 제외**한다.

- ❌ 회계 처리(전표, 원장, 부가세 신고)
- ❌ 생산 계획 / BOM / 공정 관리
- ❌ 다중 창고, 로트(LOT) 추적, 유효기간 관리
- ❌ 실제 결제 게이트웨이(PG) 연동
- ❌ L/C 개설·네고 프로세스
- ❌ 관세청 전자통관(UNIPASS) 실연동
- ❌ 다국어(i18n) — 문서 출력물(PI/CI/PL)만 영문, 화면은 한국어

### 성공 기준

1. **로컬에서 데모 시나리오 1회가 끊김 없이 돌아간다:**
   거래처 등록 → 제품 등록 → 견적 작성 → PDF 견적서 → 수주 전환 → 부분 출하 →
   재고 차감 확인 → CI/PL 생성 → 인보이스 발행 → 입금 등록 → 대시보드에 매출 반영

2. **면접에서 설명할 수 있다:**
   "여기서 동시성 문제가 어디서 났고 어떻게 풀었나"에 명확히 답할 수 있어야 한다.

3. **README 하나로 남이 20분 안에 띄울 수 있다.**

---

## 핵심 원칙 3가지

### 1️⃣ 각 단계가 끝날 때마다 배포 가능한 상태를 유지한다

**인증 → 게시판 → 마스터 → 수주흐름 → 파일 → 통계 → 알림.**
앞 단계 없이 뒤 단계를 먼저 만들지 않는다.

### 2️⃣ 필수 요건은 도메인에 흡수시킨다

"게시판 3개"를 과제용으로 따로 만들지 않는다.
**공지/문의/자료실**로 ERP의 일부가 되게 한다.
→ 이것이 포트폴리오의 차별점이 된다.

### 3️⃣ 화면보다 상태 전이가 먼저다

견적서 화면이 예쁜 것보다, **견적→수주 전환이 트랜잭션 안에서 정확한 게 훨씬 어렵고 값지다.**
면접에서 승부가 갈리는 부분이다.

---

## 기술 스택

### Backend
- **JDK 21** + **Spring Boot 3.x**
- **Oracle Database** (테이블 20개, 시퀀스, 인덱스)
- **Spring Data JPA** (Hibernate)
- **Spring Security 6** + JWT
- **Apache POI** (Excel 대량등록)
- **iText / Flying Saucer** (PDF 생성)
- **Jakarta Mail** (메일 발송)
- **Gradle** (빌드)
- **Tomcat 10.1** (WAR 배포)

### Frontend
- **React 18+** (Vite)
- **Material UI (MUI)** (컴포넌트)
- **Recharts** (통계 차트)
- **Axios** (HTTP 클라이언트)
- **Zustand or Context API** (상태 관리)

### DevOps
- **Docker Compose** (로컬 Oracle + MailHog)
- **Git / GitHub** (버전 관리)
- **Swagger** (API 문서)

---

## 개발 순서 (마일스톤)

### 📍 현 상태

- ✅ 기획 문서 완성 (프로젝트 개요, 업무 흐름, 요구사항)
- ✅ 설계 문서 완성 (ERD, API 명세, 화면 설계)
- ✅ 결정사항 78개 확정 (D1~D8 모두 완료)
- ⏳ **스프린트 0 시작 예정 (2026-09-07)**

---

### 🚀 스프린트 0 — 9/7~9/9 설계 발표용 목업 (3일)

> **목표**: "구상이 이 정도로 구체적이다"를 보여주는 것.
> 이건 M0의 일부를 앞당겨 하는 것이지, 버리는 작업이 아니다.

#### 9/7 오후 — DB를 실물로 만든다

| 시간 | 할 일 | 산출물 |
|---|---|---|
| 1h | Docker Compose (Oracle + MailHog) 실행, 접속 확인 | 컨테이너 2개 |
| 1h | Oracle DDL 전체 실행 (테이블 20개 + 시퀀스 + 인덱스) | DB 구조 완성 |
| 2h | 샘플 데이터 INSERT — 거래처 5 / 제품 8 / 견적 3 / 수주 2 / 출하 2 / 인보이스 2 | sample-data.sql |
| 0.5h | 통계 쿼리(PIVOT) 실제 실행, 숫자 확인 | 대시보드에 쓸 실제 값 |

**💡 핵심**: 화면은 목업이어도 **DB는 진짜**다.
SQL Developer로 PIVOT 쿼리를 돌려 보여주는 게 예쁜 화면보다 설득력 있다.

#### 9/8 종일 — React 화면 5장 목업

Vite + React + MUI. **API 호출 없이 더미 JSON을 import해서 렌더**한다.

| 순번 | 화면 | 보여줄 것 |
|---|---|---|
| 1 | **로그인** | 아이디 저장 체크박스(쿠키 실제 동작), 역할별 진입 |
| 2 | **견적 작성** ⭐ | 제품 다중선택 모달, 단가 자동제안, 실시간 합계, 원화 병기 |
| 3 | **수주 상세** ⭐ | 상태 배지, 품목별 주문량/기출하/잔량, 출하 이력 타임라인 |
| 4 | **출하 등록** | 잔량 기반 입력, Ocean/Air 전환 시 필드 변경 |
| 5 | **대시보드** | 요약 카드 6개 + 월별 추이 + 거래처 TOP + 미수금 Aging |

**공통**: 좌측 사이드바, 상태 배지 색상, MUI Pagination

#### 9/9 오전 — 발표 정리

- [ ] 옵시디언 노트를 발표 순서대로 정렬
- [ ] 수주 흐름 규칙 14개 표를 슬라이드 한 장으로 (차별화 포인트)
- [ ] 데모 순서 리허설: DB 쿼리 → 화면 5장 → 동시성 설계 설명
- [ ] 예상 질문 대비

**발표에서 말할 한 문장**:
> "화면은 아직 목업이지만, **업무 규칙 78개를 먼저 정하고 스키마를 확정한 뒤** 만들고 있습니다.
> 재고 차감을 어느 시점에 할지, 매출을 선적일로 잡을지 발행일로 잡을지 —
> 이런 걸 12년 무역 실무에서 알고 있어서 설계에 바로 넣을 수 있었습니다."

---

### 🔨 M0. 뼈대 세우기 (2~3일)

> 스프린트 0에서 DDL 실행·샘플데이터·프론트 초기화는 이미 끝나 있다.
> M0에서 실제로 남는 건 **백엔드 뼈대와 WAR 배포**다.

| 할 일 | 참고 |
|---|---|
| ✅ Gradle 프로젝트 생성 (JDK 21, WAR 설정) | D6-01 결정 |
| ✅ `ServletInitializer` 추가 | Tomcat 배포용 |
| ✅ 패키지 구조 생성 (8개 레이어) | [[26-패키지 구조]] |
| ✅ 전역 설정: `GlobalExceptionHandler`, `ErrorCode`, `PageResponse` | |
| ✅ 기본 엔티티: `BaseTimeEntity`, `YesNoConverter` | |
| ✅ Swagger 연결 | API 문서 자동화 |
| ✅ Vite + React 프로젝트 생성, 프록시 설정 | |
| ✅ **WAR 빌드 → Tomcat 배포 성공 확인** ⭐ | `/api/health` 응답 확인 |

**DoD**: `/api/health`가 200을 반환하고, WAR로 톰캣에 올렸을 때도 동일하게 응답한다.

**🎯 왜 배포를 맨 처음에 성공시키는가?**
마지막에 하면 시간이 없다. 여기서 하루 쓰는 게 나중에 사흘을 아낀다.

---

### 🔑 M1. 인증 (1주)

필수요건 #1~8, #14~16을 포함한다.

**Backend**:
- [ ] `Member`, `EmailToken` 엔티티 + 리포지토리
- [ ] 회원가입 + 아이디/이메일 중복확인
- [ ] 이메일 인증 (토큰 발급/검증, MailHog 발송)
- [ ] JWT 발급/검증 필터, `SecurityConfig`
- [ ] 로그인 / 로그아웃 / 토큰 갱신
- [ ] 비밀번호 찾기 → 임시비밀번호 이메일 발송
- [ ] 회원정보 수정 / 비밀번호 변경
- [ ] 관리자 회원 관리 화면용 API (조회, 비활성화)

**Frontend**:
- [ ] 로그인·가입·이메일인증·비번찾기 화면
- [ ] `AuthContext` (JWT 토큰 관리), 라우트 가드
- [ ] 아이디 저장 쿠키 (옵시디언: D3-10)
- [ ] 로그인 실패 메시지, 로딩 상태

**DoD**: 가입 → 메일 인증 → 로그인 → 정보수정 → 비번찾기 → 임시비번 로그인 → 변경 강제까지
**전 과정이 브라우저에서 끊김 없이 동작**한다.

---

### 📝 M2. 게시판 (1주)

필수요건 #9~13, #17을 포함한다.

**Backend**:
- [ ] `Board`, `Post`, `PostComment`, `PostFile`, `PostLike` 엔티티
- [ ] 게시판 공통 CRUD 서비스 (`boardCode` 분기)
  - 자유게시판: 비회원 쓰기 + 비밀번호 수정/삭제
  - Q&A: 댓글 CRUD
  - 자료실: 파일 업로드/다운로드, `mediaType` 판정
- [ ] 조회수 쿠키 처리 (서버 판정, 자정 만료)
- [ ] 좋아요 토글 (fetch() 이용)
- [ ] 서버 페이징 + 검색 + 정렬
- [ ] 파일 업로드 (MultipartFile 처리)

**Frontend**:
- [ ] 게시판 목록 / 상세 / 작성 / 수정 화면
- [ ] `Pagination` 컴포넌트 (M2에서 만들고 M3+ 재사용)
- [ ] `FileViewer` 컴포넌트 (이미지, 영상, PDF 렌더)
- [ ] `LikeButton`, `CommentSection` 컴포넌트
- [ ] 파일 업로드 폼

**DoD**: 과정 필수요건 시연 시나리오 1~6번이 전부 통과한다.
**여기까지가 "과제 요건 충족" 지점.** 남은 시간을 전부 M3 이후에 쏟는다.

---

### 🏪 M3. 마스터 CRUD (1주)

**Backend**:
- [ ] `Customer` + `CustomerContact` (1:N) 엔티티
- [ ] `Product` CRUD
- [ ] `PriceHistory` 엔티티 (기간 중복 처리, 적용단가 조회 API)
- [ ] `Inventory` 조회 + 초기화
- [ ] 검색·페이징·정렬 (M2의 `Pagination` 로직 재사용)

**Frontend**:
- [ ] 거래처 목록 / 등록 / 수정 화면
- [ ] 제품 목록 / 등록 / 수정 화면
- [ ] 단가 이력 목록 화면
- [ ] 재고 조회 화면

**DoD**: 거래처 3곳, 제품 5개, 단가 10건을 등록하고 검색·페이징이 정상 동작한다.

---

### 🚢 M4. 수주 흐름 (2주) — ⭐ 이 프로젝트의 본체

> **이것이 핵심이다.** 여기서 일어나는 모든 일이 면접 질문의 대상이다.

**Backend**:
- [ ] `DocNumberService` — 거래처별 문서번호 채번 (SO-ABC-2026-0001)
- [ ] `Quotation` CRUD + 품목 라인 + 단가 자동제안
- [ ] 견적 상태 전이 (enum 상태머신: DRAFT → SENT → ACCEPTED/REJECTED/EXPIRED)
  - `SUPERSEDED`: 개정판 발행 시 이전 건 자동 전환
  - `EXPIRED`: 배치가 매일 자동 처리
- [ ] **견적 → 수주 전환** (1트랜잭션) ⭐
  - 견적 상태 ACCEPTED로 변경
  - SalesOrder 생성 (견적 헤더/라인 **복사** - 스냅샷)
  - 재고 할당 (allocated_qty 증가)
- [ ] `SalesOrder` CRUD + 품목 라인
- [ ] 수주 상태 전이 (CONFIRMED → IN_PRODUCTION → PARTIALLY_SHIPPED → SHIPPED → CLOSED)
- [ ] `Shipment` 등록 + 잔량 검증
- [ ] **재고 차감** (`SELECT FOR UPDATE` 비관적 락) ⭐
  - SHIPPED 전환 시점에만 실행
  - 데드락 회피: 제품 ID 오름차순 정렬
- [ ] 수주 상태 자동 재판정 (부분/전량 출하 판정)
- [ ] `Invoice` (PI/CI) 발행 + 환율 고정
  - PI: 견적 단계에서도 발행 가능 (선금 용)
  - CI: 출하 1건 = 발행 1장
  - 환율 기준일: PI는 발행일, CI는 선적일(ETD)
- [ ] 입금(`Payment`) 등록 + 인보이스 상태 자동 판정 (ISSUED → PAID)

**Frontend**:
- [ ] 견적 목록 / 상세 / 작성 화면
  - 제품 다중선택 모달, 단가 자동제안, 실시간 합계, 원화 병기
  - 견적 상태 버튼: SENT, DRAFT 회수 등
- [ ] 수주 목록 / 상세 화면
  - 상태 배지, 품목별 주문량/기출하/잔량, 출하 이력 타임라인
  - 출하/인보이스 버튼
- [ ] 출하 등록 화면
  - 수주 선택 → 잔량 표시 → 수량 입력 → SHIPPED 전환
  - 중량/CBM 자동 계산 (수정 가능)
- [ ] 인보이스 목록 / 상세 / PDF 생성 화면
  - PI 선금 금액 표시, CI 선적 날짜
  - 결제기한 계산 (B/L date + creditDays)
- [ ] 입금 등록 화면
  - 인보이스별 미수금 표시, 입금 입력, 잔금 자동 계산

**동시성 테스트** ⭐ (코드로 남겨야 한다!):
```java
@Test
void 동시_출하_등록시_재고가_음수가_되지_않는다() throws Exception {
    int threads = 10;
    CountDownLatch latch = new CountDownLatch(threads);
    ExecutorService es = Executors.newFixedThreadPool(threads);
    AtomicInteger success = new AtomicInteger();
    for (int i = 0; i < threads; i++) {
        es.submit(() -> {
            try {
                shipmentService.confirmShipment(req);
                success.incrementAndGet();
            } catch (InsufficientStockException ignored) {}
            finally { latch.countDown(); }
        });
    }
    latch.await();
    assertThat(inventoryRepo.findById(productId).get().getOnHandQty())
        .isGreaterThanOrEqualTo(BigDecimal.ZERO);
}
```

**DoD**: 데모 시나리오 전체가 돌아간다:
- 견적 작성 → 수주 전환 → 3000개 부분출하 → 재고 감소 확인 → 2000개 재출하
- 수주 SHIPPED → CI 발행 → 입금 → PAID → 수주 CLOSED
- **동시 출하 등록 테스트에서 재고가 음수가 되지 않는다.**

---

### 📄 M5. 파일/문서 (1주)

**Backend**:
- [ ] `Attachment` 엔티티 (다형 참조: ref_type + ref_id)
- [ ] 파일 업로드/다운로드 (S3 또는 로컬 스토리지)
- [ ] 제품·단가 Excel 대량 등록 (POI)
  - 헤더 검증, 데이터 파싱, 부분 실패 시 리포트 생성
- [ ] Excel 업로드 양식 다운로드
- [ ] 목록 Excel 내보내기
- [ ] 인보이스 PDF 생성 (iText / Flying Saucer)
  - HTML 템플릿 → PDF, 한글 폰트 적용
  - PI, CI 서로 다른 양식
- [ ] Packing List PDF
- [ ] 견적서 PDF

**Frontend**:
- [ ] 파일 업로드 드래그앤드롭
- [ ] 파일 목록 표시
- [ ] Excel 다운로드 / 업로드 폼

**DoD**: 제품 100건 엑셀 업로드 성공 (오류행은 사유와 함께 리포트),
인보이스 PDF가 한글·영문 깨짐 없이 출력된다.

---

### 📊 M6. 통계 (3~4일)

**Backend**:
- [ ] `StatisticsQueryRepository` — 네이티브 쿼리 5종
  - 월별 매출 (전년 대비, LAG 윈도우 함수)
  - 거래처별/제품별 TOP N (RANK, RATIO_TO_REPORT)
  - 거래처×월 PIVOT (행 → 열)
  - 미수금 Aging (구간별 분류)
- [ ] 대시보드 API (요약 카드, 차트 데이터)

**Frontend**:
- [ ] 대시보드 레이아웃 (카드 + 차트)
- [ ] 요약 카드 6개: 당월 매출, 월말 미수금, 월말 재고, TOP 거래처, TOP 제품, 최근 주문수
- [ ] Recharts 차트 4개:
  - 월별 추이 (라인 차트, 전년 대비)
  - 거래처별 매출 (가로 막대)
  - 제품별 매출 (원형)
  - 미수금 Aging (누적 면적 차트)
- [ ] 날짜 필터 (월/연도 선택)

**DoD**: 대시보드가 실제 데이터로 채워지고, 요약 카드 5개가 정확한 값을 보여준다.

---

### 🔄 M7. 환율/알림 (3~4일)

**Backend**:
- [ ] 한국수출입은행 API 클라이언트
  - RestTemplate / WebClient로 환율 데이터 수집
- [ ] `@Scheduled` 일별 수집 (평일 11:30)
  - 재시도 로직 (@Retryable)
  - 폴백 (어제 데이터 사용)
- [ ] 환율 조회 화면용 API
- [ ] 배치 3종:
  - 견적 만료 (매일 00:10)
  - 납기 D-7 알림 (매일 09:00)
  - 결제기한 초과 판정 (매일 09:10)
- [ ] 알림 저장/조회 (Notification 엔티티)
- [ ] WebSocket 또는 메일 알림 발송

**Frontend**:
- [ ] 환율 조회 화면
- [ ] 알림함 화면
  - 미읽음 배지
  - 알림 클릭 시 해당 문서로 이동

**DoD**: 배치를 수동 트리거하면 환율이 저장되고, 납기 임박 수주에 알림이 생성된다.

---

### ✨ M8. 마무리 (1주)

**문서화**:
- [ ] README 작성 (설치·실행·데모 계정·스크린샷)
- [ ] ERD 이미지 출력
- [ ] API 문서 정리 (Swagger 확인)
- [ ] ADR (Architectural Decision Record) 작성 — 주요 결정 5~7개

**테스트**:
- [ ] 핵심 서비스 단위 테스트 (M1~M4)
- [ ] 동시성 테스트 (재고 차감)

**정리**:
- [ ] 코드 리뷰 (죽은 코드 제거, TODO 정리)
- [ ] 샘플 데이터 스크립트 (거래처 5, 제품 10, 견적 20, 수주 15, 출하 20, 인보이스 15)
- [ ] 데모 시나리오 영상 (3~5분)

**DoD**: 다른 사람이 README만 보고 20분 안에 실행할 수 있다.

---

### 📈 전체 일정

| 단계 | 기간 | 누적 | 상태 |
|---|---|---|---|
| 스프린트 0 | 3일 | 3일 | ⏳ |
| M0 뼈대 | 3일 | 6일 | ⏳ |
| M1 인증 | 1주 | ~1.5주 | ⏳ |
| M2 게시판 | 1주 | ~2.5주 | ⏳ |
| M3 마스터 | 1주 | ~3.5주 | ⏳ |
| **M4 수주흐름** | **2주** | ~5.5주 | ⏳ |
| M5 파일/문서 | 1주 | ~6.5주 | ⏳ |
| M6 통계 | 4일 | ~7주 | ⏳ |
| M7 환율/알림 | 4일 | ~7.5주 | ⏳ |
| M8 마무리 | 1주 | **~8.5주** | ⏳ |

> **하루 6~8시간 기준.** 처음 잡는 일정은 늘 낙관적이니 **1.5배**로 보는 게 현실적이다.
> **시간이 부족하면**: M5의 PL PDF, M7의 WebSocket을 먼저 잘라낸다. **M4는 절대 자르지 않는다.**

---

## 업무 흐름 상세

### 전체 시퀀스

```
거래처(Buyer)
    ↓ (문의)
영업담당 → 견적 작성 (제품·수량·단가·Incoterms)
    ↑ (단가 자동제안)
    ← 단가 이력
    
견적 생성 (상태: DRAFT)
    ↓ (발송)
견적 발송 (상태: SENT)
    ↓ (바이어 수락)
견적 수락 (상태: ACCEPTED)
    ↓ (1트랜잭션에서)
수주 생성 (스냅샷 복사)
재고 할당 (allocated_qty +=)
    
수주 확정 (상태: CONFIRMED)
    ↓ (생산 지시)
수주 생산중 (상태: IN_PRODUCTION)
    ↓ (선금 청구)
PI 발행 (선금)
    ↓ (선금 입금)
    
PI 발행 (상태: ISSUED)
    ↓ (선금 입금)
PI 결제 (상태: PAID)
    ↓ (생산/출고)
출하 등록 (상태: PLANNED)
    ↓ (포장)
출하 포장 (상태: PACKED)
    ↓ (확정 - 여기서 재고 차감!)
출하 확정 (상태: SHIPPED)
재고 차감 (on_hand_qty -=)
    
수주 상태 판정
    ├→ 전량 출하 (상태: SHIPPED)
    └→ 부분 출하 (상태: PARTIALLY_SHIPPED)
    
CI 발행
인보이스 발행 (상태: ISSUED)
    ↓ (잔금 입금)
잔금 입금
    ↓
인보이스 결제 (상태: PAID)
수주 완료 (상태: CLOSED)
```

### 1단계. 견적 (Quotation)

**입력**: 거래처, 통화, Incoterms, POL/POD, 유효기한, 결제조건, 품목 라인

**핵심 로직**:
- 품목 추가 시 `PriceHistory`에서 거래처 전용 단가 조회 (없으면 표준 단가)
- 합계 = Σ(수량 × 단가)
- 저장 시 **환율 스냅샷** 함께 기록 (원화 매출 통계의 기준)

**상태 전이**:
```
DRAFT → SENT → ACCEPTED / REJECTED / EXPIRED / SUPERSEDED
```

### 2단계. 수주 (Sales Order) — 💎 핵심

**견적 → 수주 전환 (1트랜잭션)**:
1. 견적 상태 ACCEPTED로 변경 (중복 전환 방지)
2. SalesOrder 생성, 견적 헤더/라인을 **복사** (참조 아님)
3. 라인별 `ordered_qty` 세팅, `shipped_qty = 0`

**왜 복사인가**?
견적서는 과거 시점의 증빙이다. 견적이 나중에 수정돼도 수주는 변하면 안 된다.
→ **스냅샷 패턴**. 면접에서 설명하기 좋은 설계 결정.

**상태 전이**:
```
CONFIRMED → IN_PRODUCTION → PARTIALLY_SHIPPED → SHIPPED → CLOSED / CANCELLED
```

### 3단계. 출하 (Shipment) — 💎 재고 동시성

**재고 차감 시점** (중요):
| 시점 | 처리 |
|---|---|
| 수주 확정 | `allocated_qty` += 주문량 |
| 출하 등록 (`PLANNED`) | 재고 변동 없음 (계획일 뿐) |
| 출하 포장 (`PACKED`) | 재고 변동 없음 |
| **출하 확정 (`SHIPPED`)** | **`on_hand_qty` -=** ← 여기서만 차감 |

**동시성 처리**: `SELECT ... FOR UPDATE` (비관적 락)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Inventory> findByProductIdForUpdate(Long productId);
```

**상태 전이**:
```
PLANNED → PACKED → SHIPPED → ARRIVED
```

### 4단계. 인보이스 (Invoice)

**PI** (Proforma Invoice):
- 견적 단계에서도 발행 가능 (선금 용)
- 금액 = 총액 × CUSTOMER.ADVANCE_RATE (기본 30%)

**CI** (Commercial Invoice):
- 출하 1건 = CI 1장
- 부분 선적이면 CI도 여러 장

**환율 고정**:
- PI → 발행일 환율
- CI → **선적일(ETD) 환율** (매출 인식 기준)

**결제기한**:
- PI → 발행일 + 7일 (선적 전 입금)
- CI → **B/L date + creditDays** (실무 표준)

**상태 전이**:
```
ISSUED → PARTIALLY_PAID → PAID / OVERDUE
```

---

## 데이터 모델

### ERD 요약

```
A. 회원·게시판 영역
├─ MEMBER (아이디, 암호, 이메일, 역할)
├─ EMAIL_TOKEN (토큰, 유효기한)
├─ BOARD (게시판 마스터)
├─ POST (게시글)
├─ POST_COMMENT (댓글)
├─ POST_FILE (첨부파일)
└─ POST_LIKE (좋아요)

B. 무역 업무 영역
├─ CUSTOMER (거래처, 단가 기본값)
├─ CUSTOMER_CONTACT (연락처 1:N)
├─ PRODUCT (제품)
├─ PRICE_HISTORY (단가 이력, N:M + 시간)
├─ INVENTORY (현재 재고)
├─ INVENTORY_TXN (재고 이동 이력)
├─ QUOTATION (견적)
├─ QUOTATION_ITEM (견적 품목)
├─ SALES_ORDER (수주)
├─ SALES_ORDER_ITEM (수주 품목)
├─ SHIPMENT (출하)
├─ SHIPMENT_ITEM (출하 품목)
├─ INVOICE (인보이스 PI/CI)
├─ PAYMENT (입금)
├─ EXCHANGE_RATE (환율)
├─ ATTACHMENT (다형 파일)
├─ DOC_NUMBER (문서번호 채번)
└─ NOTIFICATION (알림)
```

### 핵심 관계

| 관계 | 카디널리티 | 이유 |
|---|---|---|
| Quotation : SalesOrder | 1 : 0..1 | 견적 없이 바로 수주도 가능 |
| SalesOrder : Shipment | 1 : N | **부분 선적 허용** (프로젝트의 핵심 난이도) |
| SalesOrder : Invoice | 1 : N | PI 1장 + CI 여러 장 |
| Quotation : Invoice | 1 : N | **견적 단계 PI** 가능 |
| Shipment : Invoice | 1 : 1 | 출하 1건 = CI 1장 |
| Customer × Product | N : M | PRICE_HISTORY 해소 |

### 중요 테이블 설계

#### PRICE_HISTORY — N:M + 시간 차원
```
(customer_id, product_id, valid_from, valid_to, currency, unit_price)
```
- `customer_id IS NULL` = 표준 단가 (모든 거래처 공통 fallback)
- 단가 조회: 거래처 전용 우선 → 표준 단가 fallback
- 기간 중복 방지: 서비스 레이어에서 검증

#### INVENTORY — 동시성 대상
```
product_id(PK), on_hand_qty, allocated_qty, version
```
- `available = on_hand_qty - allocated_qty` (음수 가능 = 백오더)
- 갱신은 항상 `SELECT ... FOR UPDATE`
- INVENTORY_TXN에 모든 변동 이력

#### ATTACHMENT — 다형 참조
```
ref_type(QUOTATION/SALES_ORDER/SHIPMENT/INVOICE) + ref_id
```
JPA 관계 불가지만, 4개 테이블보다 낫다.

---

## 상태 전이와 동시성

### 상태 머신 구현

```java
public enum OrderStatus {
    CONFIRMED, IN_PRODUCTION, PARTIALLY_SHIPPED, SHIPPED, CLOSED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
        CONFIRMED,         EnumSet.of(IN_PRODUCTION, CANCELLED),
        IN_PRODUCTION,     EnumSet.of(PARTIALLY_SHIPPED, SHIPPED, CANCELLED),
        PARTIALLY_SHIPPED, EnumSet.of(PARTIALLY_SHIPPED, SHIPPED),
        SHIPPED,           EnumSet.of(CLOSED),
        CLOSED,            EnumSet.noneOf(OrderStatus.class),
        CANCELLED,         EnumSet.noneOf(OrderStatus.class)
    );

    public void validateTransitionTo(OrderStatus next) {
        if (!ALLOWED.getOrDefault(this, Set.of()).contains(next)) {
            throw new InvalidStateTransitionException(
                "%s → %s 전이는 허용되지 않습니다".formatted(this, next));
        }
    }
}
```

엔티티 안에 전이 메서드:
```java
@Entity
public class SalesOrder {
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public void changeStatus(OrderStatus next) {
        this.status.validateTransitionTo(next);
        this.status = next;
    }
}
```

### 견적 → 수주 전환 (트랜잭션 경계)

```java
@Service
@Transactional
public Long convertToOrder(Long quotationId, ConvertRequest req) {
    // 1) 견적 조회 + 행 잠금
    Quotation q = quotationRepo.findByIdForUpdate(quotationId)
        .orElseThrow(() -> new NotFoundException("견적을 찾을 수 없습니다"));

    // 2) 상태 검증
    if (orderRepo.existsByQuotationId(quotationId)) {
        throw new BusinessException("이미 수주로 전환된 견적입니다");
    }

    // 3) 견적 상태 변경
    q.accept();

    // 4) 수주 생성 — 스냅샷 복사
    SalesOrder order = SalesOrder.builder()
        .orderNo(docNumberService.next("SO"))
        .quotation(q)
        .customer(q.getCustomer())
        // ... 헤더 복사
        .status(OrderStatus.CONFIRMED)
        .build();

    // 라인 복사
    int lineNo = 1;
    for (QuotationItem qi : q.getItems()) {
        order.addItem(SalesOrderItem.builder()
            .lineNo(lineNo++)
            .product(qi.getProduct())
            .orderedQty(qi.getQty())
            .unitPrice(qi.getUnitPrice())     // ← 가격 스냅샷
            .build());
    }

    // 5) 재고 할당
    order.getItems().forEach(i ->
        inventoryService.allocate(i.getProduct().getId(), i.getOrderedQty()));

    return orderRepo.save(order).getId();
}
```

**트랜잭션 경계** = 서비스 메서드 1개.
예외 발생 시 견적 상태, 수주, 재고 할당 **모두 롤백**.

### 재고 차감 — `SELECT FOR UPDATE`

**문제**: 재고 100개, A와 B가 동시에 각각 60개씩 출하
```
Lost Update:
A: SELECT on_hand → 100    B: SELECT on_hand → 100
A: 100 >= 60 통과           B: 100 >= 60 통과
A: UPDATE on_hand = 40      B: UPDATE on_hand = 40
결과: 120개 출하됐는데 재고는 40 (20개가 허공에서 생겨남)
```

**해결 (비관적 락)**:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
Optional<Inventory> findByProductIdForUpdate(Long productId);
```

생성되는 SQL:
```sql
SELECT * FROM INVENTORY WHERE PRODUCT_ID = ? FOR UPDATE WAIT 3
```

**사용**:
```java
@Transactional
public void confirmShipment(Long shipmentId) {
    Shipment sh = shipmentRepo.findById(shipmentId).orElseThrow();
    sh.changeStatus(SHIPPED);
    
    // 데드락 회피: 항상 같은 순서로 잠금
    sh.getItems().stream()
      .sorted(comparing(i -> i.getProduct().getId()))
      .forEach(i -> inventoryService.deduct(i.getProduct().getId(), i.getQty()));
}

public void deduct(Long productId, BigDecimal qty) {
    Inventory inv = inventoryRepo.findByProductIdForUpdate(productId)   // ← B는 여기서 대기
        .orElseThrow(() -> new NotFoundException("재고 정보 없음"));

    if (inv.getOnHandQty().compareTo(qty) < 0) {
        throw new InsufficientStockException(...);
    }

    inv.deduct(qty);  // on_hand -= qty
}   // ← 커밋 시 락 해제, 그제서야 B 진행
```

### 데드락 회피

여러 제품을 한 번에 차감할 때 **잠그는 순서를 항상 동일하게**:

```java
List<Long> productIds = items.stream()
    .map(i -> i.getProduct().getId())
    .sorted()   // ★ 정렬이 데드락을 막는다
    .toList();
for (Long pid : productIds) { ... }
```

### 문서번호 채번

**D2-07 반영**: 거래처별로 돈다.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public String next(String docType, String customerCode) {
    int year = Year.now().getValue();
    DocNumber dn = docNumberRepo.findForUpdate(docType, customerCode, year)
        .orElseGet(() -> docNumberRepo.save(new DocNumber(docType, customerCode, year, 0)));
    int seq = dn.increment();
    return "%s-%s-%d-%04d".formatted(docType, customerCode, year, seq);  // SO-ABC-2026-0001
}

// D5-03: 견적 개정판은 새 번호를 따지 않음
public String nextRevision(String baseQuoteNo, int revNo) {
    return "%s-R%d".formatted(stripRev(baseQuoteNo), revNo);             // QT-ABC-2026-0001-R1
}
```

### 배치 (`@Scheduled`)

```java
@Component
@RequiredArgsConstructor
public class DailyBatchJob {

    // 매일 00:10 — 유효기한 지난 견적 만료
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void expireQuotations() {
        int n = quotationRepo.expireOverdue(LocalDate.now());
        log.info("견적 만료 처리 {}건", n);
    }

    // 매일 09:00 — 납기 D-7 알림
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void notifyDeliveryDue() { ... }

    // 매일 09:10 — 결제기한 초과 판정
    @Scheduled(cron = "0 10 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void markOverdueInvoices() {
        invoiceRepo.markOverdue(LocalDate.now());
    }

    // 평일 11:30 — 환율 수집
    @Scheduled(cron = "0 30 11 * * MON-FRI", zone = "Asia/Seoul")
    public void fetchExchangeRates() { ... }
}
```

---

## 핵심 결정사항 (78개)

### D1. 범위와 일정 결정 (8개) ✅

- ✅ D1-01: 프로젝트 완성 목표일
- ✅ D1-02: 부분 출하 허용
- ✅ D1-03: 거래처 로그인(ROLE_CUSTOMER) 포함 여부
- ✅ D1-04: 다중 통화 지원
- ✅ D1-05: 백오더(잔요청) 허용
- ✅ D1-06: 재고 추적 수준
- ✅ D1-07: PDF 문서 3종 포함
- ✅ D1-08: 샘플 데이터 — 실제 취급 품목 기반

### D2. 데이터 모델 결정 (12개) ✅

- ✅ D2-01: 재고 관리 수준
- ✅ D2-02: 다중 통화 지원
- ✅ D2-03: 거래처 체계(그룹 구분)
- ✅ D2-04: 단가 이력 저장
- ✅ D2-05: 부분 출하 스키마
- ✅ D2-06: 인보이스 원화 저장 여부
- ✅ D2-07: **거래처별 문서번호 채번** ⭐
- ✅ D2-08: PI 견적 단계 발행 가능
- ✅ D2-09: 선적 케이스/중량 필드
- ✅ D2-10: 임차 중량(Gross/Net) 구분
- ✅ D2-11: 댓글 계층 깊이 (단계/무제한)
- ✅ D2-12: 게시글 조회수 중복 방지 (쿠키)

### D3. 인증과 회원 결정 (10개) ✅

- ✅ D3-01: SMTP 이메일 인증 (MailHog 개발용)
- ✅ D3-02: JWT 만료 기간 (30분)
- ✅ D3-03: 임시비밀번호 발급
- ✅ D3-04: 아이디 저장 쿠키
- ✅ D3-05: 회원 역할 3종 (ADMIN/SALES/CUSTOMER)
- ✅ D3-06: 이중 인증 여부 (불포함)
- ✅ D3-07: 로그인 세션 정책
- ✅ D3-08: CORS 설정
- ✅ D3-09: OAuth 연동 (불포함)
- ✅ D3-10: 회원 탈퇴 시 데이터 처리 (소프트 삭제)

### D4. 게시판 정책 결정 (10개) ✅

- ✅ D4-01: 게시판 3종 명칭
- ✅ D4-02: 비회원 글쓰기 (자유 게시판만)
- ✅ D4-03: 비밀번호 관리
- ✅ D4-04: 파일 첨부 정책
- ✅ D4-05: 파일 용량 제한
- ✅ D4-06: 좋아요 기능
- ✅ D4-07: 조회수 중복 방지
- ✅ D4-08: 검색 범위 (제목/내용/작성자)
- ✅ D4-09: 페이지 크기 (기본 10개)
- ✅ D4-10: 페이지 네비게이션 UI (MUI Pagination)

### D5. 수주 흐름 규칙 결정 (14개) 🧭 ✅

- ✅ D5-01: 견적 유효기한 (거래처별 기본값 설정 가능)
- ✅ D5-02: 견적 없이 바로 수주 가능 (재주문)
- ✅ D5-03: **견적 개정 이력 관리** (Rev 체인) ⭐
- ✅ D5-04: **재고 할당 시점** (수주 확정 시)
- ✅ D5-05: **백오더 허용** (음수 재고 가능)
- ✅ D5-06: 출하 부분 삭제 가능 여부
- ✅ D5-07: 수주 취소 시점 (인보이스 발행 전까지)
- ✅ D5-08: **재고 차감 시점** (출하 SHIPPED 전환 시) ⭐
- ✅ D5-09: **PI 견적 단계 발행 가능**
- ✅ D5-10: **CI 출하 1건 = 1장** (1:1)
- ✅ D5-11: **결제기한 기준일** (B/L date)
- ✅ D5-12: 환율 적용 시점
- ✅ D5-13: 수주 상태 자동 판정
- ✅ D5-14: **PI/CI 환율 기준일** (PI: 발행일, CI: 선적일)

### D6. 기술 선택 결정 (9개) ⚠️

- ✅ D6-01: **Gradle** (Maven 대신)
- ✅ D6-02: JPA Cascade 정책
- ✅ D6-03: **WAR 통합 배포** (분리 아님)
- ✅ D6-04: Vite + React
- ✅ D6-05: 상태 관리 (Zustand / Context API)
- ✅ D6-06: HTTP 클라이언트 (Axios)
- ✅ D6-07: ⏳ PDF 라이브러리 선택 대기
- ✅ D6-08: Excel 라이브러리 (Apache POI)
- ✅ D6-09: 테스트 (JUnit 5, TestContainers)

### D7. 화면과 UX 결정 (8개) ✅

- ✅ D7-01: **좌측 사이드바** 네비게이션
- ✅ D7-02: 상태 배지 스타일 (색상 코드화)
- ✅ D7-03: 제품 검색 방식 (자동완성)
- ✅ D7-04: **제품 다중선택 모달**
- ✅ D7-05: 상태별 버튼 가시화
- ✅ D7-06: 화면 레이아웃 (헤더/사이드바/콘텐츠)
- ✅ D7-07: 다크 모드 지원
- ✅ D7-08: 모바일 반응형 (필수 아님)

### D8. 문서 출력 양식 결정 (7개) 🧭 ✅

- ✅ D8-01: **PI/CI/PL 영문 양식** (국제 표준)
- ✅ D8-02: **Ocean/Air 케이스별 다른 필드**
- ✅ D8-03: 로고/워터마크 삽입
- ✅ D8-04: 한글 폰트 포함 (NotoSans)
- ✅ D8-05: QR코드 인쇄 (선택)
- ✅ D8-06: 영문 주소 자동 생성
- ✅ D8-07: Incoterms별 책임 조항 표기

---

## 체크리스트

### 지금 당장 해야 할 것 (스프린트 0)

- [ ] Docker Compose (Oracle + MailHog) 구성
- [ ] Oracle DDL 전체 실행 (테이블 20개)
- [ ] 샘플 데이터 INSERT (거래처/제품/견적/수주/출하/인보이스)
- [ ] 통계 쿼리(PIVOT) 실제 실행 및 검증
- [ ] React 화면 5장 목업 (로그인, 견적, 수주, 출하, 대시보드)

### M0 시작 전 확인

- [ ] 프로젝트 근본 원칙 3가지 이해
- [ ] 78개 결정사항 검토
- [ ] 마일스톤 일정 확인
- [ ] 팀 구성원과 역할 분담 (1명인 경우 생략)

### 각 마일스톤별 시작 전

- [ ] 해당 마일스톤의 필수 설계 문서 읽기
- [ ] API 명세 확인
- [ ] 화면 설계 검토
- [ ] 동시성 / 상태 전이 이슈 파악
- [ ] DoD (Definition of Done) 확인

---

## 중요 학습 포인트

### 이 프로젝트에서 배우는 것

1. **상태 머신 + 트랜잭션**
   - 견적 → 수주 전환이 원자성을 만족해야 하는 이유
   - enum으로 상태 전이 규칙을 한 곳에 모으기

2. **동시성 제어**
   - 비관적 락 vs 낙관적 락의 트레이드오프
   - `SELECT FOR UPDATE` 사용과 데드락 회피
   - 테스트로 입증하기 (`CountDownLatch`)

3. **트랜잭션 경계**
   - 서비스 메서드 = 1 트랜잭션
   - private 메서드에 @Transactional은 안 된다

4. **스냅샷 패턴**
   - 견적 → 수주 복사하는 이유
   - 과거 데이터의 불변성

5. **N:M + 시간 차원 모델링**
   - PRICE_HISTORY로 Customer × Product × Period 표현

6. **다형 참조**
   - ATTACHMENT의 ref_type + ref_id 패턴

7. **배치 처리**
   - @Scheduled로 daily job 구성
   - 타임존 명시의 중요성

8. **환율 API 연동**
   - RestTemplate / WebClient 사용
   - 재시도 + 폴백 로직

9. **문서 생성**
   - Excel (POI) 대량 등록
   - PDF (iText) 다국어 처리

10. **통계 쿼리**
    - Oracle 윈도우 함수 (LAG, RANK)
    - PIVOT으로 행 → 열

---

## 참고 자료

### 프로젝트 구조

```
TreaderAPP/
├── 10-기획/             # 프로젝트 개요, 요구사항
├── 20-설계/             # ERD, API, 화면 설계
├── 30-개발환경/         # 세팅 가이드, 외부 연동
├── 40-학습/             # 학습 포인트, 마일스톤
├── 50-로그/             # ADR, 작업 로그 템플릿
├── 60-결정/             # 78개 결정사항 (D1~D8)
└── DEVELOPMENT_GUIDE.md # 이 파일
```

### 주요 결정 문서

- `60-결정/60-결정 대시보드.md` — 78개 항목 전체 목록
- `60-결정/69-충돌 점검 리포트.md` — 설계 적용 결과 검증
- `42-마일스톤.md` — 상세 개발 순서

### 기술 설계 문서

- `21-데이터 모델(ERD).md` — 테이블 관계도 (20개)
- `22-Oracle DDL 초안.md` — 실행 가능한 DDL
- `23-상태 전이와 동시성.md` — 트랜잭션 설계
- `24-API 명세.md` — REST 엔드포인트
- `25-화면 설계.md` — React 라우팅 + 컴포넌트
- `26-패키지 구조.md` — 8계층 레이어 구조
- `27-회원과 인증 설계.md` — JWT/이메일 인증
- `28-게시판 설계.md` — 3종 통합 모델
- `29-통계 쿼리(윈도우함수·PIVOT).md` — 분석 SQL

---

## 자주 묻는 질문 (FAQ)

### Q. 왜 "설계 발표용 목업"을 먼저 만드나?

A. 실무에서 "돌아가는 코드"의 진정한 가치를 보여주기 위해서다.
화면은 목업이어도 **DB 스키마는 진짜**고, **PIVOT 쿼리도 진짜**다.
면접관에게 이것이 무엇을 의미하는지 설명할 때, 아이디어보다 구체성이 훨씬 강하다.

### Q. 스냅샷 복사를 왜 그렇게 강조하나?

A. 회계에서 가장 중요한 원칙이 "과거 기록의 불변성"이기 때문이다.
견적서를 나중에 수정해도 수주는 변하면 안 된다.
이것이 단순한 OO 설계가 아니라 **업무 도메인을 이해한 설계**임을 보여준다.
면접에서 정확히 이 점을 물어본다.

### Q. 재고 차감 시점을 SHIPPED로 미루는 이유?

A. 출하를 등록한 후에도 삭제할 수 있어야 하기 때문이다.
PLANNED 상태에서 삭제하면 재고를 되돌려야 하는데, 그 사이 다른 출하가 끼어들면 정합성이 깨진다.
**문제를 근본부터 해결하는 설계**. 단순 구현이 아니라 **트레이드오프를 고민한 설계**.

### Q. 78개 결정을 다 ADR로 남겨야 하나?

A. 아니다. **10개**만 제대로 남기면 충분하다.
그 10개는 나중에 설명하거나 면접에서 물어볼 만한 것들:
D5-03 (개정 이력), D5-04 (재고 할당), D5-08 (차감 시점), D5-14 (환율 기준일),
D2-07 (거래처별 채번), D3-10 (탈퇴 처리) 등.

### Q. 시간이 부족하면 뭘 먼저 자른다?

A. 순서대로:
1. M7의 WebSocket 알림 (메일로 충분)
2. M5의 PL PDF (CI만 해도 됨)
3. M6의 일부 차트
4. **M4는 절대 자르지 않는다.** ← 이 프로젝트의 핵심이다.

---

**Last Updated: 2026-09-07**
**Status: 스프린트 0 대기 중**
**Next Step: Docker Compose 실행 → DDL 실행 → 샘플 데이터 입력**
