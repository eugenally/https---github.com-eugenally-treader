# Oracle to MariaDB 마이그레이션 가이드

## 개요
Oracle 데이터베이스로 작성된 TreaderAPP 스키마를 MariaDB 호환 형식으로 변환했습니다.

## 주요 변환 사항

### 1. 데이터베이스/스키마
| Oracle | MariaDB |
|--------|---------|
| `ALTER SESSION SET CONTAINER = XEPDB1` | `CREATE DATABASE treader_db` |
| `CREATE USER boot_user` | MariaDB 데이터베이스 권한 (별도 사용자 생성 불필요) |
| `ALTER SESSION SET CURRENT_SCHEMA` | `USE treader_db` |

### 2. 데이터 타입 매핑
| Oracle | MariaDB | 설명 |
|--------|---------|------|
| `NUMBER(19)` | `BIGINT` | 큰 정수 (자동 증가) |
| `NUMBER(3)`, `NUMBER(10)` | `INT` | 일반 정수 |
| `NUMBER(5,2)`, `NUMBER(18,4)` | `DECIMAL(x,y)` | 소수점이 필요한 숫자 |
| `VARCHAR2(n)` | `VARCHAR(n)` | 가변 길이 문자열 |
| `CHAR(n)` | `CHAR(n)` | 고정 길이 문자열 (동일) |
| `CLOB` | `LONGTEXT` | 긴 텍스트 (최대 4GB) |
| `DATE` | `DATE` | 날짜 (동일) |
| `TIMESTAMP` | `TIMESTAMP` | 타임스탬프 (동일) |

### 3. 시퀀스 → AUTO_INCREMENT
**Oracle:**
```sql
CREATE SEQUENCE SEQ_MEMBER START WITH 1 INCREMENT BY 1 NOCACHE;
INSERT INTO MEMBER (MEMBER_ID, ...) VALUES (SEQ_MEMBER.NEXTVAL, ...);
```

**MariaDB:**
```sql
CREATE TABLE MEMBER (
    MEMBER_ID BIGINT NOT NULL AUTO_INCREMENT,
    ...
    PRIMARY KEY (MEMBER_ID)
);
INSERT INTO MEMBER (LOGIN_ID, ...) VALUES (...);
-- MEMBER_ID는 자동으로 증가
```

### 4. 기본값 함수
| Oracle | MariaDB |
|--------|---------|
| `DEFAULT SYSTIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` |
| `SYSDATE` | `CURDATE()` 또는 `NOW()` |

### 5. 타임스탬프 자동 업데이트
**Oracle:**
```sql
UPDATED_AT TIMESTAMP
-- 수동으로 업데이트 필요
```

**MariaDB:**
```sql
UPDATED_AT TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP
-- 자동으로 현재 시간으로 업데이트됨
```

### 6. NULL 타임스탬프
**Oracle:**
```sql
PWD_CHANGED_AT TIMESTAMP
```

**MariaDB:**
```sql
PWD_CHANGED_AT TIMESTAMP NULL
-- NULL 값을 저장할 수 있도록 명시
```

### 7. 제약조건 (Constraint)
| Oracle | MariaDB |
|--------|---------|
| `CONSTRAINT PK_X PRIMARY KEY` | `PRIMARY KEY` (동일) |
| `CONSTRAINT UK_X UNIQUE` | `UNIQUE KEY` (동일) |
| `CONSTRAINT FK_X FOREIGN KEY` | `FOREIGN KEY` (동일) |
| `CASCADE CONSTRAINTS` | 자동 처리 (제거) |

### 8. PL/SQL 블록 제거
**Oracle:**
```sql
BEGIN
    FOR t IN (SELECT table_name FROM user_tables) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE "' || t.table_name || '" CASCADE CONSTRAINTS';
    END LOOP;
END;
/
```

**MariaDB:**
```sql
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS TABLE_NAME;
DROP TABLE IF EXISTS TABLE_NAME2;
-- ...
SET FOREIGN_KEY_CHECKS = 1;
```

### 9. 인덱스 정의
**Oracle:**
```sql
CREATE INDEX IX_POST_LIST ON POST (BOARD_ID, DEL_YN, POST_ID DESC);
-- 테이블과 분리됨
```

**MariaDB:**
```sql
CREATE TABLE POST (
    ...
    INDEX IX_POST_LIST (BOARD_ID, DEL_YN, POST_ID DESC)
);
-- 또는 별도로: CREATE INDEX IX_POST_LIST ON POST (BOARD_ID, DEL_YN, POST_ID DESC);
```

### 10. 인코딩 설정
**MariaDB 추가:**
```sql
DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
-- 모든 테이블에 명시적으로 설정 (한글 지원)
```

### 11. 스토리지 엔진
**MariaDB 추가:**
```sql
ENGINE=InnoDB
-- 트랜잭션 지원을 위해 명시적으로 설정
```

### 12. 대소문자 처리
**Oracle:**
- 테이블/컬럼명이 대문자로 저장됨
- 쿼리에서 `"TABLE_NAME"` 처럼 따옴표 사용

**MariaDB:**
- 테이블명은 파일시스템에 따라 대소문자 처리 (Windows: 무시, Linux: 구분)
- 컬럼명은 대소문자 구분 안 함
- 더 안전하게 하려면 소문자 사용 권장

## 마이그레이션 단계

