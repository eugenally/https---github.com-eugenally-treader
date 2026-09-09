# Lightsail 콘솔 배포 가이드 — TreaderAPP

이 계정(`733866507097`)은 조직 `276096488803` 의 멤버라 **EC2 와 RDS 가 SCP 로 차단**되어 있다.
SCP 는 콘솔·CLI 를 가리지 않고, 멤버 계정의 루트 사용자에게도 적용되므로 우회할 수 없다.
Lightsail 은 그 차단에 걸려 있지 않아 이 경로를 쓴다.

**구성**: Lightsail 인스턴스 1대에 Spring Boot + MariaDB 를 같이 올린다.
React 는 WAR 안에 들어 있어 별도 배포가 없다.

```
사용자 → http://<고정 IP>:8080 → Lightsail 인스턴스
                                   ├ Spring Boot (treader.war)
                                   └ MariaDB
```

관리형 DB(Lightsail Database, 월 $15~)를 따로 두는 방법도 있지만, 데모 규모에서는
같은 인스턴스에 MariaDB 를 설치하는 편이 절반 값이고 관리 지점도 하나다.

> **HTTPS 아님.** 도메인이 없어 평문 HTTP 로 뜬다. JWT 가 그대로 노출되니
> 실사용 서비스라면 도메인을 붙이고 HTTPS 를 켤 것. 포트폴리오 전제의 구성이다.

---

## 0. 먼저 1분만 확인

Lightsail 이 정말 열려 있는지부터 본다. 막혀 있으면 아래 단계가 전부 무의미하다.

1. 콘솔 상단 검색창에 **Lightsail** → 서비스 진입
2. 우상단 리전을 **서울(ap-northeast-2)** 로
3. **"인스턴스 생성"** 버튼이 눌리는지 확인

여기서 권한 오류나 "이 작업을 수행할 권한이 없습니다" 가 뜨면 Lightsail 도 막힌 것이다.
그 경우 조직 관리자에게 요청하거나 개인 계정을 새로 만드는 수밖에 없다.

---

## 1. 인스턴스 생성

**Lightsail → 인스턴스 → 인스턴스 생성**

| 항목 | 선택 |
|---|---|
| 리전 | 서울 (ap-northeast-2) |
| 플랫폼 | Linux/Unix |
| 블루프린트 | **OS 전용 → Amazon Linux 2023** |
| 플랜 | **2GB RAM / 2 vCPU / 60GB SSD** (월 $12) |
| 이름 | `treader-app` |

플랜 선택이 중요하다. 1GB($5) 는 Spring Boot 기동 중 OOM 이 나기 쉽다.
Java 힙에 더해 Gradle 없이 WAR 만 띄워도 500MB 안팎을 쓰고, 여기에 MariaDB 가 얹힌다.

> 첫 3개월 무료 프로모션이 표시되면 그대로 받으면 된다.

**"인스턴스 생성"** 을 누르고 상태가 `실행 중` 이 될 때까지 1~2분 기다린다.

---

## 2. 고정 IP 연결

기본 공인 IP 는 인스턴스를 재시작하면 바뀐다. 고정 IP 를 붙인다.
인스턴스에 연결해 두는 동안은 **무료**다(연결하지 않고 방치하면 과금).

**Lightsail → 네트워킹 → 고정 IP 생성**

- 리전: 서울
- 연결할 인스턴스: `treader-app`
- 이름: `treader-ip`

생성된 IP 를 적어 둔다. 이하 `<고정IP>` 로 표기한다.

---

## 3. 방화벽에 8080 열기

**인스턴스 `treader-app` → 네트워킹 탭 → IPv4 방화벽 → 규칙 추가**

| 애플리케이션 | 프로토콜 | 포트 |
|---|---|---|
| 사용자 지정 | TCP | **8080** |

SSH(22) 는 기본으로 열려 있다.

---

## 4. 접속

**인스턴스 → "SSH를 사용하여 연결"** 버튼이면 브라우저에서 바로 터미널이 열린다.
키 파일도 필요 없다. 아래 명령은 그 터미널에 붙여 넣는다.

