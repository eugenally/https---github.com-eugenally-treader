# AWS 배포 가이드 — TreaderAPP

**구성**: 서울 리전(`ap-northeast-2`) / EC2 1대 + RDS MariaDB 1대 / ALB·도메인 없음 (최소 비용)

React 는 WAR 안에 들어 있어 Spring Boot 가 API 와 화면을 함께 서빙한다. 따라서 배포물은
`treader.war` 하나뿐이고, 프론트엔드용 S3·CloudFront 는 쓰지 않는다.

```
사용자 → http://<EC2 공인 IP>:8080 → EC2 (Spring Boot + React) → RDS MariaDB
```

> **HTTPS 아님.** 도메인이 없어 ACM 인증서를 못 받으므로 평문 HTTP 로 뜬다.
> JWT 가 그대로 노출되니 실제 사용자를 받는 서비스라면 도메인을 붙이고 HTTPS 를 켤 것.
> 포트폴리오·데모 용도라는 전제의 구성이다.

---

## 0. 사전 준비

```bash
winget install --id Amazon.AWSCLI -e
```

새 터미널에서:

```bash
aws configure
# Access Key ID / Secret Access Key: IAM 에서 발급
# Default region: ap-northeast-2
# Default output: json
```

확인:

```bash
aws sts get-caller-identity
```

---

## 1. RDS MariaDB 생성

프리티어는 `db.t4g.micro` · 20GB · 단일 AZ 기준 12개월 무료다.

기본 VPC 의 보안 그룹을 먼저 만든다.

```bash
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)

APP_SG=$(aws ec2 create-security-group --group-name treader-app-sg \
  --description "TreaderAPP EC2" --vpc-id $VPC_ID --query GroupId --output text)

RDS_SG=$(aws ec2 create-security-group --group-name treader-rds-sg \
  --description "TreaderAPP RDS" --vpc-id $VPC_ID --query GroupId --output text)

# RDS 는 EC2 에서만 접근 — 3306 을 인터넷에 열지 않는다
aws ec2 authorize-security-group-ingress --group-id $RDS_SG \
  --protocol tcp --port 3306 --source-group $APP_SG

echo "APP_SG=$APP_SG  RDS_SG=$RDS_SG"
```

파라미터 그룹 (utf8mb4 가 기본이 아니라 반드시 지정):

```bash
aws rds create-db-parameter-group \
  --db-parameter-group-name treader-mariadb-params \
  --db-parameter-group-family mariadb10.6 \
  --description "TreaderAPP utf8mb4"

aws rds modify-db-parameter-group \
  --db-parameter-group-name treader-mariadb-params \
  --parameters \
    "ParameterName=character_set_server,ParameterValue=utf8mb4,ApplyMethod=pending-reboot" \
    "ParameterName=collation_server,ParameterValue=utf8mb4_unicode_ci,ApplyMethod=pending-reboot"
```

인스턴스 생성:

```bash
aws rds create-db-instance \
  --db-instance-identifier treader-mariadb-prod \
  --db-instance-class db.t4g.micro \
  --engine mariadb --engine-version 10.6 \
  --master-username admin --master-user-password '<강한_비밀번호>' \
  --allocated-storage 20 --storage-type gp2 \
  --backup-retention-period 7 \
  --no-multi-az \
  --publicly-accessible \
  --vpc-security-group-ids $RDS_SG \
  --db-parameter-group-name treader-mariadb-params
```

> `--publicly-accessible` 로 두는 이유는 스키마를 로컬에서 넣기 위해서다. 적재가 끝나면
> `--no-publicly-accessible` 로 되돌리는 편이 안전하다. 그동안에도 보안 그룹이 EC2 만
> 허용하므로, 로컬에서 붙으려면 내 IP 를 한시적으로 열어야 한다.

생성은 5~10분 걸린다. 완료 후 엔드포인트 확인:

```bash
aws rds wait db-instance-available --db-instance-identifier treader-mariadb-prod
RDS_HOST=$(aws rds describe-db-instances --db-instance-identifier treader-mariadb-prod \
  --query 'DBInstances[0].Endpoint.Address' --output text)
echo $RDS_HOST
```

---

## 2. 스키마·데이터 적재

로컬에서 붙으려면 내 IP 를 잠시 연다.

