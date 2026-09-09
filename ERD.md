# 🗄️ TreaderAPP 데이터베이스 ERD

## 전체 다이어그램 (Mermaid)

```mermaid
erDiagram
    MEMBER ||--o{ QUOTATION : "작성"
    MEMBER ||--o{ SALES_ORDER : "관리"
    MEMBER ||--o{ BOARD_POST : "작성"
    MEMBER ||--o{ NOTIFICATION : "수신"
    CUSTOMER ||--o{ SALES_ORDER : "주문"
    CUSTOMER ||--o{ CONTACT : "담당자"
    CUSTOMER ||--o{ PRICE : "거래처단가"
    PRODUCT ||--o{ PRICE : "가격설정"
    PRODUCT ||--o{ QUOTATION_ITEM : "포함"
    PRODUCT ||--o{ SALES_ORDER_ITEM : "포함"
    PRODUCT ||--o{ SHIPMENT_ITEM : "출하"
    PRODUCT ||--o{ STOCK : "재고"
    QUOTATION ||--o{ QUOTATION_ITEM : "상세"
    QUOTATION ||--o{ SALES_ORDER : "변환"
    SALES_ORDER ||--o{ SALES_ORDER_ITEM : "상세"
    SALES_ORDER ||--o{ SHIPMENT : "출하대상"
    SHIPMENT ||--o{ SHIPMENT_ITEM : "상세"
    SHIPMENT ||--o{ INVOICE : "인보이스화"
    INVOICE ||--o{ PAYMENT : "입금"
    EXCHANGE_RATE ||--o{ INVOICE : "적용환율"
    BOARD ||--o{ BOARD_POST : "소속"
    BOARD_POST ||--o{ BOARD_COMMENT : "댓글"
    BOARD_POST ||--o{ BOARD_LIKE : "좋아요"
    ATTACHMENT ||--o{ BOARD_POST : "첨부"
    ATTACHMENT ||--o{ QUOTATION : "첨부"

    MEMBER : PK member_id INT
    MEMBER : role VARCHAR2(20)
    MEMBER : login_id VARCHAR2(50)
    MEMBER : password_hash VARCHAR2(255)
    MEMBER : email VARCHAR2(100)
    MEMBER : name VARCHAR2(50)
    MEMBER : email_verified_yn CHAR(1)

    CUSTOMER : PK customer_id INT
    CUSTOMER : customer_code VARCHAR2(20)
    CUSTOMER : name_en VARCHAR2(100)
    CUSTOMER : name_ko VARCHAR2(100)
    CUSTOMER : active_yn CHAR(1)

    CONTACT : PK contact_id INT
    CONTACT : FK customer_id INT
    CONTACT : name VARCHAR2(50)
    CONTACT : email VARCHAR2(100)
    CONTACT : phone VARCHAR2(20)

    PRODUCT : PK product_id INT
    PRODUCT : product_code VARCHAR2(20)
    PRODUCT : name_en VARCHAR2(100)
    PRODUCT : name_ko VARCHAR2(100)
    PRODUCT : hs_code VARCHAR2(20)
    PRODUCT : active_yn CHAR(1)

    PRICE : PK price_id INT
    PRICE : FK product_id INT
    PRICE : FK customer_id INT
    PRICE : unit_price DECIMAL(12,2)
    PRICE : start_date DATE
    PRICE : end_date DATE

    STOCK : PK stock_id INT
    STOCK : FK product_id INT
    STOCK : qty INT
    STOCK : unit VARCHAR2(20)

    QUOTATION : PK quotation_id INT
    QUOTATION : FK member_id INT
    QUOTATION : quotation_no VARCHAR2(50)
    QUOTATION : FK customer_id INT
    QUOTATION : status VARCHAR2(20)
    QUOTATION : valid_until DATE

    QUOTATION_ITEM : PK item_id INT
    QUOTATION_ITEM : FK quotation_id INT
    QUOTATION_ITEM : FK product_id INT
    QUOTATION_ITEM : qty INT
    QUOTATION_ITEM : unit_price DECIMAL(12,2)

    SALES_ORDER : PK order_id INT
    SALES_ORDER : FK member_id INT
    SALES_ORDER : order_no VARCHAR2(50)
    SALES_ORDER : FK customer_id INT
    SALES_ORDER : FK quotation_id INT
    SALES_ORDER : status VARCHAR2(20)
    SALES_ORDER : order_date DATE
    SALES_ORDER : delivery_date DATE

    SALES_ORDER_ITEM : PK item_id INT
    SALES_ORDER_ITEM : FK order_id INT
    SALES_ORDER_ITEM : FK product_id INT
    SALES_ORDER_ITEM : qty INT
    SALES_ORDER_ITEM : unit_price DECIMAL(12,2)
    SALES_ORDER_ITEM : shipped_qty INT

    SHIPMENT : PK shipment_id INT
    SHIPMENT : shipment_no VARCHAR2(50)
    SHIPMENT : FK order_id INT
    SHIPMENT : status VARCHAR2(20)
    SHIPMENT : etd DATE
    SHIPMENT : eta DATE

    SHIPMENT_ITEM : PK item_id INT
    SHIPMENT_ITEM : FK shipment_id INT
    SHIPMENT_ITEM : FK order_item_id INT
    SHIPMENT_ITEM : qty INT

    INVOICE : PK invoice_id INT
    INVOICE : invoice_no VARCHAR2(50)
    INVOICE : FK order_id INT
    INVOICE : invoice_type CHAR(2)
    INVOICE : amount DECIMAL(15,2)
    INVOICE : amount_paid DECIMAL(15,2)
    INVOICE : exchange_rate DECIMAL(12,4)
    INVOICE : due_date DATE
    INVOICE : status VARCHAR2(20)

    PAYMENT : PK payment_id INT
    PAYMENT : FK invoice_id INT
    PAYMENT : amount DECIMAL(15,2)
    PAYMENT : payment_date DATE

    EXCHANGE_RATE : PK rate_id INT
    EXCHANGE_RATE : base_date DATE
    EXCHANGE_RATE : currency_unit VARCHAR2(3)
    EXCHANGE_RATE : deal_bas_rate DECIMAL(12,4)

    MEMBER : created_by VARCHAR2(50)
    MEMBER : created_at TIMESTAMP
    MEMBER : updated_by VARCHAR2(50)
    MEMBER : updated_at TIMESTAMP

    BOARD : PK board_id INT
    BOARD : board_code VARCHAR2(20)
    BOARD : board_name VARCHAR2(100)

    BOARD_POST : PK post_id INT
    BOARD_POST : FK board_id INT
    BOARD_POST : FK member_id INT
    BOARD_POST : title VARCHAR2(255)
    BOARD_POST : content CLOB
    BOARD_POST : view_count INT
    BOARD_POST : answered_yn CHAR(1)

    BOARD_COMMENT : PK comment_id INT
    BOARD_COMMENT : FK post_id INT
    BOARD_COMMENT : FK member_id INT
    BOARD_COMMENT : content VARCHAR2(1000)

    BOARD_LIKE : PK like_id INT
    BOARD_LIKE : FK post_id INT
    BOARD_LIKE : FK member_id INT

    ATTACHMENT : PK attachment_id INT
    ATTACHMENT : ref_type VARCHAR2(50)
    ATTACHMENT : ref_id INT
    ATTACHMENT : file_name VARCHAR2(255)
    ATTACHMENT : file_key VARCHAR2(255)
    ATTACHMENT : file_size INT

    NOTIFICATION : PK notification_id INT
    NOTIFICATION : FK member_id INT
    NOTIFICATION : notif_type VARCHAR2(50)
    NOTIFICATION : title VARCHAR2(255)
    NOTIFICATION : message VARCHAR2(1000)
    NOTIFICATION : ref_type VARCHAR2(50)
    NOTIFICATION : ref_id INT
    NOTIFICATION : read_yn CHAR(1)
```

