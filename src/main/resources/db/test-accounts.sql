-- ====================================================================
-- 테스트 계정 10건
--
-- 비밀번호는 전부 'password' 다. oracle_ddl.sql 의 admin 과 같은 BCrypt 해시를
-- 그대로 쓴다 — 해시를 새로 만들면 앱의 BCryptPasswordEncoder 설정과 어긋날 수 있다.
--
-- EMAIL_VERIFIED_YN='Y', STATUS='ACTIVE' 로 넣는다. 회원가입 API 로 만들면
-- PENDING 상태로 남고 인증 메일을 받아야 하는데, 배포 환경에는 메일 서버가 없다.
--
-- 다시 실행해도 되도록 같은 LOGIN_ID 를 먼저 지운다. admin 은 건드리지 않는다.
-- ====================================================================

DELETE FROM MEMBER WHERE LOGIN_ID IN (
    'admin2','sales1','sales2','sales3',
    'user01','user02','user03','user04','user05','user06'
);

INSERT INTO MEMBER
    (LOGIN_ID, PASSWORD, NAME, EMAIL, PHONE, ROLE, CUSTOMER_ID, DEPT, POSITION,
     EMAIL_VERIFIED_YN, STATUS, CREATED_BY)
VALUES
-- 관리자 1
('admin2', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 '김관리', 'admin2@example.com', '010-1000-0001', 'ADMIN', NULL, '경영지원팀', '팀장',
 'Y', 'ACTIVE', 'seed'),

-- 영업 3
('sales1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 '박영업', 'sales1@example.com', '010-2000-0001', 'SALES', NULL, '해외영업1팀', '과장',
 'Y', 'ACTIVE', 'seed'),
('sales2', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 '이수출', 'sales2@example.com', '010-2000-0002', 'SALES', NULL, '해외영업1팀', '대리',
 'Y', 'ACTIVE', 'seed'),
('sales3', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 '최무역', 'sales3@example.com', '010-2000-0003', 'SALES', NULL, '해외영업2팀', '사원',
 'Y', 'ACTIVE', 'seed'),

-- 거래처 담당자 6 — CUSTOMER_ID 는 sample-data.sql 이 넣은 거래처 1~5 를 가리킨다
('user01', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'John Smith', 'user01@example.com', '010-3000-0001', 'CUSTOMER', 1, NULL, 'Buyer',
 'Y', 'ACTIVE', 'seed'),
('user02', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Klaus Mueller', 'user02@example.com', '010-3000-0002', 'CUSTOMER', 2, NULL, 'Buyer',
 'Y', 'ACTIVE', 'seed'),
('user03', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Yuki Tanaka', 'user03@example.com', '010-3000-0003', 'CUSTOMER', 3, NULL, 'Buyer',
 'Y', 'ACTIVE', 'seed'),
('user04', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'David Tan', 'user04@example.com', '010-3000-0004', 'CUSTOMER', 4, NULL, 'Buyer',
 'Y', 'ACTIVE', 'seed'),
('user05', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Nguyen Van Minh', 'user05@example.com', '010-3000-0005', 'CUSTOMER', 5, NULL, 'Buyer',
 'Y', 'ACTIVE', 'seed'),

-- 휴면 계정 1 — 상태별 화면 확인용. 로그인은 막혀야 정상이다.
('user06', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 '정휴면', 'user06@example.com', '010-3000-0006', 'CUSTOMER', 1, NULL, 'Buyer',
 'Y', 'DORMANT', 'seed');

COMMIT;

-- 확인
SELECT MEMBER_ID, LOGIN_ID, NAME, ROLE, STATUS FROM MEMBER ORDER BY MEMBER_ID;
