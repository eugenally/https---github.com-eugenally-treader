#!/bin/bash
# TreaderAPP 프론트엔드 배포 스크립트
# 사용법: ./deploy-frontend.sh

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 설정
FRONTEND_DIR="frontend-react"
S3_BUCKET="treader-frontend-prod"
DISTRIBUTION_ID=""  # CloudFront Distribution ID
AWS_REGION="us-east-1"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}TreaderAPP Frontend Deployment Script${NC}"
echo -e "${YELLOW}========================================${NC}"

# 1. 프론트엔드 디렉토리 확인
if [ ! -d "$FRONTEND_DIR" ]; then
    echo -e "${RED}Error: $FRONTEND_DIR directory not found${NC}"
    exit 1
fi

# 2. 의존성 설치
echo -e "${YELLOW}[1/5] Installing dependencies...${NC}"
cd "$FRONTEND_DIR"
npm install

# 3. 빌드
echo -e "${YELLOW}[2/5] Building React application...${NC}"
npm run build

if [ ! -d "dist" ]; then
    echo -e "${RED}Error: Build failed, dist directory not found${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Build successful${NC}"

# 4. S3 배포
echo -e "${YELLOW}[3/5] Deploying to S3 bucket: $S3_BUCKET${NC}"
aws s3 sync dist/ s3://$S3_BUCKET/ \
    --delete \
    --region $AWS_REGION \
    --exclude ".git/*" \
    --exclude "node_modules/*"

echo -e "${GREEN}✓ S3 upload successful${NC}"

# 5. CloudFront 캐시 무효화
if [ -z "$DISTRIBUTION_ID" ]; then
    echo -e "${YELLOW}[4/5] CloudFront Distribution ID not set${NC}"
    echo -e "${YELLOW}       Please set DISTRIBUTION_ID in the script${NC}"
else
    echo -e "${YELLOW}[4/5] Invalidating CloudFront cache...${NC}"
    aws cloudfront create-invalidation \
        --distribution-id $DISTRIBUTION_ID \
        --paths "/*" \
        --region $AWS_REGION

    echo -e "${GREEN}✓ CloudFront cache invalidated${NC}"
fi

# 완료
echo -e "${YELLOW}[5/5] Deployment complete${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Frontend deployment successful!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "CloudFront Distribution: https://d1234567890.cloudfront.net"
echo "Custom Domain: https://yourdomain.com"
echo ""