---

## 테이블 상세

### 👤 MEMBER (회원)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **MEMBER_ID** | NUMBER(10) | PK, 자동증가 |
| LOGIN_ID | VARCHAR2(50) | 고유 로그인 ID |
| PASSWORD_HASH | VARCHAR2(255) | bcrypt 해시 |
| EMAIL | VARCHAR2(100) | 이메일 (인증 대상) |
| NAME | VARCHAR2(50) | 사용자명 |
| ROLE | VARCHAR2(20) | MEMBER / ADMIN |
| EMAIL_VERIFIED_YN | CHAR(1) | Y/N (이메일 인증 여부) |
| CREATED_AT | TIMESTAMP | 가입일시 |

**인덱스**
- UNIQUE(LOGIN_ID)
- UNIQUE(EMAIL)

---

### 🏢 CUSTOMER (거래처)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **CUSTOMER_ID** | NUMBER(10) | PK |
| CUSTOMER_CODE | VARCHAR2(20) | 거래처 코드 (고유) |
| NAME_EN | VARCHAR2(100) | 영문명 |
| NAME_KO | VARCHAR2(100) | 한글명 |
| ACTIVE_YN | CHAR(1) | Y/N (활성화) |

**관계**
- 1:N CONTACT (담당자)
- 1:N SALES_ORDER
- 1:N PRICE (거래처별 단가)