```bash
MYIP=$(curl -s https://checkip.amazonaws.com)
aws ec2 authorize-security-group-ingress --group-id $RDS_SG \
  --protocol tcp --port 3306 --cidr $MYIP/32
```

계정과 DB 를 만든다.

```bash
mysql -h $RDS_HOST -u admin -p -e "
CREATE DATABASE IF NOT EXISTS treader_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'treader_user'@'%' IDENTIFIED BY '<DB_비밀번호>';
GRANT ALL PRIVILEGES ON treader_db.* TO 'treader_user'@'%';"
```

스키마와 샘플 데이터를 넣는다. 두 스크립트 모두 MariaDB 10.6·12.2 에서 오류 0건으로 검증했다.

```bash
mysql -h $RDS_HOST -u treader_user -p treader_db < src/main/resources/db/oracle_ddl.sql
mysql -h $RDS_HOST -u treader_user -p treader_db < src/main/resources/db/sample-data.sql

mysql -h $RDS_HOST -u treader_user -p treader_db -e "
SELECT COUNT(*) AS tables_ FROM information_schema.TABLES WHERE TABLE_SCHEMA='treader_db';
SELECT COUNT(*) AS customers FROM CUSTOMER;"
# 기대: tables_ 25, customers 5
```

> 파일명이 `oracle_ddl.sql` 이지만 내용은 MariaDB DDL 이다. 마이그레이션 때 이름만 남았다.

적재가 끝나면 열어둔 내 IP 규칙을 닫는다.

```bash
aws ec2 revoke-security-group-ingress --group-id $RDS_SG \
  --protocol tcp --port 3306 --cidr $MYIP/32
```

---

## 3. JWT 서명키 만들기

Secrets Manager 는 쓰지 않는다. 비밀값은 EC2 안 `env.conf` 에만 두고 파일 권한으로 막는다.
그만큼 IAM 역할·인스턴스 프로파일도 필요 없어서, 이 배포에 필요한 권한은 EC2 와 RDS 뿐이다.

서명키를 로컬에서 만들어 둔다. 최소 32바이트여야 하고, 저장소의 개발용 기본값을 그대로
쓰면 누구나 토큰을 위조할 수 있다.

```bash
openssl rand -base64 48
```

출력값을 5단계 `env.conf` 의 `APP_JWT_SECRET` 에 넣는다.

---

## 4. EC2 생성

프리티어는 `t3.micro` 750시간/월. Spring Boot + Tomcat 에 2GB 는 빠듯하지만 데모 규모면 돈다.

```bash
# SSH 는 내 IP 만, 앱 포트는 공개
aws ec2 authorize-security-group-ingress --group-id $APP_SG \
  --protocol tcp --port 22 --cidr $MYIP/32
aws ec2 authorize-security-group-ingress --group-id $APP_SG \
  --protocol tcp --port 8080 --cidr 0.0.0.0/0

aws ec2 create-key-pair --key-name treader-prod-key \
  --query KeyMaterial --output text > treader-prod-key.pem
chmod 600 treader-prod-key.pem
```

인스턴스 기동 (Amazon Linux 2023, Java 21 자동 설치). IAM 역할은 붙이지 않는다 —
EC2 가 AWS API 를 호출할 일이 없다.

```bash
AMI=$(aws ssm get-parameter \
  --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
  --query 'Parameter.Value' --output text)

cat > /tmp/userdata.sh <<'SH'
#!/bin/bash
dnf install -y java-21-amazon-corretto-headless
mkdir -p /app/treader /app/uploads /app/logs
chown -R ec2-user:ec2-user /app
SH

INSTANCE_ID=$(aws ec2 run-instances --image-id $AMI --instance-type t3.micro \
  --key-name treader-prod-key --security-group-ids $APP_SG \
  --user-data file:///tmp/userdata.sh \
  --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3}' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=treader-app-prod}]' \
  --query 'Instances[0].InstanceId' --output text)

aws ec2 wait instance-running --instance-ids $INSTANCE_ID
EC2_IP=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
echo "EC2_IP=$EC2_IP"
```

---

## 5. 애플리케이션 배포

로컬에서 빌드한다. **프론트엔드를 먼저** 빌드해야 WAR 에 최신 화면이 들어간다.

```bash
cd frontend-react && npm ci && npm run build && cd ..
./gradlew clean bootWar -x test

unzip -l build/libs/treader.war | grep 'static/index.html'   # 포함 확인
```