---

## 5. Java 와 MariaDB 설치

```bash
sudo dnf update -y
sudo dnf install -y java-21-amazon-corretto-headless mariadb105-server

sudo systemctl enable --now mariadb
mariadb --version
```

DB 계정과 스키마 그릇을 만든다. `<DB비밀번호>` 는 직접 정한다.

```bash
sudo mysql -e "
CREATE DATABASE treader_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'treader_user'@'localhost' IDENTIFIED BY '<DB비밀번호>';
GRANT ALL PRIVILEGES ON treader_db.* TO 'treader_user'@'localhost';
FLUSH PRIVILEGES;"
```

> MariaDB 를 외부에 열지 않는다. 앱이 같은 장비에 있으므로 `localhost` 로 충분하고,
> 3306 을 방화벽에 추가하지 않으면 인터넷에서 닿지 않는다.

앱 디렉터리를 만든다.

```bash
sudo mkdir -p /app/treader /app/uploads /app/logs
sudo chown -R ec2-user:ec2-user /app
```

---

## 6. 스키마와 샘플 데이터 넣기

DDL 두 개를 인스턴스로 옮겨야 한다. 파일이 작아서 브라우저 SSH 의 업로드 기능으로 충분하다.

**브라우저 SSH 창 우하단 → 업로드 아이콘** 으로 다음 두 파일을 올린다.

- `src/main/resources/db/oracle_ddl.sql`
- `src/main/resources/db/sample-data.sql`

올린 파일은 `/home/ec2-user/` 에 떨어진다. 적재한다.

```bash
cd /home/ec2-user
mysql -u treader_user -p treader_db < oracle_ddl.sql
mysql -u treader_user -p treader_db < sample-data.sql

mysql -u treader_user -p treader_db -e "
SELECT COUNT(*) AS tables_ FROM information_schema.TABLES WHERE TABLE_SCHEMA='treader_db';
SELECT COUNT(*) AS customers FROM CUSTOMER;"
```

기대값: `tables_` **25**, `customers` **5**.

> 파일명이 `oracle_ddl.sql` 이지만 내용은 MariaDB DDL 이다. 마이그레이션 때 이름만 남았다.
> 두 스크립트 모두 MariaDB 10.6·12.2 에서 오류 0건으로 검증했다.

---

## 7. WAR 빌드해서 올리기

**로컬(내 PC)** 에서 빌드한다. 프론트엔드를 먼저 빌드해야 WAR 에 최신 화면이 들어간다.

```bash
cd frontend-react && npm ci && npm run build && cd ..
./gradlew clean bootWar -x test

unzip -l build/libs/treader.war | grep 'static/index.html'   # 포함 확인
```

WAR 는 90MB 라 브라우저 업로드로는 느리고 자주 끊긴다. **SSH 키를 받아 `scp` 로 보낸다.**

**Lightsail → 계정 → SSH 키 → 기본 키 다운로드** (`LightsailDefaultKey-ap-northeast-2.pem`)

```bash
# 로컬에서
chmod 600 ~/Downloads/LightsailDefaultKey-ap-northeast-2.pem
scp -i ~/Downloads/LightsailDefaultKey-ap-northeast-2.pem \
    build/libs/treader.war ec2-user@<고정IP>:/app/treader/
```

---

## 8. 환경 설정과 서비스 등록

**인스턴스 터미널에서** `/app/treader/env.conf` 를 만든다.

```bash
cat > /app/treader/env.conf <<'CONF'
export SPRING_DATASOURCE_URL='jdbc:mariadb://localhost:3306/treader_db'
export SPRING_DATASOURCE_USERNAME=treader_user
export SPRING_DATASOURCE_PASSWORD='<DB비밀번호>'
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
export SPRING_JPA_HIBERNATE_DDL_AUTO=validate
export SPRING_JPA_SHOW_SQL=false

export APP_JWT_SECRET='<아래에서 만든 값>'
export APP_FRONTEND_BASE_URL=http://<고정IP>:8080
export APP_UPLOAD_DIR=/app/uploads

# API 문서는 닫는다. 켜 두면 전체 엔드포인트 구조가 인증 없이 노출된다.
export SPRINGDOC_API_DOCS_ENABLED=false
export SPRINGDOC_SWAGGER_UI_ENABLED=false
CONF

chmod 600 /app/treader/env.conf
```