---

### 📦 PRODUCT (제품)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **PRODUCT_ID** | NUMBER(10) | PK |
| PRODUCT_CODE | VARCHAR2(20) | 제품 코드 |
| NAME_EN | VARCHAR2(100) | 영문명 |
| NAME_KO | VARCHAR2(100) | 한글명 |
| HS_CODE | VARCHAR2(20) | HS 관세 코드 |
| ACTIVE_YN | CHAR(1) | Y/N |

---

### 💰 PRICE (단가 이력)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **PRICE_ID** | NUMBER(10) | PK |
| PRODUCT_ID | NUMBER(10) | FK (제품) |
| CUSTOMER_ID | NUMBER(10) | FK (거래처) / NULL이면 표준가 |
| UNIT_PRICE | DECIMAL(12,2) | USD 단가 |
| START_DATE | DATE | 유효 시작 |
| END_DATE | DATE | 유효 종료 |

**제약조건**
- 기간 겹침 방지 (CHECK 또는 트리거)
- CUSTOMER_ID + START_DATE + END_DATE 복합 인덱스

---

### 📋 QUOTATION (견적)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **QUOTATION_ID** | NUMBER(10) | PK |
| QUOTATION_NO | VARCHAR2(50) | 견적 번호 (고유) |
| MEMBER_ID | NUMBER(10) | FK (작성자) |
| CUSTOMER_ID | NUMBER(10) | FK (거래처) |
| STATUS | VARCHAR2(20) | DRAFT / SENT / ACCEPTED / EXPIRED / REJECTED |
| VALID_UNTIL | DATE | 견적 유효기간 |

**상태 전이**
```
DRAFT → SENT → ACCEPTED → (SALES_ORDER 변환)
                ↓
              REJECTED
              
또는 EXPIRED (자동)
```

---

### 🛒 SALES_ORDER (수주)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **ORDER_ID** | NUMBER(10) | PK |
| ORDER_NO | VARCHAR2(50) | 수주 번호 (고유) |
| MEMBER_ID | NUMBER(10) | FK (관리자) |
| CUSTOMER_ID | NUMBER(10) | FK |
| QUOTATION_ID | NUMBER(10) | FK (출처 견적) |
| STATUS | VARCHAR2(20) | PENDING / PARTIAL_SHIPPED / SHIPPED / INVOICED |
| ORDER_DATE | DATE | 수주일 |
| DELIVERY_DATE | DATE | 납기일 (D-7 알림 대상) |

**관계**
- 1:N SALES_ORDER_ITEM
- 1:N SHIPMENT
- 1:N INVOICE

---

### 📦 SHIPMENT (출하)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **SHIPMENT_ID** | NUMBER(10) | PK |
| SHIPMENT_NO | VARCHAR2(50) | 선적 번호 |
| ORDER_ID | NUMBER(10) | FK |
| STATUS | VARCHAR2(20) | DRAFT / CONFIRMED / SHIPPED |
| ETD | DATE | Estimated Time of Departure |
| ETA | DATE | Estimated Time of Arrival |

**제약조건**
- 비관적 락으로 출하 확정 시 동시성 보호

---

### 📄 INVOICE (인보이스)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **INVOICE_ID** | NUMBER(10) | PK |
| INVOICE_NO | VARCHAR2(50) | 인보이스 번호 |
| ORDER_ID | NUMBER(10) | FK |
| INVOICE_TYPE | CHAR(2) | CI (상업) / PI (형식) |
| AMOUNT | DECIMAL(15,2) | USD 금액 |
| AMOUNT_PAID | DECIMAL(15,2) | 입금액 |
| EXCHANGE_RATE | DECIMAL(12,4) | KRW 환율 |
| DUE_DATE | DATE | 결제기한 (연체 알림) |
| STATUS | VARCHAR2(20) | ISSUED / PARTIALLY_PAID / PAID / OVERDUE / VOID |

**상태 관리**
```
ISSUED → (입금) → PARTIALLY_PAID → PAID
         (기한경과) ↓
                  OVERDUE (매일 09:10 배치로 체크)
```

---