### Step 1: MariaDB 설치
```bash
# Windows (Chocolatey)
choco install mariadb

# Linux (Ubuntu/Debian)
sudo apt-get install mariadb-server

# Docker
docker run --name treader-mariadb -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d mariadb:latest
```

### Step 2: 데이터베이스 생성
```bash
mysql -u root -p < mariadb_ddl.sql
```

### Step 3: 기존 데이터 마이그레이션
Oracle에서 데이터 추출:
```sql
-- Oracle에서
SELECT * FROM MEMBER;
-- CSV 또는 데이터 펌프로 추출
```

MariaDB로 데이터 로드:
```sql
-- MariaDB에서
LOAD DATA INFILE '/path/to/data.csv' 
INTO TABLE MEMBER 
FIELDS TERMINATED BY ',' 
LINES TERMINATED BY '\n';
```

### Step 4: 애플리케이션 연결 설정

**application.properties (Spring Boot):**
```properties
# Oracle 설정
# spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
# spring.datasource.username=boot_user
# spring.datasource.password=1234
# spring.datasource.driver-class-name=oracle.jdbc.driver.OracleDriver

# MariaDB 설정
spring.datasource.url=jdbc:mysql://localhost:3306/treader_db?useUnicode=true&characterEncoding=utf8mb4
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# JPA 설정
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.MariaDB103Dialect
```

**pom.xml (Maven):**
```xml
<!-- Oracle 제거 -->
<!-- <dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
</dependency> -->

<!-- MariaDB 추가 -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.1.4</version>
</dependency>
```

## 주의사항

### 1. 시퀀스 관련
- Oracle의 `.NEXTVAL` 사용 불가
- JPA `@GeneratedValue(strategy = GenerationType.IDENTITY)` 사용
- Hibernate에서 자동으로 AUTO_INCREMENT 처리

### 2. 트랜잭션 격리 수준
MariaDB 기본값 확인:
```sql
SHOW VARIABLES LIKE 'transaction_isolation';
-- MariaDB 기본: REPEATABLE-READ
```

### 3. 시간대 설정
```sql
-- MariaDB 시간대 설정 (UTC 권장)
SET time_zone = '+00:00';
SELECT NOW(); -- 현재 시간 확인
```

### 4. 초기 데이터
- AUTO_INCREMENT 사용 시 ID 명시하면 안 됨
- INSERT 시 ID 필드 생략
- 예: `INSERT INTO MEMBER (LOGIN_ID, ...) VALUES ('admin', ...);`

### 5. 대소문자 처리
Windows에서 테스트한 스크립트:
- 테이블명 대문자/소문자 혼합 가능
- Linux 운영 환경에서는 일관성 필요
- 권장: 테이블명 소문자 통일 (application-level naming convention)

## 성능 고려사항

### MariaDB 최적화 설정
```sql
-- MariaDB 설정 파일 (my.cnf / my.ini)
[mysqld]
innodb_buffer_pool_size = 1G        # 시스템 메모리의 50-75%
innodb_log_file_size = 256M         # 충분한 로그 버퍼
max_connections = 1000              # 연결 수 제한
query_cache_size = 64M              # 쿼리 캐시 (선택사항)
slow_query_log = 1                  # 느린 쿼리 로그
long_query_time = 2                 # 2초 이상 쿼리
```

### 인덱스 최적화
```sql
-- 자주 사용되는 조회 쿼리 인덱스 추가
CREATE INDEX IX_MEMBER_EMAIL ON MEMBER(EMAIL);
CREATE INDEX IX_MEMBER_LOGIN_ID ON MEMBER(LOGIN_ID);
CREATE INDEX IX_POST_CREATED_AT ON POST(CREATED_AT DESC);
```

## 검증 체크리스트

- [ ] 모든 테이블 생성 확인: `SHOW TABLES;`
- [ ] 테이블 구조 확인: `DESC TABLE_NAME;`
- [ ] 제약조건 확인: `SHOW CREATE TABLE TABLE_NAME;`
- [ ] 초기 데이터 확인: `SELECT COUNT(*) FROM BOARD;`
- [ ] 외래키 무결성 테스트: 부모 데이터 없이 자식 입력 시도
- [ ] AUTO_INCREMENT 동작 테스트: 데이터 입력 후 ID 자동증가 확인
- [ ] 타임스탬프 자동 업데이트 테스트: 업데이트 후 `UPDATED_AT` 변경 확인

## 자주 묻는 질문 (FAQ)

**Q: 기존 Oracle 데이터를 MariaDB로 옮기려면?**
A: 데이터 펌프, sqoop, 또는 데이터 변환 도구 사용. 복잡한 경우 DBA 상담.

**Q: MariaDB와 MySQL의 차이는?**
A: MariaDB는 MySQL의 커뮤니티 포크. 대부분 호환되지만, 일부 기능은 다를 수 있음.

**Q: SEQUENCE를 다시 생성해야 하나?**
A: 아니오. AUTO_INCREMENT를 사용하므로 시퀀스 불필요.

**Q: 쿼리는 그대로 사용 가능한가?**
A: 대부분 그대로 가능. Oracle 특화 함수(예: `TRUNC`, `TO_CHAR` 등)는 변경 필요.

## 참고 자료
- [MariaDB 공식 문서](https://mariadb.com/docs/)
- [Oracle to MySQL 마이그레이션 가이드](https://dev.mysql.com/doc/refman/8.0/en/)
- [MariaDB 타입 시스템](https://mariadb.com/kb/en/data-types/)
