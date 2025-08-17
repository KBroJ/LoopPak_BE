# Commerce API 성능 테스트

k6를 사용한 Commerce API의 성능 테스트 환경입니다.

## 구조

```
qa/k6/
├── k6
│   ├── scripts/xxx/    # 테스트 스크립트 디렉토리
│   │   └── smoke-test.js  # 스모크 테스트 스크립트
│   └── run-tests.sh    # 테스트 실행 스크립트
├── sample
│   ├── data/          # 샘플 데이터 (CSV/JSON)
│   ├── results/       # 동적 쿼리 저장 디렉토리
│   │   └── sample-data.sql     # 동적 쿼리
│   ├── scripts/       # SQL 생성 스크립트
│   │   └── generate-sql.js  # CSV/JSON → SQL 생성 스크립트
│   └── insert-sample-data.sh # 전체 동작 스크립트
└── README.md
```

## 사전 요구사항

- Docker & Docker Compose
- k6 (선택사항, Docker 없이 실행하려면)

## 사용법

### 1. 전체 워크플로우

```bash
# 1단계: 샘플 데이터 준비 및 삽입
node scripts/generate-sql.js          # CSV/JSON → SQL 생성
./insert-sample-data.sh               # 데이터베이스에 직접 삽입

# 2단계: 모니터링 환경 시작 (프로젝트 루트에서)
cd ../../docker
docker-compose -f monitoring-compose.yml up -d

# 3단계: 성능 테스트 실행
cd ../performance-tests/k6
k6 run --env BASE_URL=http://localhost:8080 scripts/xxx/smoke-test.js
k6 run --env BASE_URL=http://localhost:8080 scripts/xxx/load-test.js

# 4단계: 결과 확인
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
# InfluxDB: http://localhost:8086
```

### 2. 샘플 데이터 삽입 (직접 데이터베이스 삽입)

샘플 데이터를 데이터베이스에 직접 삽입합니다:

```bash
# 1. SQL 파일 생성
node scripts/generate-sql.js

# 2. 데이터베이스에 직접 삽입
./insert-sample-data.sh

# 3. 삽입된 데이터 확인
# - 사용자: 10명
# - 브랜드: 1000개  
# - 상품: 1000개
```

### 2. Docker 환경으로 실행 (권장)

```bash
# 1. 모니터링 환경 시작 (프로젝트 루트에서)
cd ../../docker
docker-compose -f monitoring-compose.yml up -d

# 2. 테스트 실행
cd ../performance-tests/k6
./run-tests.sh

# 3. 결과 확인
# Grafana: http://localhost:3000 (admin/admin) - 통합 모니터링
# Prometheus: http://localhost:9090 - 애플리케이션 메트릭
# InfluxDB: http://localhost:8086 - k6 성능 테스트 결과
```

### 3. 로컬 k6로 실행

```bash
# 1. k6 설치 (macOS)
brew install k6

# 2. 테스트 실행
cd performance-tests/k6
./run-tests.sh

# 또는 개별 테스트 실행
k6 run --env BASE_URL=http://localhost:8080 scripts/load-test.js
```

## 테스트 종류

### 1. 데이터 설정 (generate-sql.js + insert-sample-data.sh)
- 사용자, 브랜드, 상품 샘플 데이터를 SQL로 생성하고 데이터베이스에 직접 삽입
- 성능 테스트 전에 한 번만 실행
- API 상태와 무관하게 안정적으로 데이터 삽입

### 2. 스모크 테스트 (smoke-test.js)
- 기본 기능 확인
- 1명의 사용자로 1분간 실행
- 응답 시간 < 500ms

### 3. 부하 테스트 (load-test.js)
- 정상 부하 상황 테스트
- 2분간 10명까지 증가 → 5분간 유지 → 2분간 감소
- 응답 시간 < 500ms

## 환경 변수

- `BASE_URL`: 테스트 대상 API URL (기본값: http://localhost:8080)

## 결과 확인

### Grafana 대시보드
- URL: http://localhost:3000
- 계정: admin/admin
- k6 메트릭 대시보드 제공

### k6 콘솔 출력
- 요청 성공/실패율
- 응답 시간 통계
- 에러율

## 커스터마이징

### 테스트 시나리오 수정
- `scripts/` 디렉토리의 각 테스트 파일 수정
- API 엔드포인트, 요청 데이터, 검증 로직 변경

### 부하 설정 수정
- `options` 객체의 `stages` 수정
- 가상 사용자 수, 테스트 시간 조정

### 임계값 설정
- `thresholds` 객체에서 성능 기준 설정
- 응답 시간, 에러율 등 조정 