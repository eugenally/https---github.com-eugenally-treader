# Treader Docker Compose 가이드

이 문서는 `treader` 프로젝트의 Docker Compose 환경 구성 및 사용법을 설명합니다.

---

## 1. 구성 서비스 요약

| 서비스명 | 이미지 | 호스트 포트 | 컨테이너 포트 | 용도 / 접속 정보 |
|---|---|---|---|---|
| **oracle** | `gvenzl/oracle-xe:21-slim` | **1523** | 1521 | Oracle XE 21c Database (`XEPDB1`) |
| **mailhog** | `mailhog/mailhog:latest` | **1025**<br>**8025** | 1025<br>8025 | 가상 SMTP 서버<br>웹 메일 모니터링 UI (`http://localhost:8025`) |
| **app** *(선택)* | Dockerfile 빌드 | **8080** | 8080 | Treader Spring Boot 백엔드 애플리케이션 |

> [!NOTE]
> **포트 1523 매핑 사유**: 로컬 PC에 설치된 기존 Oracle 리스너가 이미 1521 포트를 점유하고 있으므로, 충돌 방지를 위해 Docker 컨테이너의 1521 포트를 호스트 **1523** 포트로 연결하였습니다.

---

## 2. 데이터베이스 접속 정보 (SQL Developer / DBeaver)

- **호스트**: `localhost`
- **포트**: `1523`
- **서비스 이름 (Service Name)**: `XEPDB1`
- **사용자 계정**: `boot_user`
- **비밀번호**: `1234`
- **관리자 계정**: `SYSTEM` / 비밀번호: `1234`
- **JDBC URL**: `jdbc:oracle:thin:@localhost:1523/XEPDB1`

---

## 3. 실행 명령어

### 3.1 개발용 인프라(DB + 메일)만 실행 (권장)
로컬 IDE(STS, VS Code, IntelliJ)에서 애플리케이션 코드를 수정 및 디버깅할 때 사용합니다.

```bash
# 백그라운드로 Oracle 및 MailHog 실행
docker compose up -d

# 실행 상태 확인
docker compose ps

# Oracle 컨테이너 로그 확인 (최초 실행 시 DB 생성 로그 확인)
docker compose logs -f oracle
```

### 3.2 전체 스택(DB + MailHog + Spring Boot App) 실행
애플리케이션까지 컨테이너로 한 번에 실행하고자 할 때 사용합니다.

```bash
docker compose --profile app up -d --build
```

### 3.3 컨테이너 중지 및 종료
```bash
# 컨테이너 중지 (데이터는 볼륨에 보존됨)
docker compose down

# 컨테이너 및 볼륨(DB 데이터)까지 완전히 초기화
docker compose down -v
```

---

## 4. 파일 구성

```
treader/
├── docker-compose.yml       # Docker Compose 설정 파일
├── .env                     # 포트, 계정 등 환경변수 정의
├── Dockerfile               # Spring Boot 애플리케이션 컨테이너 빌드 파일
├── .dockerignore            # Docker 빌드 제외 파일 목록
├── DOCKER_GUIDE.md          # 본 가이드 문서
└── docker/
    └── oracle-init/
        └── 01_init.sql      # 컨테이너 최초 생성 시 자동 실행할 SQL 스크립트
```