JWT 서명키는 인스턴스에서 바로 만들어 위 파일에 넣는다.
저장소의 개발용 기본값을 그대로 쓰면 누구나 토큰을 위조할 수 있다.

```bash
openssl rand -base64 48
```

systemd 유닛을 만든다.

```bash
sudo tee /etc/systemd/system/treader.service > /dev/null <<'UNIT'
[Unit]
Description=TreaderAPP
After=network.target mariadb.service

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/app/treader
EnvironmentFile=/app/treader/env.conf
ExecStart=/usr/bin/java -Xmx768m -jar /app/treader/treader.war
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
UNIT

sudo systemctl daemon-reload
sudo systemctl enable --now treader
sudo systemctl status treader
```

> `-Xmx768m` 은 2GB 인스턴스에서 MariaDB 와 나눠 쓰기 위한 상한이다.
> 이걸 안 주면 JVM 이 메모리의 1/4 을 기본 힙으로 잡았다가 부하 때 함께 죽을 수 있다.

---

## 9. 확인

인스턴스 안에서:

```bash
curl -s localhost:8080/api/health
sudo journalctl -u treader -n 40 --no-pager
```

로컬 브라우저에서:

```
http://<고정IP>:8080
```

로그인 `admin` / `password` → 대시보드에 매출·미수금 차트가 뜨면 성공이다.

**샘플 데이터의 기본 비밀번호는 접속 확인 직후 바꿀 것.** 인터넷에 열려 있는 주소다.

문제가 있으면 이 순서로 좁힌다.

```bash
sudo systemctl status treader          # 서비스가 죽었나
sudo journalctl -u treader -n 50       # 예외 내용
mysql -u treader_user -p treader_db -e "SELECT 1"   # DB 접속되나
sudo ss -tlnp | grep 8080              # 8080 을 잡고 있나
```

밖에서 안 열리면 3번의 방화벽 규칙(8080)을 다시 확인한다.

---

## 10. 재배포

코드를 고친 뒤에는 이 세 줄이면 된다.

```bash
# 로컬
cd frontend-react && npm run build && cd .. && ./gradlew clean bootWar -x test
scp -i ~/Downloads/LightsailDefaultKey-ap-northeast-2.pem \
    build/libs/treader.war ec2-user@<고정IP>:/app/treader/
ssh -i ~/Downloads/LightsailDefaultKey-ap-northeast-2.pem \
    ec2-user@<고정IP> 'sudo systemctl restart treader'
```

---

## 11. 정리

Lightsail 은 **인스턴스를 정지해도 과금이 계속된다.** 쓰지 않으면 삭제해야 한다.

```
Lightsail → 인스턴스 → treader-app → ⋮ → 삭제
Lightsail → 네트워킹 → treader-ip → 삭제      ← 고정 IP 도 같이
```

고정 IP 는 인스턴스에서 분리된 채 남아 있으면 시간당 과금된다. 반드시 함께 지운다.

---

## 나중에 붙일 것

| 항목 | 지금 | 붙이려면 |
|---|---|---|
| HTTPS | 없음 (평문 HTTP) | 도메인 → Lightsail 로드밸런서(월 $18) 또는 인스턴스에 nginx + Let's Encrypt(무료) |
| 80 포트 | 8080 직접 노출 | nginx 리버스 프록시로 80 → 8080 |
| 이메일 인증 | 미설정 | SES 검증·샌드박스 해제 후 `SPRING_MAIL_*` 주입 |
| DB 백업 | 없음 | Lightsail 스냅샷 자동화, 또는 관리형 DB 로 이전 |
