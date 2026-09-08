# 수출 오더 관리 시스템 (TreaderAPP) — 통합 개발 가이드

> **한 줄 정의**
> 견적(Quotation) → 수주(Sales Order) → 출하(Shipment) → 인보이스(Invoice)로 이어지는
> 수출 영업 실무 한 사이클을 Spring Boot + Oracle + React로 구현하는 포트폴리오 프로젝트.

**최종 수정**: 2026-09-08 | **상태**: M0 뼈대 · M1 인증 · M4 수주흐름 구현 완료 (실 DB 연동 검증 완료)

---

## 📋 목차

1. [현재 진행 상황](#현재-진행-상황)
2. [프로젝트 개요](#프로젝트-개요)
3. [핵심 원칙 3가지](#핵심-원칙-3가지)
4. [기술 스택](#기술-스택)
5. [실행 방법](#실행-방법)
6. [개발 순서 (마일스톤)](#개발-순서-마일스톤)
7. [구현된 API 목록](#구현된-api-목록)
8. [업무 흐름 상세](#업무-흐름-상세)
9. [데이터 모델](#데이터-모델)
10. [상태 전이와 동시성](#상태-전이와-동시성)
11. [핵심 결정사항 (78개)](#핵심-결정사항-78개)

---

## 현재 진행 상황

> 이 절은 **실제로 돌아가는 것만** 적는다. 계획은 아래 마일스톤 절에 있다.

| 마일스톤 | 상태 | 비고 |
|---|---|---|
| 스프린트 0 (설계 발표용 목업) | ✅ 완료 | 목업은 실 API로 전부 교체됨 |
| **M0 뼈대** | ✅ 완료 | Gradle·WAR·전역 예외·Swagger·Docker |
| **M1 인증** | ✅ 완료 | 가입·이메일인증·로그인·정보수정·비밀번호찾기·JWT |
| **M2 게시판** | ✅ 완료 | 자유/Q&A/자료실 + 댓글·좋아요·조회수·페이징·검색·첨부 |
| **M3 마스터 CRUD** | ✅ 완료 | 거래처(담당자 1:N)·제품·단가이력·재고 CRUD + 페이징·검색 |
| **M4 수주 흐름** | ✅ 완료 | 견적→수주→출하→인보이스→입금 전 과정 + 동시성 테스트 |
| **M5 파일/문서** | ✅ 완료 | Excel 대량등록·양식·내보내기 + PDF 3종 + 업무문서 첨부(다형 참조) |
| M6 통계 | 🔶 간이 | 대시보드는 목록 API를 프론트에서 집계. 네이티브 통계 쿼리 미구현 |
| M7 환율/알림 | ⏳ 미착수 | EXCHANGE_RATE 조회는 붙어 있으나 수집 배치 없음 |
| M8 마무리 | ⏳ 미착수 | |

### 검증 현황 (2026-09-08)

- **업무 CRUD 통합 검증 43건 전부 통과** — 인증·마스터·견적·PI·수주·출하·인보이스·인가·탈퇴
- **게시판 CRUD 통합 검증 47건 전부 통과** — 게시판 권한·비회원글·조회수·좋아요·댓글·페이징·검색·첨부
- **마스터 CRUD 통합 검증 45건 전부 통과** — 거래처·담당자·제품·재고·단가 기간겹침·삭제정책
- **파일/문서 통합 검증 24건 전부 통과** — 엑셀 100행 대량등록·양식·내보내기·PDF 3종·다형 첨부
- **동시성 테스트 2건 통과** — 실제 Oracle 대상, 락을 떼면 깨지는 것까지 확인

> 통합 검증 합계 **159건**. 스크립트는 `scratchpad/{crud,board,master,excel}-check.ps1` 에 있다.

**실행하며 발견해 고친 버그**

| 버그 | 증상 |
|---|---|
| DB 포트 오설정 (1521→1523) | Docker 가 아닌 로컬 Oracle 로 붙거나 연결 실패 |
| CI 가 PI 발행을 막음 | 중복 검사에 `invoiceType` 필터 누락 |
| `SHIPPED_QTY` 갱신 손실 | 출하 확정이 수주 행을 잠그지 않아 동시 확정 시 장부가 조용히 어긋남 |
| 감사 컬럼 길이 초과 (ORA-12899) | `AuditorAware` 가 record 의 `toString()`(56자)을 `VARCHAR2(50)` 에 넣음 |
| DDL 주석의 admin 비밀번호 오기 | `1234` 라고 적혀 있으나 실제 해시는 `password` |
| 클라이언트 오류가 500 으로 나감 | 깨진 JSON·Content-Type 불일치·파트 누락이 전부 `handleException` 으로 떨어짐 |
| 원시 `boolean` 요청 필드가 본문 파싱을 깸 | Jackson 3 은 `FAIL_ON_NULL_FOR_PRIMITIVES` 기본 ON — 값이 빠지면 400 |
| 제품 삭제 시 FK 위반 | `findSeries(id, null)` 이 표준 단가만 줘서 거래처 전용 단가가 남았다 |
| 내가 만든 양식을 내 리더가 못 읽음 | 양식 1행이 안내문인데 리더는 첫 행을 헤더로 단정했다 |
| 엑셀 아닌 파일이 500 으로 나감 | POI 의 `NotOfficeXmlFileException` 은 `IOException` 이 아니라 안 잡혔다 |

### 아직 안 된 것 (솔직하게)

- Oracle 윈도우함수·PIVOT 통계 쿼리 — M6 (대시보드는 프론트 집계로 대체)
- 환율 수집 배치, 알림 — M7
- 리프레시 토큰 / 토큰 블랙리스트 — 로그아웃은 클라이언트 토큰 폐기로만 동작한다
- 게시글 낙관적 락 — 설계 노트는 원하지만 `POST` 에 `VERSION` 컬럼이 없다 (스키마가 원본이라 보류)

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

> 실제 설치된 버전이다. 계획이 아니라 `build.gradle` / `package.json` 에서 확인한 값이다.

### Backend
- **JDK 21** + **Spring Boot 4.1.1** (Hibernate 7.4, Jackson 3)
- **Oracle Database 21c XE** (테이블 21개, 시퀀스 22개, 인덱스 13개)
- **Spring Data JPA**
- **Spring Security 7** + **JJWT 0.12.6** (HS256)
- **openhtmltopdf 1.0.10** (인보이스 PDF)
- **Jakarta Mail** (MailHog 발송)
- **Gradle 9.7** / **Tomcat 11** (WAR 배포)

> ⚠️ **Boot 4 주의점 3가지 (실제로 겪은 것)**
> 1. Jackson 이 3.x(`tools.jackson`)다. `com.fasterxml.jackson.databind.ObjectMapper` 빈은 없다.
> 2. 스타터가 잘게 쪼개졌다. `spring-boot-starter-web` 대신 `spring-boot-starter-webmvc` 다.
> 3. **요청 DTO 에 원시 `boolean` 을 쓰면 안 된다.** Jackson 3 은
>    `FAIL_ON_NULL_FOR_PRIMITIVES` 가 기본 켜져 있어, JSON 에서 그 필드가 빠지면
>    `Cannot map null into type boolean` 으로 **본문 전체가 파싱 실패**한다 (Jackson 2 는 false 였다).
>    `Boolean` 래퍼로 받고 `Boolean.TRUE.equals(...)` 로 읽는다.

### Frontend
- **React 19** (Vite 8)
- **MUI 9** + **@mui/icons-material 9**
- **Recharts 3**
- **Axios** (인터셉터로 JWT 자동 첨부 + 401 처리)
- **React Router 7**
- **Context API** (`AuthContext`)

> ⚠️ **MUI 9 주의점 3가지 (실제로 겪은 것)**
> 1. Grid 문법이 바뀌었다. `<Grid item xs={12}>` → `<Grid size={{ xs: 12 }}>`
> 2. `<ListItem button>` 제거 → `<ListItemButton>`
> 3. Typography/Stack 의 `display`·`alignItems`·`justifyContent` prop 제거 → `sx` 로 넣어야 한다
> 4. 아이콘 이름이 Lucide 와 다르다. `ErrorOutline`(X) → `ErrorOutlined`(O), `Truck`(X) → `LocalShipping`(O)

### DevOps
- **Docker Compose** (Oracle XE 1523, MailHog 1025/8025)
- **Swagger** (`/swagger-ui.html`)

---

## 실행 방법

### 1. 인프라 기동

```bash
cd C:\02Workspaces\12SpringBoot\treader
docker compose up -d
```

| 서비스 | 주소 | 계정 |
|---|---|---|
| Oracle XE | `localhost:1523` / `XEPDB1` | `boot_user` / `1234` |
| MailHog SMTP | `localhost:1025` | — |
| MailHog 웹 UI | http://localhost:8025 | 인증 메일을 여기서 확인 |

> ⚠️ 포트가 **1523**이다. 로컬에 이미 Oracle 이 1521 을 쓰고 있어 피한 것이다.

### 2. DB 초기화 (최초 1회)

`oracle_ddl.sql` → `sample-data.sql` 순서로 실행한다.

### 3. 백엔드

```bash
.\gradlew.bat bootRun
```

http://localhost:8080 · 헬스체크 `/api/health` · API 문서 `/swagger-ui.html`

### 4. 프론트엔드

```bash
cd frontend-react && npm install && npm run dev
```

http://localhost:5173 (Vite 가 `/api` 를 8080 으로 프록시한다)

> 💡 아이콘 import 를 추가한 뒤 화면이 안 뜨고 `504 Outdated Optimize Dep` 이 뜨면
> Vite 의 의존성 사전번들 캐시가 낡은 것이다. `rm -rf node_modules/.vite` 후 재시작하면 된다.

### 5. 테스트

```bash
.\gradlew.bat test
```

동시성 테스트는 **실행 중인 Oracle 에 직접 붙는다.** Docker 가 떠 있어야 한다.

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

### 🔑 M1. 인증 — ✅ 완료

**Backend**:
- [x] `Member`, `EmailToken` 엔티티 + 리포지토리
- [x] 회원가입 + **아이디/이메일 중복확인** API
- [x] **이메일 인증** (UUID 토큰 24시간, 일회용, MailHog 발송)
- [x] JWT 발급/검증 필터, `SecurityConfig` (`/api/**` 인증 필수)
- [x] 로그인 / 로그아웃
- [x] 비밀번호 찾기 → 임시비밀번호 메일 발송
- [x] 회원정보 수정 / 비밀번호 변경
- [x] 회원 탈퇴 (소프트 삭제)
- [x] 관리자 회원 목록 (`@PreAuthorize("hasRole('ADMIN')")`)

**Frontend**:
- [x] 로그인 · 가입 · 이메일인증 · 비번찾기 · 내정보 화면
- [x] `AuthContext` + `RequireAuth` 라우트 가드
- [x] **아이디 저장** (localStorage, 비밀번호는 저장하지 않음)
- [x] axios 인터셉터 — 토큰 자동 첨부, 401 시 로그인 화면으로

#### 가입 항목

| 항목 | 필수 | 검증 |
|---|---|---|
| 아이디 | ✅ | 영문·숫자·밑줄 4~20자, **중복확인 필수** |
| 비밀번호 | ✅ | 영문+숫자 포함 8자 이상 |
| 비밀번호 확인 | ✅ | 입력 즉시 불일치 표시 + 서버 재검증 |
| 이름 | ✅ | 50자 이내 |
| 이메일 | ✅ | 형식 검증, **중복확인 필수**, 인증메일 수신처 |
| 전화번호 | ✅ | 숫자·하이픈·괄호 7~30자 |
| 부서 / 직급 | 선택 | 추가 항목 (`DEPT`, `POSITION`) |

#### 상태 흐름

```
가입 → PENDING (로그인 불가)
   ↓ 메일의 인증 링크 클릭
인증 완료 → ACTIVE, EMAIL_VERIFIED_YN='Y' → 로그인 가능
   ↓ 이메일 변경 시
인증 해제 → 새 주소로 인증메일 재발송, 다시 인증할 때까지 로그인 불가
```

**설계 근거**
- 중복확인은 **버튼과 저장 시점 양쪽에서** 본다. 버튼을 누른 뒤 저장 전까지 남이 채갈 수 있다.
- 로그인 실패 메시지는 아이디 없음과 비밀번호 틀림을 **구분하지 않는다.** 계정 존재 여부가 새면 안 된다.
- 메일 발송은 `@Async` 다. SMTP 가 느리다고 가입 트랜잭션이 같이 실패하면 안 된다.
- 이메일을 바꾸면 인증이 풀린다. 확인되지 않은 주소로 납기·연체 알림이 나가면 안 되기 때문이다.

**로그아웃의 한계 (솔직하게)**
JWT 가 무상태라 서버가 회수할 것이 없다. 실제 무효화는 클라이언트가 토큰을 버리는 것으로 끝난다.
탈취된 토큰을 즉시 차단하려면 블랙리스트 저장소가 필요한데, 이 프로젝트는 **짧은 만료(30분)로 갈음**했다.
면접에서 물으면 이렇게 답하면 된다 — "리프레시 토큰과 블랙리스트를 넣지 않은 건 범위 판단이었고,
그 대가로 로그아웃이 즉시성을 잃는다는 걸 알고 선택했다."

**DoD**: ✅ 가입 → 메일 인증 → 로그인 → 정보수정 → 비밀번호 변경 → 탈퇴까지
브라우저와 API 양쪽에서 검증 완료.

---

### 📝 M2. 게시판 — ✅ 완료

**Backend**:
- [x] `Board`, `Post`, `PostComment`, `PostFile`, `PostLike` 엔티티
- [x] 게시판 공통 CRUD — 컨트롤러·서비스 **한 벌**로 세 게시판을 처리
- [x] 자유게시판: **비회원 쓰기 + 비밀번호로 수정/삭제**
- [x] Q&A: 댓글·대댓글 CRUD, 답변완료 표시
- [x] 자료실: 파일 업로드/다운로드, `mediaType` 판정
- [x] **조회수 쿠키** (서버 판정, 자정 만료)
- [x] 좋아요 토글
- [x] 서버 페이징 + 검색(제목/내용/작성자)
- [x] 소프트 삭제

**Frontend**:
- [x] 게시판 목록(탭·검색·페이징) / 상세 / 작성·수정 화면
- [x] 이미지·영상·오디오 인라인 미리보기
- [x] 좋아요 버튼, 댓글/대댓글 영역
- [x] 파일 업로드 폼

#### 게시판 3종 — 설정이 곧 동작이다

게시판을 **테이블로** 두었다. 자유·Q&A·자료실을 각각 테이블로 만들면 CRUD 코드를 세 벌 쓰게 된다.
권한·첨부·댓글·좋아요 허용 여부를 전부 `BOARD` 행이 결정하므로,
**"공지사항 하나 더"가 INSERT 한 줄**이 된다.

| 코드 | 이름 | 읽기 | 쓰기 | 첨부 | 댓글 | 좋아요 |
|---|---|---|---|---|---|---|
| `FREE` | 공지·문의 | ALL | **ALL (비회원)** | ✕ | ✕ | ○ |
| `QNA` | 거래처 Q&A | MEMBER | MEMBER | ✕ | **○** | ○ |
| `ARCHIVE` | 자료실 | MEMBER | MEMBER | **○** | ○ | ○ |

검증에서 실제로 확인한 것 — FREE 에 댓글을 달면 400, FREE 에 파일을 올리면 400,
비로그인으로 QNA 를 열면 401. 코드에 `if (boardCode.equals("FREE"))` 같은 분기는 한 줄도 없다.

#### 설계 판단 4가지

**1. 조회수는 서버가 판단한다**
클라이언트가 "이번엔 세 주세요"를 보내게 하면 새로고침으로 얼마든지 부풀릴 수 있다.
쿠키(`treader_viewed`)에 읽은 글 번호를 누적하고 **자정에 만료**시켜 하루 한 번만 센다.
`"_" + 값 + "_"` 로 감싸 비교해야 1 과 12 가 섞이지 않는다.

**2. 파생 카운트는 증감하지 않고 다시 센다**
`LIKE_CNT`·`COMMENT_CNT` 는 목록 성능을 위한 비정규화 컬럼이고,
진실의 원천은 `POST_LIKE`·`POST_COMMENT` 다. `+1/-1` 로 처리하면 어딘가에서 한 번 빠뜨렸을 때
영영 어긋난다. 같은 트랜잭션에서 `COUNT(*)` 를 다시 넣는다.

**3. 삭제는 전부 소프트 삭제**
댓글이 달린 글을 물리 삭제하면 참조가 깨진다. 대댓글이 달린 댓글도 마찬가지라
"삭제된 댓글입니다"로 자리를 지킨다.

**4. 좋아요 중복은 애플리케이션이 아니라 PK 가 막는다**
`POST_LIKE` 가 (글, 회원) 복합키다. 코드에서 검사하는 것보다 제약이 확실하다.

#### 겪은 문제

**`lower()` 를 CLOB 에 못 쓴다**
본문 검색을 대소문자 무시로 하려다 Hibernate 가 거부했다 —
`Parameter 1 of function 'lower()' has type 'STRING', but argument is of type ... mapped to 'CLOB'`.
검색 조건을 파라미터 하나로 분기하는 큰 HQL 을 쓰려던 것도 같이 걷어내고
조건별 메서드로 나눴다. **본문 검색만 대소문자를 구분**한다 —
한글에는 대소문자가 없어 실사용에서는 거의 문제가 되지 않는다.

**DoD**: ✅ 게시판 CRUD 검증 47건 통과. 비회원 글 작성→수정(비밀번호 확인)까지
브라우저에서 직접 확인.

---

### 🏪 M3. 마스터 CRUD — ✅ 완료

**Backend**:
- [x] `Customer` + `CustomerContact` (1:N) — 대표 연락처 관리 포함
- [x] `Product` CRUD — 등록 시 재고 행 자동 생성
- [x] `PriceHistory` — **기간 겹침 처리**, 적용 단가 조회 API
- [x] `Inventory` 조회 + **실사 조정**
- [x] 검색·페이징 (`PageResponse` 재사용)
- [x] 삭제 정책 — 거래 이력이 있으면 비활성

**Frontend**:
- [x] `/master` 한 화면에 4개 탭 (거래처 · 제품 · 단가 · 재고)
- [x] 거래처 등록/수정 — 담당자 여러 명, 별표로 대표 지정
- [x] 제품 등록/수정 — 초기 재고 입력
- [x] 단가 등록/수정 — 기간 겹침 시 자동 마감 옵션
- [x] 재고 실사 조정 — 차이 자동 표시

#### 단가 기간 겹침 — 이 단계에서 가장 어려운 부분

`PRICE_HISTORY` 는 (거래처 × 제품 × **기간**) 이라, 기간이 겹치면
"이 날짜에 어떤 단가를 쓸지"가 모호해진다. 그런데 **범위 겹침은 UNIQUE 제약으로 표현할 수 없다.**
DB 가 막아 주지 못하므로 서비스가 책임진다.

두 갈래로 처리한다:

| 상황 | 처리 |
|---|---|
| `closePrevious=true` | 겹치는 이전 단가를 새 시작일 **하루 전**으로 닫는다. 가격 인상처럼 "오늘부터 새 단가"인 실무 상황 |
| `closePrevious=false` | 오류로 돌려준다. 과거 구간을 소급 입력하다 기존 이력을 조용히 덮는 사고를 막는다 |
| 새 단가보다 **뒤에** 시작하는 단가가 있음 | 거절. 미래 단가를 뒤로 밀 수는 없다 |

검증에서 확인한 동작 — 2026-01-01 시작 단가 3.50 이 있는 상태에서 2026-06-01 부터 3.90 을 넣으면
이전 단가가 `validTo = 2026-05-31` 로 닫힌다. 그 뒤 3월 조회는 3.50, 8월 조회는 3.90 이 나온다.
**한 줄에서 동시에 유효한 단가는 항상 1건**이다.

#### 삭제 정책 — 지우지 않는 것이 기본

마스터를 물리 삭제하면 과거 문서가 깨진다. 그래서 **흔적이 전혀 없을 때만** 지운다.

| 대상 | 비활성 처리하는 조건 |
|---|---|
| 거래처 | 견적·수주가 있음 / 연결된 회원 계정이 있음 |
| 제품 | 견적 라인에 쓰임 / **재고 변동 이력이 있음** |

재고 변동 이력을 삭제 차단 사유에 넣은 이유는 그게 **감사 추적**이기 때문이다.
제품을 지우자고 함께 지울 성격이 아니다. 반면 재고 행과 단가는 제품에 딸린 것이라 함께 지운다.

#### 그 외 결정

- **코드는 등록 후 변경 불가.** `CUSTOMER_CODE` 는 문서번호(`QT-ABC-2026-0001`)에 박혀 나가므로
  바꾸면 과거 문서와 어긋난다 (D2-07). `PRODUCT_CODE` 도 같은 이유다. 수정 DTO 에 아예 필드가 없다.
- **제품 등록 시 재고 행을 함께 만든다.** 없으면 출하 확정에서 "재고 정보가 없습니다"로 터진다.
- **재고 조정은 차이가 아니라 센 값을 받는다.** 차이를 입력받으면 부호를 헷갈려 반대로 넣는 사고가 난다.
- **선금율 0% 거래처는 PI 를 낼 수 없다** — 마스터에서 0 을 넣으면 견적 화면의 PI 버튼이 사라진다.

**DoD**: ✅ 마스터 CRUD 검증 45건 통과. 브라우저에서 거래처 등록(담당자 포함),
단가 기간 겹침 경고와 자동 마감까지 직접 확인.

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

### 📄 M5. 파일/문서 — ✅ 완료

**Backend**:
- [x] `Attachment` 엔티티 — **다형 참조** (`refType` + `refId`)
- [x] 파일 업로드/다운로드 (로컬 스토리지, 날짜별 폴더)
- [x] 제품·단가 Excel 대량등록 (POI) — **부분 실패 리포트**
- [x] Excel 업로드 양식 다운로드
- [x] 목록 Excel 내보내기 (제품·단가)
- [x] 인보이스 PDF (PI/CI 서로 다른 양식)
- [x] **견적서 PDF**
- [x] **Packing List PDF**

**Frontend**:
- [x] `ExcelBar` — 양식 다운로드 · 대량등록 · 목록 내보내기 (제품·단가 탭 공유)
- [x] 업로드 결과 다이얼로그 — 실패 행을 사유와 함께, 오류 목록 엑셀로 재다운로드
- [x] `AttachmentPanel` — 견적·수주에서 `refType` 만 바꿔 재사용
- [x] 문서별 PDF 버튼

#### 부분 실패를 인정한다 — 이 단계의 핵심 결정

100건 중 3건이 틀렸다고 97건을 되돌리면 사용자는 **파일을 통째로 고쳐 처음부터 다시** 올려야 한다.
그래서 성공한 건 넣고, 실패한 건 **엑셀 화면의 행 번호와 사유**를 돌려준다.

이걸 가능하게 하는 건 `REQUIRES_NEW` 다:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveProduct(ExcelReader.RowData row, String code) { ... }
```

한 트랜잭션에 묶으면 중간에 한 행이 터졌을 때 앞서 넣은 행까지 전부 롤백된다.
행마다 커밋해야 부분 성공이 된다.

> ⚠️ **별도 빈으로 뺀 이유**: `REQUIRES_NEW` 는 프록시를 거쳐야 동작한다.
> 같은 클래스에서 `this.saveRow()` 로 부르면 AOP 를 타지 않아 바깥 트랜잭션에 그대로 합류한다.
> 자기 주입으로도 풀리지만 순환 참조를 만들므로 `MasterExcelRowWriter` 로 책임을 나눴다.

실측 결과 (100행: 정상 97 + 오류 3):

```
행 100  BULK-098   영문품명 은(는) 필수입니다.
행 101  BULK-099   MOQ 은(는) 숫자여야 합니다: abc
행 102  BULK-001   파일 안에 같은 제품코드가 중복됩니다.
→ 성공 97건은 DB 에 남고, 재업로드하면 97건이 "건너뜀" 처리된다
```

#### 엑셀 처리에서 겪은 것

**셀 타입이 제각각이다.** 사용자가 수량을 텍스트로 넣거나, 숫자 셀이 `1.0` 으로 읽혀
제품코드가 `"1.0"` 이 되는 일이 늘 생긴다. `ExcelReader` 가 타입을 흡수하고
변환 실패는 **행 단위 오류**로 넘긴다.

**내가 만든 양식을 내 리더가 못 읽었다.** 양식 1행에 안내문을 넣었는데
리더는 첫 행을 헤더로 단정했다. 헤더를 **위치가 아니라 내용으로** 찾도록 고쳤다 —
필수 열이 모두 있는 행을 위에서부터 10행까지 훑는다. 안내문이든 빈 줄이든 견딘다.

**POI 예외가 500 으로 새어 나갔다.** xlsx 가 아닌 파일에 `NotOfficeXmlFileException` 이
뜨는데 이건 `IOException` 이 아니라 `RuntimeException` 계열이라 catch 에 안 걸렸다.
잘못된 파일을 올린 건 클라이언트 잘못이므로 400 이어야 한다.

#### PDF 3종

| 문서 | 내용 | 특징 |
|---|---|---|
| 견적서 (Quotation) | 품목·단가·금액·유효기한 | 유효기한 경고 문구 |
| 인보이스 (PI/CI) | 청구액·결제기한·환율 | PI 는 선금 안내, CI 는 선적 정보 |
| **Packing List** | 수량·박스·중량·CBM | **금액이 없다** |

PL 에 금액을 넣지 않는 게 핵심이다. 인보이스가 **돈**을 말한다면 PL 은 **물건**을 말한다.
통관과 창고 검수에서 쓰는 서류라 금액이 들어가면 오히려 문제가 된다.

세 문서는 `PdfRenderer` 의 CSS 를 공유한다. 출력물은 D8-01 대로 영문이라 한글 폰트 임베드가 필요 없다.

#### 다형 참조의 대가

`ATTACHMENT` 는 `refType`(QUOTATION/SALES_ORDER/SHIPMENT/INVOICE) + `refId` 로
네 종류 문서를 한 테이블이 받는다. 테이블 네 개보다 낫지만 **FK 를 걸 수 없다.**

그래서 저장 전에 참조 대상이 실제로 있는지 확인한다. 이 확인이 FK 를 대신한다:

```java
boolean exists = switch (type) {
    case QUOTATION -> quotationRepository.existsById(refId);
    case SALES_ORDER -> salesOrderRepository.existsById(refId);
    ...
};
```

검증에서 없는 수주에 첨부를 시도하면 404 가 나온다. 이 확인을 빼면
아무도 찾지 못하는 고아 파일이 쌓인다.

**DoD**: ✅ 제품 100건 업로드 — 97건 성공, 3건은 행 번호와 사유로 리포트.
PDF 3종 모두 정상 출력. 브라우저에서 대량등록·첨부까지 직접 확인.

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

## 구현된 API 목록

> 실제로 붙어 있는 엔드포인트만 적는다. Swagger(`/swagger-ui.html`)에서도 볼 수 있다.
> `🔓` 는 인증 없이 호출 가능, 나머지는 `Authorization: Bearer <token>` 이 필요하다.

### 인증 · 회원

| Method | Path | 설명 |
|---|---|---|
| `GET` 🔓 | `/api/auth/check-login-id?loginId=` | 아이디 중복확인 |
| `GET` 🔓 | `/api/auth/check-email?email=` | 이메일 중복확인 |
| `POST` 🔓 | `/api/auth/signup` | 회원가입 (인증메일 발송) |
| `POST` 🔓 | `/api/auth/verify-email?token=` | 이메일 인증 |
| `POST` 🔓 | `/api/auth/resend-verification?email=` | 인증메일 재발송 |
| `POST` 🔓 | `/api/auth/login` | 로그인 (JWT 발급) |
| `POST` 🔓 | `/api/auth/logout` | 로그아웃 |
| `POST` 🔓 | `/api/auth/find-password` | 임시비밀번호 발송 |
| `GET` | `/api/members/me` | 내 정보 |
| `PUT` | `/api/members/me` | 회원정보 수정 |
| `PUT` | `/api/members/me/password` | 비밀번호 변경 |
| `POST` | `/api/members/me/withdraw` | 탈퇴 (소프트 삭제) |
| `GET` | `/api/members` | 회원 목록 — **ADMIN 전용** |

### 게시판

> 여기는 Spring Security 에서 일괄로 막지 않는다. 게시판별 권한을 `BoardAccessPolicy` 가 판단한다.

| Method | Path | 설명 |
|---|---|---|
| `GET` 🔓 | `/api/boards` | 게시판 목록 (읽을 수 있는 것만, 글쓰기 가능 여부 포함) |
| `GET` 🔓 | `/api/boards/{boardCode}/posts?page=&type=&keyword=` | 글 목록 · 페이징 · 검색 |
| `GET` 🔓 | `/api/boards/posts/{postId}` | 글 상세 (조회수 쿠키 처리) |
| `POST` 🔓 | `/api/boards/{boardCode}/posts` | 글 작성 (비회원은 이름·비밀번호 필요) |
| `PUT` 🔓 | `/api/boards/posts/{postId}` | 글 수정 |
| `DELETE` 🔓 | `/api/boards/posts/{postId}?guestPwd=` | 글 삭제 (소프트) |
| `POST` 🔓 | `/api/boards/posts/{postId}/verify-password` | 비회원 비밀번호 확인 |
| `POST` | `/api/boards/posts/{postId}/answered?value=` | Q&A 답변완료 표시 |
| `POST` | `/api/boards/posts/{postId}/comments` | 댓글·대댓글 작성 |
| `PUT` | `/api/boards/comments/{commentId}` | 댓글 수정 |
| `DELETE` | `/api/boards/comments/{commentId}` | 댓글 삭제 (소프트) |
| `POST` | `/api/boards/posts/{postId}/like` | 좋아요 토글 |
| `POST` | `/api/boards/posts/{postId}/files` | 파일 업로드 (multipart) |
| `GET` 🔓 | `/api/boards/files/{fileId}` | 파일 다운로드 (이미지·영상은 inline) |
| `DELETE` | `/api/boards/files/{fileId}` | 파일 삭제 |

### 마스터 — 거래처

> ⚠️ `/api/customers` 는 **페이지 응답**(`PageResponse`)이고, 드롭다운용 배열은 `/active` 다.
> 둘을 헷갈리면 화면이 빈 목록으로 뜬다.

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/customers?page=&size=&keyword=&status=` | 목록 · 페이징 · 검색 |
| `GET` | `/api/customers/active` | 활성 거래처 전체 (배열, 드롭다운용) |
| `GET` | `/api/customers/{id}` | 상세 (담당자·거래 건수 포함) |
| `POST` | `/api/customers` | 등록 (담당자 함께) |
| `PUT` | `/api/customers/{id}` | 수정 — **코드는 바꿀 수 없다** |
| `DELETE` | `/api/customers/{id}` | 삭제 또는 비활성 (처리 결과를 메시지로) |
| `POST` | `/api/customers/{id}/status?active=` | 활성/비활성 전환 |

### 마스터 — 제품 · 재고

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/products?page=&size=&keyword=&status=` | 목록 · 페이징 · 검색 |
| `GET` | `/api/products/active` | 활성 제품 전체 (배열, **재고 포함**) |
| `GET` | `/api/products/{id}` | 상세 |
| `POST` | `/api/products` | 등록 — **재고 행이 함께 생성된다** |
| `PUT` | `/api/products/{id}` | 수정 |
| `DELETE` | `/api/products/{id}` | 삭제 또는 비활성 |
| `POST` | `/api/products/{id}/status?active=` | 활성/비활성 전환 |
| `GET` | `/api/products/stocks?keyword=` | 재고 현황 (가용·안전재고 포함) |
| `POST` | `/api/products/{id}/stock-adjust` | **실사 조정** — 센 수량으로 덮는다 |

### 파일 · 문서 (M5)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/excel/products/template` | 제품 등록양식 xlsx |
| `GET` | `/api/excel/prices/template` | 단가 등록양식 xlsx |
| `POST` | `/api/excel/products/import` | 제품 대량등록 — **부분 성공 + 실패행 리포트** |
| `POST` | `/api/excel/prices/import` | 단가 대량등록 |
| `POST` | `/api/excel/errors/export` | 실패 행만 엑셀로 재다운로드 |
| `GET` | `/api/excel/products/export` | 제품 목록 xlsx (재고 포함) |
| `GET` | `/api/excel/prices/export` | 단가 목록 xlsx |
| `GET` | `/api/quotations/{id}/pdf` | **견적서 PDF** |
| `GET` | `/api/shipments/{id}/packing-list` | **Packing List PDF** (금액 없음) |
| `GET` | `/api/invoices/{id}/pdf` | 인보이스 PDF |
| `GET` | `/api/attachments?refType=&refId=` | 업무문서 첨부 목록 |
| `POST` | `/api/attachments?refType=&refId=&docType=` | 첨부 업로드 (multipart) |
| `GET` | `/api/attachments/{id}` | 첨부 다운로드 |
| `DELETE` | `/api/attachments/{id}` | 첨부 삭제 |

### 마스터 — 단가

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/prices?productId=&customerId=` | 단가 이력 |
| `GET` | `/api/prices/resolve?productId=&customerId=&baseDate=` | 특정 날짜의 적용 단가 |
| `POST` | `/api/prices` | 등록 — `closePrevious` 로 겹침 처리 방식 결정 |
| `PUT` | `/api/prices/{id}` | 수정 |
| `DELETE` | `/api/prices/{id}` | 삭제 |

### 견적

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/quotations` | 목록 |
| `GET` | `/api/quotations/{id}` | 상세 (품목·PI 발행가능 여부 포함) |
| `POST` | `/api/quotations` | 생성 (거래처 기본값 자동 적용) |
| `POST` | `/api/quotations/{id}/items` | 품목 추가 (단가 생략 시 자동 제안) |
| `DELETE` | `/api/quotations/{id}/items/{itemId}` | 품목 삭제 |
| `POST` | `/api/quotations/{id}/send` | 발송 (DRAFT → SENT) |
| `GET` | `/api/quotations/{id}/price-suggestion?productId=` | 적용 단가 조회 |
| `POST` | `/api/quotations/{id}/convert` | **수주 전환** |

### 수주

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/orders` | 목록 |
| `GET` | `/api/orders/{id}` | 상세 (품목별 진행률 + 출하 이력) |
| `GET` | `/api/orders/{id}/pending-items` | 미출하 잔량 (출하 등록 화면용) |

### 출하

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/shipments` | 목록 |
| `GET` | `/api/shipments/{id}` | 상세 |
| `POST` | `/api/shipments` | 등록 (PLANNED, **재고 변동 없음**) |
| `POST` | `/api/shipments/{id}/confirm` | **확정 (재고 차감)** — B/L 또는 AWB 필수 |
| `DELETE` | `/api/shipments/{id}` | 삭제 — **PLANNED 만 가능** |

### 인보이스

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/invoices` | 목록 (입금 내역 포함) |
| `GET` | `/api/invoices/{id}` | 상세 |
| `POST` | `/api/invoices/pi` | **선금 PI 발행** (견적 단계) |
| `POST` | `/api/invoices/ci` | **CI 발행** (확정 출하 1건당 1장) |
| `POST` | `/api/invoices/{id}/payments` | 입금 등록 (상태 자동 판정) |
| `GET` | `/api/invoices/{id}/pdf` | **PDF 다운로드** (영문 양식) |

### 업무 규칙이 API 에 드러나는 지점

| 규칙 | 어디서 막히나 |
|---|---|
| 발송 안 한 견적은 수주 전환 불가 | `convert` → 400 |
| 견적 1건은 수주 1건까지만 | `convert` 재호출 → 400 |
| DRAFT 견적에는 PI 발행 불가 | `invoices/pi` → 400 |
| 선금율 0% 거래처는 PI 불가 | `invoices/pi` → 400 |
| 출하 1건 = CI 1장 | `invoices/ci` 재호출 → 400 |
| 잔량 초과 출하 불가 | `shipments` → 400 |
| 확정된 출하는 삭제 불가 | `DELETE shipments/{id}` → 400 |
| 미수 잔액 초과 입금 불가 | `payments` → 400 |
| 인증 안 된 계정 로그인 불가 | `login` → 403 |
| 댓글 미지원 게시판에 댓글 불가 | `comments` → 400 |
| 첨부 미지원 게시판에 업로드 불가 | `files` → 400 |
| 답글에 다시 답글 불가 (1단계까지) | `comments` → 400 |
| 비회원 글은 비밀번호 없이 수정 불가 | `PUT posts/{id}` → 400 |
| 비로그인은 회원 전용 게시판 접근 불가 | `QNA/posts` → 401 |
| 거래처·제품 코드 중복 불가 | `POST customers` / `POST products` → 400 |
| 단가 기간 겹침 (자동마감 미선택) | `POST prices` → 400 |
| 미래 단가가 있으면 소급 등록 불가 | `POST prices` → 400 |
| 재고를 음수로 조정 불가 | `stock-adjust` → 400 |
| 엑셀 아닌 파일 업로드 불가 | `excel/*/import` → 400 |
| 양식 헤더가 다르면 한 줄도 안 읽음 | `excel/*/import` → 400 |
| 없는 문서에 첨부 불가 (FK 대신) | `POST attachments` → 404 |
| 허용되지 않은 `refType` 불가 | `attachments` → 400 |

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

**실제 잠금 순서** — 출하 확정은 항상 이 순서를 지킨다:

```
1) SALES_ORDER (수주 행)
2) INVENTORY  (제품 ID 오름차순)
```

이 순서를 모든 경로에서 지켜야 서로 다른 자원을 반대로 잡는 데드락이 안 생긴다.

### 동시성 테스트 — 실측 결과 ⭐

`src/test/java/.../shipment/ShipmentConcurrencyTest.java` 에 2건이 있다.
실제 Oracle 에 붙어 `CountDownLatch` 로 10개 스레드를 **동시에** 출발시킨다.

> `ready`/`start` 두 개의 래치를 쓴다. 그냥 submit 만 하면 스레드가 순차로 시작돼
> 경합이 거의 일어나지 않는다. 전원을 출발선에 세운 뒤 한 번에 풀어줘야 진짜로 부딪힌다.

**테스트가 증명하는 것은 "락을 걸었다"가 아니라 "락을 떼면 깨진다"다.**
실제로 떼어 보고 확인한 결과, 두 락의 실패 양상이 서로 달랐다.

| 뗀 락 | 결과 | 위험도 |
|---|---|---|
| `Inventory` 비관적 락 | 데이터는 안 깨짐. `@Version` 낙관적 락이 2차 방어선으로 걸려 **10건 중 9건이 `ObjectOptimisticLockingFailureException`** | 중 — 사용자가 계속 충돌 에러를 봄 |
| `SalesOrder` 비관적 락 | **조용히 깨짐.** 10건 모두 성공하고 재고는 600 빠지는데 `SHIPPED_QTY` 에는 **60** 만 남음 (실측: expected 600, but was 60). 예외 0건 | **높음 — 아무도 모른 채 장부가 어긋남** |

**여기서 배운 것**

1. **비관적 락의 역할은 정합성 그 자체가 아니다.** `@Version` 만 있어도 데이터는 지켜진다.
   비관적 락이 하는 일은 **경합을 대기로 바꿔 순차적으로 성공시키는 것**이다.
   설계 노트에서 "두 락을 다 써보고 차이를 설명할 수 있는 게 최선"이라 했는데, 그 차이가 이것이다.

2. **`@Version` 이 없는 테이블이 진짜 위험하다.** `SALES_ORDER_ITEM` 에는 버전 컬럼이 없어
   막아 줄 것이 아무것도 없었다. 예외조차 뜨지 않아 발견이 늦는다.
   이 버그는 동시성 테스트를 쓰다가 발견해서 고쳤다.

3. 면접에서는 "락을 걸었습니다"가 아니라 **"이 테스트가 락 없이는 이렇게 깨집니다"** 로 말한다.

**주의**: 테스트가 개발 DB 에 직접 쓴다. `TST-` 접두사로 자기가 만든 행만 골라
`@AfterEach` 에서 지우고 재고도 원래 값으로 복원한다. 강제 종료되면 정리가 안 될 수 있다.

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