전송:

```bash
scp -i treader-prod-key.pem build/libs/treader.war ec2-user@$EC2_IP:/app/treader/
```

EC2 에 접속해 환경 파일을 만든다.

```bash
ssh -i treader-prod-key.pem ec2-user@$EC2_IP
```

`/app/treader/env.conf` 를 만든다. `<...>` 자리는 실제 값으로 채운다.

```bash
cat > /app/treader/env.conf <<'CONF'
export SPRING_DATASOURCE_URL='jdbc:mariadb://<RDS_HOST>:3306/treader_db'
export SPRING_DATASOURCE_USERNAME=treader_user
export SPRING_DATASOURCE_PASSWORD='<DB_비밀번호>'
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
export SPRING_JPA_HIBERNATE_DDL_AUTO=validate
export SPRING_JPA_SHOW_SQL=false

export APP_JWT_SECRET='<3단계에서 만든 값>'
export APP_FRONTEND_BASE_URL=http://<EC2_IP>:8080
export APP_UPLOAD_DIR=/app/uploads

# API 문서는 닫는다. 켜 두면 전체 엔드포인트 구조가 인증 없이 노출된다.
export SPRINGDOC_API_DOCS_ENABLED=false
export SPRINGDOC_SWAGGER_UI_ENABLED=false
CONF

# 비밀번호와 서명키가 평문으로 들어 있다. 소유자만 읽게 막는다.
chmod 600 /app/treader/env.conf
```

systemd 유닛을 올린다. 저장소 루트의 `treader.service` 를 로컬에서 전송한 뒤 설치한다.

```bash
# 로컬에서
scp -i treader-prod-key.pem treader.service ec2-user@$EC2_IP:/tmp/

# EC2 에서
sudo mv /tmp/treader.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now treader
sudo systemctl status treader
```

> 메일 인증(회원가입)을 쓰려면 SES 를 붙여야 한다. SES 는 처음에 샌드박스 상태라
> 검증한 주소로만 보낼 수 있고 해제 신청이 필요하다. 데모라면 `admin` 계정으로
> 로그인해 쓰고 메일은 나중에 붙여도 된다.

---

## 6. 검증

EC2 안에서:

```bash
curl -s localhost:8080/api/health
sudo journalctl -u treader -n 30 --no-pager
```

로컬에서:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://$EC2_IP:8080/api/health   # 200
curl -s -o /dev/null -w "%{http_code}\n" http://$EC2_IP:8080/             # 200 (React)
curl -s -o /dev/null -w "%{http_code}\n" http://$EC2_IP:8080/v3/api-docs  # 404 (차단 확인)

TOKEN=$(curl -s -X POST http://$EC2_IP:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginId":"admin","password":"password"}' \
  | grep -oE '"accessToken":"[^"]+"' | cut -d'"' -f4)

curl -s -H "Authorization: Bearer $TOKEN" \
  http://$EC2_IP:8080/api/statistics/dashboard-summary
```

브라우저로 `http://<EC2_IP>:8080` 에 접속해 로그인(`admin` / `password`)과 대시보드를 확인한다.
**샘플 데이터의 기본 비밀번호는 즉시 바꿀 것.**

---

## 7. 정리 (다 쓰고 나서)

과금을 멈추려면 반드시 지운다. RDS 는 삭제해도 스냅샷이 남아 과금될 수 있다.

```bash
aws ec2 terminate-instances --instance-ids $INSTANCE_ID
aws rds delete-db-instance --db-instance-identifier treader-mariadb-prod \
  --skip-final-snapshot --delete-automated-backups
```

---

## 나중에 붙일 것

| 항목 | 지금 | 붙이려면 |
|---|---|---|
| HTTPS | 없음 (평문 HTTP) | 도메인 확보 → ACM 인증서 → ALB 또는 EC2 에 nginx + Let's Encrypt |
| 80 포트 | 8080 을 직접 노출 | `sudo firewall-cmd` 또는 iptables 로 80→8080 리다이렉트 |
| 이메일 | 미설정 | SES 검증·샌드박스 해제 후 `SPRING_MAIL_*` 주입 |
| 업로드 파일 | EC2 로컬 디스크 | 인스턴스를 늘릴 거면 S3 로 이전 |
| 이중화 | 없음 (단일 EC2) | ALB + Auto Scaling, RDS Multi-AZ |
