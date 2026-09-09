# AWS 배포 가이드 - TreaderAPP (MariaDB)

## 📋 목차
1. [사전 준비](#사전-준비)
2. [AWS 인프라 구성](#aws-인프라-구성)
3. [데이터베이스 마이그레이션](#데이터베이스-마이그레이션)
4. [EC2 배포](#ec2-배포)
5. [프론트엔드 배포](#프론트엔드-배포)
6. [검증 및 모니터링](#검증-및-모니터링)

---

## 사전 준비

### 필수 요구사항
- AWS 계정 (IAM 권한: EC2, RDS, S3, CloudFront, CloudWatch)
- AWS CLI v2 설치 및 구성
- Gradle 및 Java 21
- Node.js 18+
- MySQL 클라이언트 (`mysql` 명령어)

### AWS CLI 설정
```bash
aws configure
# AWS Access Key ID: ***
# AWS Secret Access Key: ***
# Default region: us-east-1
# Default output: json
```

---

## AWS 인프라 구성

### 1단계: RDS MariaDB 인스턴스 생성

#### AWS 콘솔 또는 CLI로 생성

**CLI 예시**:
```bash
aws rds create-db-instance \
  --db-instance-identifier treader-mariadb-prod \
  --db-instance-class db.t4g.micro \
  --engine mariadb \
  --engine-version 10.6 \
  --master-username admin \
  --master-user-password '<YOUR_STRONG_PASSWORD>' \
  --allocated-storage 20 \
  --storage-type gp3 \
  --backup-retention-period 7 \
  --multi-az false \
  --publicly-accessible false \
  --vpc-security-group-ids sg-xxxxxxxxx \
  --db-parameter-group-name treader-mariadb-params \
  --region us-east-1
```

#### RDS 파라미터 그룹 설정
1. AWS RDS 콘솔 → Parameter Groups
2. 새로운 파라미터 그룹 생성 (`treader-mariadb-params`)
3. 다음 파라미터 설정:
   ```
   character_set_server: utf8mb4
   collation_server: utf8mb4_unicode_ci
   max_connections: 100
   max_allowed_packet: 67108864 (64MB)
   slow_query_log: 1
   long_query_time: 2
   ```

#### 보안 그룹 설정
1. AWS EC2 콘솔 → Security Groups
2. RDS용 보안 그룹 생성 (`treader-rds-sg`)
3. **인바운드 규칙**:
   - 타입: MySQL/Aurora (3306)
   - 소스: EC2 보안 그룹 ID (예: `sg-app-xxxxxxxxx`)

### 2단계: EC2 인스턴스 생성

#### AMI 선택
- **Amazon Linux 2023** 또는 **Ubuntu 22.04 LTS**
- 최소 무료 티어: `t3.small` (2vCPU, 2GB RAM)

#### EC2 인스턴스 생성 (AWS 콘솔)
1. EC2 Dashboard → Instances → Launch Instance
2. 이름: `treader-app-prod`
3. AMI: Amazon Linux 2023
4. 인스턴스 타입: `t3.small`
5. 키 페어: 새로 생성 (`treader-prod-key`) → PEM 파일 다운로드
6. 네트워크:
   - VPC: 기본 VPC
   - 서브넷: 공개 서브넷
   - 자동 공개 IP 할당: 활성화
7. 보안 그룹: 새로 생성 (`treader-app-sg`)
   - HTTP (80): 0.0.0.0/0
   - HTTPS (443): 0.0.0.0/0
   - SSH (22): 관리자 IP/32 (권장)
8. 스토리지: 30GB gp3
9. IAM 인스턴스 프로필: 아래 IAM 역할 생성 후 선택

#### EC2 IAM 역할 생성
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:us-east-1:ACCOUNT_ID:log-group:/aws/ec2/treader-app:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:treader/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::treader-uploads-prod/*"
    }
  ]
}
```

#### 추가 EBS 볼륨 (업로드 파일 저장)
1. EC2 콘솔 → 인스턴스 → 스토리지
2. 새 볼륨 생성: 50GB gp3
3. `/dev/sdf` 마운트 포인트로 할당

### 3단계: Application Load Balancer (ALB) 설정

#### ALB 생성
1. EC2 → Load Balancers → Create Load Balancer
2. 타입: Application Load Balancer
3. 이름: `treader-alb`
4. 스키마: 인터넷 연결
5. 리스너: HTTP (80) + HTTPS (443)
6. 가용 영역: 최소 2개 선택

#### 대상 그룹 생성
1. EC2 → Target Groups → Create Target Group
2. 이름: `treader-tg`
3. 프로토콜: HTTP, 포트: 8080
4. VPC: 기본 VPC
5. **헬스 체크**:
   - 경로: `/api/health` (인증 불필요. Swagger 는 운영에서 끄므로 헬스체크로 쓰지 않는다)
   - 포트: 8080
   - 간격: 30초
   - 타임아웃: 5초
6. EC2 인스턴스 등록

#### SSL/TLS 인증서 (AWS Certificate Manager)
1. AWS ACM 콘솔 → Request Certificate
2. 도메인: `api.yourdomain.com` (또는 Route53 도메인)
3. DNS 또는 이메일 검증 완료
4. ALB 리스너 HTTPS (443)에 인증서 연결

### 4단계: S3 + CloudFront (프론트엔드)

#### S3 버킷 생성
```bash
aws s3 mb s3://treader-frontend-prod --region us-east-1
aws s3api put-bucket-versioning --bucket treader-frontend-prod --versioning-configuration Status=Enabled
```

#### CloudFront 배포 생성
1. CloudFront 콘솔 → Create Distribution
2. 원본: S3 버킷 (`treader-frontend-prod.s3.us-east-1.amazonaws.com`)
3. **캐시 설정**:
   - Default TTL: 0 (HTML)
   - Maximum TTL: 31536000 (1년, JS/CSS)
4. HTTPS만 허용
5. Gzip 압축 활성화
6. 도메인 연결 (Route53 또는 CNAME)

#### S3 업로드 S3 버킷 (아티팩트 저장)
```bash
aws s3 mb s3://treader-deploy-artifacts --region us-east-1
```

---

## 데이터베이스 마이그레이션

### 1단계: RDS 초기 데이터베이스 생성

#### RDS 엔드포인트 확인
```bash
aws rds describe-db-instances --db-instance-identifier treader-mariadb-prod \
  --query 'DBInstances[0].Endpoint.Address' --output text
```

#### DDL 스크립트 실행
```bash
# RDS에 접속 (EC2 또는 로컬에서)
mysql -h <RDS_ENDPOINT> -u admin -p

# MariaDB 프롬프트에서:
CREATE DATABASE IF NOT EXISTS treader_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'treader_user'@'%' IDENTIFIED BY 'YOUR_PASSWORD';
GRANT ALL PRIVILEGES ON treader_db.* TO 'treader_user'@'%';
FLUSH PRIVILEGES;
EXIT;

# DDL 및 샘플 데이터 로드
mysql -h <RDS_ENDPOINT> -u treader_user -p treader_db < src/main/resources/db/oracle_ddl.sql  # (MariaDB DDL. 파일명은 마이그레이션 이전 이름을 유지)
mysql -h <RDS_ENDPOINT> -u treader_user -p treader_db < src/main/resources/db/sample-data.sql
```

### 2단계: AWS Secrets Manager에 자격증명 저장

```bash
# DB 비밀번호
aws secretsmanager create-secret \
  --name treader/mariadb/password \
  --secret-string 'YOUR_DB_PASSWORD' \
  --region us-east-1

# JWT 서명키
aws secretsmanager create-secret \
  --name treader/jwt-secret \
  --secret-string 'YOUR_SECURE_JWT_SECRET_32BYTES_MINIMUM' \
  --region us-east-1

# AWS SES 자격증명 (선택)
aws secretsmanager create-secret \
  --name treader/ses/username \
  --secret-string 'YOUR_SES_USERNAME' \
  --region us-east-1
```

---

## EC2 배포

### 1단계: EC2 환경 준비

#### SSH 접속
```bash
chmod 600 treader-prod-key.pem
ssh -i treader-prod-key.pem ec2-user@<EC2_PUBLIC_IP>
```

#### OS 업데이트 및 Java 설치
```bash
# Amazon Linux 2023
sudo yum update -y
sudo yum install -y java-21-amazon-corretto-devel git wget curl mysql

# 설치 확인
java -version
```

#### 애플리케이션 디렉토리 준비
```bash
# 앱 홈 디렉토리
sudo mkdir -p /app/treader /app/uploads /app/logs
sudo chown ec2-user:ec2-user /app

# EBS 볼륨 마운트 (추가 스토리지)
lsblk  # 볼륨 확인 (예: /dev/nvme1n1)
sudo mkfs.ext4 /dev/nvme1n1
sudo mount /dev/nvme1n1 /app/uploads
echo '/dev/nvme1n1 /app/uploads ext4 defaults,nofail 0 2' | sudo tee -a /etc/fstab
```

### 2단계: WAR 파일 빌드 및 배포

#### 로컬에서 빌드
```bash
cd /path/to/treader
./gradlew clean bootWar -x test
# 결과: build/libs/treader.war
```

#### S3를 통한 배포
```bash
# 로컬에서 S3 업로드
aws s3 cp build/libs/treader.war s3://treader-deploy-artifacts/treader.war

# EC2에서 다운로드
cd /app/treader
aws s3 cp s3://treader-deploy-artifacts/treader.war .
```

### 3단계: 환경 변수 설정

#### `/app/treader/env.conf` 작성
```bash
export SPRING_DATASOURCE_URL=jdbc:mariadb://<RDS_ENDPOINT>:3306/treader_db?sslMode=trust
export SPRING_DATASOURCE_USERNAME=treader_user
export SPRING_DATASOURCE_PASSWORD=$(aws secretsmanager get-secret-value --secret-id treader/mariadb/password --query SecretString --output text)
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
export SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MariaDBDialect
export SPRING_JPA_HIBERNATE_DDL_AUTO=validate
export SPRING_JPA_SHOW_SQL=false

export APP_JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id treader/jwt-secret --query SecretString --output text)
export APP_FRONTEND_BASE_URL=https://yourdomain.com

export SPRING_MAIL_HOST=smtp.us-east-1.amazonaws.com
export SPRING_MAIL_PORT=587
export SPRING_MAIL_USERNAME=$(aws secretsmanager get-secret-value --secret-id treader/ses/username --query SecretString --output text)
export SPRING_MAIL_PASSWORD=$(aws secretsmanager get-secret-value --secret-id treader/ses/password --query SecretString --output text)
export APP_MAIL_FROM=noreply@yourdomain.com

export APP_UPLOAD_DIR=/app/uploads

# API 문서는 운영에서 닫는다. 켜 두면 /v3/api-docs 로 전체 엔드포인트 구조가
# 인증 없이 노출된다. ALB 헬스체크는 /api/health 를 쓰므로 영향이 없다.
export SPRINGDOC_API_DOCS_ENABLED=false
export SPRINGDOC_SWAGGER_UI_ENABLED=false
```

### 4단계: systemd 서비스 설정

#### `/etc/systemd/system/treader.service` 생성
```ini
[Unit]
Description=TreaderAPP Spring Boot Application
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/app/treader
EnvironmentFile=/app/treader/env.conf
ExecStart=/usr/bin/java -Dserver.port=8080 \
  -Dspring.datasource.url=${SPRING_DATASOURCE_URL} \
  -Dspring.datasource.username=${SPRING_DATASOURCE_USERNAME} \
  -Dspring.datasource.password=${SPRING_DATASOURCE_PASSWORD} \
  -Dapp.jwt.secret=${APP_JWT_SECRET} \
  -Dapp.frontend.base-url=${APP_FRONTEND_BASE_URL} \
  -jar treader.war
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

#### 서비스 활성화 및 시작
```bash
sudo systemctl daemon-reload
sudo systemctl enable treader
sudo systemctl start treader
sudo systemctl status treader

# 로그 확인
sudo journalctl -u treader -f
```

---

## 프론트엔드 배포

### 1단계: React 빌드

```bash
cd frontend-react
npm install
npm run build

# 결과: dist/
```

### 2단계: S3에 배포

```bash
aws s3 sync dist/ s3://treader-frontend-prod/ --delete --region us-east-1

# CloudFront 캐시 무효화
aws cloudfront create-invalidation --distribution-id <DISTRIBUTION_ID> --paths "/*"
```

---

## 검증 및 모니터링

### 1단계: EC2 애플리케이션 상태 확인

```bash
# EC2에서 애플리케이션 상태 확인
curl http://localhost:8080/api/health

# 데이터베이스 연결 확인
curl http://localhost:8080/api/health
# HTTP 200 응답 확인
```

### 2단계: ALB 엔드포인트 테스트

```bash
# ALB DNS 이름으로 접속
curl https://<ALB_DNS>/api/health
# 또는 Route53 도메인: https://api.yourdomain.com/api/health
```

### 3단계: 프론트엔드 배포 확인

```bash
curl https://<CloudFront_Domain>/
# HTML 응답 확인
```

### 4단계: CloudWatch 로깅 설정

```bash
# 로그 그룹 생성
aws logs create-log-group --log-group-name /aws/ec2/treader-app
aws logs put-retention-policy --log-group-name /aws/ec2/treader-app --retention-in-days 30

# CloudWatch Agent 설치 (선택)
wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
sudo rpm -U ./amazon-cloudwatch-agent.rpm
```

### 5단계: 모니터링 알람 설정

```bash
# RDS CPU 사용률 > 80%
aws cloudwatch put-metric-alarm \
  --alarm-name treader-rds-cpu-high \
  --alarm-description "Alert when RDS CPU > 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --dimensions Name=DBInstanceIdentifier,Value=treader-mariadb-prod

# EC2 상태 확인
aws cloudwatch put-metric-alarm \
  --alarm-name treader-ec2-status-check \
  --alarm-description "Alert when EC2 status check fails" \
  --metric-name StatusCheckFailed \
  --namespace AWS/EC2 \
  --statistic Sum \
  --period 300 \
  --threshold 1 \
  --comparison-operator GreaterThanOrEqualToThreshold \
  --evaluation-periods 1 \
  --dimensions Name=InstanceId,Value=i-xxxxxxxxx
```

---

## 트러블슈팅

### MariaDB 연결 실패
```bash
# EC2에서 RDS 연결 테스트
mysql -h <RDS_ENDPOINT> -u treader_user -p treader_db -e "SELECT 1;"

# 보안 그룹 확인
aws ec2 describe-security-groups --group-ids sg-xxxxxxxxx --query 'SecurityGroups[0].IpPermissions'
```

### 애플리케이션 실행 실패
```bash
# systemd 로그 확인
sudo journalctl -u treader -n 50

# 환경 변수 확인
cat /app/treader/env.conf
```

### ALB 헬스 체크 실패
```bash
# ALB 대상 그룹 상태 확인
aws elbv2 describe-target-health --target-group-arn <TG_ARN> --region us-east-1

# ALB 보안 그룹 확인
# HTTP(80) → HTTPS(443) 리다이렉트 설정 확인
```

---

## 배포 후 체크리스트

- [ ] RDS 자동 백업 활성화 (7일)
- [ ] EC2 보안 그룹: SSH는 특정 IP만 허용
- [ ] Secrets Manager에 모든 민감 정보 저장
- [ ] CloudWatch 로깅 활성화
- [ ] 모니터링 알람 설정
- [ ] Route53 DNS 레코드 생성
- [ ] SSL/TLS 인증서 갱신 자동화
- [ ] 정기적 백업 테스트
- [ ] 성능 모니터링 (CPU, 메모리, 디스크)

---

**작성일**: 2026-09-09  
**버전**: 1.0
