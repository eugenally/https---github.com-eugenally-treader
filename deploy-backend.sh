#!/bin/bash
# TreaderAPP 백엔드 배포 스크립트
# 사용법: ./deploy-backend.sh [ec2-ip] [pem-key-path]

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 설정
WAR_NAME="treader.war"
BUILD_OUTPUT="build/libs/$WAR_NAME"
S3_DEPLOY_BUCKET="treader-deploy-artifacts"
AWS_REGION="us-east-1"
EC2_USER="ec2-user"
EC2_REMOTE_DIR="/app/treader"
SYSTEMD_SERVICE="treader"

# 인자 확인
if [ $# -lt 2 ]; then
    echo -e "${RED}Usage: ./deploy-backend.sh <EC2_IP> <PEM_KEY_PATH>${NC}"
    echo ""
    echo "Example:"
    echo "  ./deploy-backend.sh 54.123.45.67 /path/to/treader-prod-key.pem"
    exit 1
fi

EC2_IP=$1
PEM_KEY=$2

# PEM 파일 확인
if [ ! -f "$PEM_KEY" ]; then
    echo -e "${RED}Error: PEM key file not found: $PEM_KEY${NC}"
    exit 1
fi

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}TreaderAPP Backend Deployment Script${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "Target EC2: $EC2_IP"
echo "PEM Key: $PEM_KEY"
echo ""

# 1. Gradle 빌드
echo -e "${YELLOW}[1/5] Building WAR file...${NC}"
./gradlew clean bootWar -x test

if [ ! -f "$BUILD_OUTPUT" ]; then
    echo -e "${RED}Error: Build failed, WAR file not found: $BUILD_OUTPUT${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Build successful: $BUILD_OUTPUT${NC}"

# 2. S3에 업로드
echo -e "${YELLOW}[2/5] Uploading WAR to S3...${NC}"
aws s3 cp "$BUILD_OUTPUT" "s3://$S3_DEPLOY_BUCKET/$WAR_NAME" \
    --region $AWS_REGION

echo -e "${GREEN}✓ S3 upload successful${NC}"

# 3. EC2에 다운로드 및 배포
echo -e "${YELLOW}[3/5] Connecting to EC2 and deploying...${NC}"

ssh -i "$PEM_KEY" "$EC2_USER@$EC2_IP" << 'EOF'
set -e

echo "Downloading WAR file from S3..."
cd /app/treader
aws s3 cp s3://treader-deploy-artifacts/treader.war . --region us-east-1

echo "Backing up previous WAR..."
if [ -f treader.war ]; then
    cp treader.war treader.war.backup.$(date +%s)
    echo "Backup created"
fi

echo "Restarting service..."
sudo systemctl restart treader

echo "Waiting for service to start..."
sleep 5

echo "Checking service status..."
sudo systemctl status treader

EOF

echo -e "${GREEN}✓ EC2 deployment successful${NC}"

# 4. 배포 후 검증
echo -e "${YELLOW}[4/5] Verifying deployment...${NC}"

# EC2 애플리케이션 상태 확인
if ssh -i "$PEM_KEY" "$EC2_USER@$EC2_IP" "curl -s http://localhost:8080/swagger-ui.html > /dev/null"; then
    echo -e "${GREEN}✓ Application is running${NC}"
else
    echo -e "${YELLOW}⚠ Warning: Could not verify application status${NC}"
fi

# 5. 로그 확인
echo -e "${YELLOW}[5/5] Checking application logs...${NC}"
ssh -i "$PEM_KEY" "$EC2_USER@$EC2_IP" "sudo journalctl -u $SYSTEMD_SERVICE -n 20 --no-pager"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Backend deployment successful!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Next steps:"
echo "  1. Check application logs for any errors"
echo "  2. Verify database connectivity"
echo "  3. Test API endpoints"
echo ""
echo "Useful commands:"
echo "  Check service status:"
echo "    ssh -i $PEM_KEY $EC2_USER@$EC2_IP 'sudo systemctl status treader'"
echo "  View logs (live):"
echo "    ssh -i $PEM_KEY $EC2_USER@$EC2_IP 'sudo journalctl -u treader -f'"
echo "  Restart service:"
echo "    ssh -i $PEM_KEY $EC2_USER@$EC2_IP 'sudo systemctl restart treader'"
echo ""