### 💵 EXCHANGE_RATE (환율)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **RATE_ID** | NUMBER(10) | PK |
| BASE_DATE | DATE | 기준일 |
| CURRENCY_UNIT | VARCHAR2(3) | USD / EUR / CNY / JPY |
| DEAL_BAS_RATE | DECIMAL(12,4) | 기준 환율 |

**데이터 수집**
- 매일 11:30 (평일): Korea Exim Bank API
- 공휴일 제외 (이전 거래일 사용)

---

### 📝 BOARD_POST (게시글)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **POST_ID** | NUMBER(10) | PK |
| BOARD_ID | NUMBER(10) | FK |
| MEMBER_ID | NUMBER(10) | FK / NULL (비회원 글) |
| TITLE | VARCHAR2(255) | 제목 |
| CONTENT | CLOB | 본문 |
| VIEW_COUNT | NUMBER(10) | 조회수 (SELECT FOR UPDATE 동시성) |
| ANSWERED_YN | CHAR(1) | Y/N (Q&A 게시판용) |

---

### 🔔 NOTIFICATION (알림)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| **NOTIFICATION_ID** | NUMBER(10) | PK |
| MEMBER_ID | NUMBER(10) | FK (수신자) |
| NOTIF_TYPE | VARCHAR2(50) | DELIVERY_DUE / PAYMENT_OVERDUE / QNA_ANSWERED |
| TITLE | VARCHAR2(255) | 제목 (이모지 포함) |
| MESSAGE | VARCHAR2(1000) | 본문 |
| REF_TYPE | VARCHAR2(50) | SALES_ORDER / INVOICE / POST |
| REF_ID | NUMBER(10) | 참조 ID (클릭 시 이동) |
| READ_YN | CHAR(1) | Y/N |
| CREATED_AT | TIMESTAMP | 생성일시 |

**생성 방식**
- 배치 작업 (ExchangeRateBatchJob)으로 자동 생성
- 사용자는 읽기만 가능

---

## 🔄 주요 상태 다이어그램

### 수주 흐름
```
Quotation          SalesOrder       Shipment        Invoice
┌────────┐        ┌────────┐      ┌────────┐      ┌────────┐
│ DRAFT  │─[send] │ PENDING│─[출] │ DRAFT  │─[확] │ ISSUED │
│        │        │        │  하  │        │  정  │        │
│ SENT   │─[OK]   │ PARTIAL│────> │ CONF'D │────> │ PAID   │
│        │─[NO]   │SHIPPED │      │        │      │ (입금) │
│ACCEPTED│        │ SHIPPED│      │SHIPPED │      │        │
└────────┘        └────────┘      └────────┘      └────────┘
   ↑                   ↑
   └─ EXPIRED          └─ OVERDUE (배치체크)
      (자동)                (배치체크)
```

---

## 📊 통계 쿼리 (M6)

### 1️⃣ 월별 매출 (YoY)
```sql
SELECT
  TRUNC(sh.ETD, 'MM') AS month,
  SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales,
  LAG(...) OVER (ORDER BY TRUNC(...)) AS prev_month_sales,
  ... AS yoy_growth
```

### 2️⃣ 거래처 TOP 5
```sql
SELECT
  c.NAME_EN,
  SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales,
  RANK() OVER (ORDER BY SUM(...) DESC) AS rank
```

### 3️⃣ 미수금 Aging
```sql
SELECT
  CASE WHEN days < 30 THEN '30일'
       WHEN days < 60 THEN '60일'
       ELSE '90일초과' END AS bucket,
  COUNT(*) AS invoice_count,
  RATIO_TO_REPORT(SUM(...)) OVER () AS percentage
```

---

## 🔒 동시성 제어

| 상황 | 전략 | 구현 |
|------|------|------|
| 재고 차감 | 비관적 락 | `@Lock(PESSIMISTIC_WRITE)` |
| 조회수 증가 | SELECT FOR UPDATE | Native Query |
| 출하 확정 | 트랜잭션 격리 | `ISOLATION_LEVEL.READ_COMMITTED` + 행 락 |

---

## 📋 감사 추적 (Audit)

모든 주요 엔티티:
```sql
CREATED_BY VARCHAR2(50)      -- 작성자
CREATED_AT TIMESTAMP          -- 작성일시
UPDATED_BY VARCHAR2(50)       -- 수정자
UPDATED_AT TIMESTAMP          -- 수정일시
```

Spring Data의 `@CreatedBy`, `@LastModifiedBy` + `AuditorAware`로 자동 기록.

---

**마지막 업데이트**: 2026-09-09

