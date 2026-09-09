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
AWS_REGION="ap-northeast-2"
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

# 1. 프론트엔드 → src/main/resources/static, 그다음 WAR
# Vite outDir 이 WAR 리소스 경로라 순서를 지켜야 최신 화면이 담긴다.
echo -e "${YELLOW}[1/5] Building frontend and WAR...${NC}"
(cd frontend-react && npm ci && npm run build)

if [ ! -f "src/main/resources/static/index.html" ]; then
    echo -e "${RED}Error: frontend build produced no static/index.html${NC}"
    exit 1
fi

./gradlew clean bootWar -x test

if [ ! -f "$BUILD_OUTPUT" ]; then
    echo -e "${RED}Error: Build failed, WAR file not found: $BUILD_OUTPUT${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Build successful: $BUILD_OUTPUT${NC}"

# 2. 직전 WAR 백업 — 새 파일을 올리기 전에 해야 롤백본이 남는다
echo -e "${YELLOW}[2/5] Backing up current WAR on EC2...${NC}"
ssh -i "$PEM_KEY" "$EC2_USER@$EC2_IP" \
    "cd $EC2_REMOTE_DIR && [ -f $WAR_NAME ] && cp $WAR_NAME $WAR_NAME.backup.\$(date +%s) && echo 'backup created' || echo 'no previous WAR'"

# 3. 전송 후 재기동
echo -e "${YELLOW}[3/5] Uploading WAR and restarting...${NC}"
scp -i "$PEM_KEY" "$BUILD_OUTPUT" "$EC2_USER@$EC2_IP:$EC2_REMOTE_DIR/$WAR_NAME"

ssh -i "$PEM_KEY" "$EC2_USER@$EC2_IP" << EOF
set -e
sudo systemctl restart $SYSTEMD_SERVICE
sleep 5
sudo systemctl status $SYSTEMD_SERVICE --no-pager
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
