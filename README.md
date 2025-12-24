# LoopPak E-Commerce Backend

Spring Boot 기반의 멀티 모듈 이커머스 백엔드 애플리케이션입니다.

## 프로젝트 개요

10주간의 BE L2 과정을 통해 단순 CRUD를 넘어 실제 서비스에서 마주치는 문제들을 단계적으로 해결하며 구축한 이커머스 백엔드 시스템입니다.

**주요 기능**: 상품 조회, 사용자 관리, 포인트/쿠폰, 주문/결제, 좋아요, 실시간 랭킹 시스템

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.4.4, Spring Batch |
| **Database** | MySQL, Redis (Master/Replica) |
| **Messaging** | Apache Kafka |
| **ORM** | Spring Data JPA, QueryDSL |
| **Resilience** | Resilience4j (Circuit Breaker, Retry) |
| **Testing** | JUnit 5, Testcontainers, SpringMockK |
| **Build** | Gradle (Multi-module) |

## 아키텍처

### 멀티 모듈 구조

```
Root
├── apps/                          # 실행 가능한 Spring Boot 애플리케이션
│   ├── commerce-api              # REST API 서버 (메인)
│   ├── commerce-collector        # Kafka 이벤트 컨슈머 (메트릭/랭킹 처리)
│   ├── commerce-batch            # Spring Batch Job (주간/월간 랭킹 집계)
│   └── pg-simulator              # 결제 게이트웨이 시뮬레이터
│
├── modules/                       # 재사용 가능한 인프라 설정
│   ├── jpa                       # JPA, QueryDSL, DataSource 설정
│   ├── redis                     # Redis 설정 (Master/Replica)
│   ├── kafka                     # Kafka Producer/Consumer 설정
│   └── feign                     # Feign Client + Resilience4j 설정
│
└── supports/                      # 부가 기능 애드온
    ├── jackson                   # JSON 직렬화 설정
    ├── logging                   # Logback + Slack Appender
    └── monitoring                # Actuator + Prometheus 메트릭
```

### 레이어드 아키텍처 (DDD 기반)

```
com.loopers/
├── interfaces/        # 표현 계층 (REST Controller, DTO, API Spec)
├── application/       # 응용 계층 (Facade, QueryService)
├── domain/            # 도메인 계층 (Entity, Service, Repository Interface)
├── infrastructure/    # 인프라 계층 (Repository 구현체, 외부 Client)
└── support/           # 지원 유틸리티 (예외 처리, 에러 타입)
```

### 이벤트 주도 아키텍처

```
[commerce-api] → Kafka → [commerce-collector] → Redis ZSET (실시간 랭킹)
                                              → MySQL (product_metrics)

[commerce-batch] → product_metrics → mv_product_rank_weekly/monthly (Materialized View)
```

## 프로젝트 진행 이력

### Round 1-3: 도메인 모델링 및 기본 구현
- 요구사항 명세서 작성
- ERD, 클래스 다이어그램, 시퀀스 다이어그램 설계
- 도메인 엔티티 및 서비스 구현 (User, Product, Order, Payment, Coupon, Point, Like)
- 레이어드 아키텍처 적용 (Facade/QueryService 패턴)

> **관련 소스**
> - [도메인 엔티티](apps/commerce-api/src/main/java/com/loopers/domain/) - Product, Order, Payment, User, Coupon 등
> - [BaseEntity](modules/jpa/src/main/java/com/loopers/domain/BaseEntity.java) - 공통 엔티티 (생성/수정 시간)

### Round 4: 동시성 제어
- 동시성 문제 분석 및 테스트 작성
- 비관적/낙관적 락 적용
- 포인트 차감, 재고 차감, 쿠폰 발급 동시성 처리

> **관련 소스**
> - [OrderConcurrencyTest](apps/commerce-api/src/test/java/com/loopers/application/order/OrderConcurrencyTest.java) - 동시성 테스트
> - [OrderFacade](apps/commerce-api/src/main/java/com/loopers/application/order/OrderFacade.java) - 주문 오케스트레이션
> - [ProductService](apps/commerce-api/src/main/java/com/loopers/domain/product/ProductService.java) - 재고 동시성 처리

### Round 5: 성능 최적화
- 인덱스 설계 및 적용
- Redis 캐싱 도입 (상품 조회)
- Master/Replica 구조 적용

> **관련 소스**
> - [ProductCacheRepository](apps/commerce-api/src/main/java/com/loopers/domain/product/ProductCacheRepository.java) - 캐시 저장소 인터페이스
> - [ProductCacheRepositoryImpl](apps/commerce-api/src/main/java/com/loopers/infrastructure/product/ProductCacheRepositoryImpl.java) - Redis 캐시 구현
> - [RedisConfig](modules/redis/src/main/java/com/loopers/config/redis/RedisConfig.java) - Master/Replica 설정

### Round 6: 외부 시스템 연동
- PG(Payment Gateway) 시뮬레이터 연동
- Feign Client를 통한 HTTP 통신
- Resilience4j 적용 (Circuit Breaker, Retry, Timeout)
- 장애 복구 및 지연 대응 전략 구현

> **관련 소스**
> - [pg-simulator](apps/pg-simulator/) - PG 시뮬레이터 애플리케이션
> - [PgClient](apps/commerce-api/src/main/java/com/loopers/infrastructure/pg/PgClient.java) - Feign 클라이언트
> - [ResilienceConfig](apps/commerce-api/src/main/java/com/loopers/config/ResilienceConfig.java) - Resilience4j 설정
> - [FeignResilienceConfig](modules/feign/src/main/java/com/loopers/config/feign/FeignResilienceConfig.java) - Feign Resilience 설정

### Round 7: 이벤트 기반 디커플링
- ApplicationEvent를 활용한 트랜잭션 분리
- 주문-결제-쿠폰 처리 이벤트 기반 분리
- 좋아요-집계 분리 (Eventual Consistency)
- `@TransactionalEventListener`, `@Async` 활용

> **관련 소스**
> - [OrderCreatedEvent](apps/commerce-api/src/main/java/com/loopers/application/order/event/OrderCreatedEvent.java) - 주문 생성 이벤트
> - [OrderEventHandler](apps/commerce-api/src/main/java/com/loopers/application/order/event/OrderEventHandler.java) - 주문 이벤트 핸들러
> - [LikeEventHandler](apps/commerce-api/src/main/java/com/loopers/application/like/event/LikeEventHandler.java) - 좋아요 이벤트 핸들러
> - [DataPlatformEventHandler](apps/commerce-api/src/main/java/com/loopers/application/dataplatform/event/DataPlatformEventHandler.java) - 데이터 플랫폼 이벤트

### Round 8: Kafka 이벤트 파이프라인
- commerce-collector 모듈 구축
- Kafka Producer (At Least Once 보장)
- Kafka Consumer (멱등 처리)
- 감사 로그(Audit Log), 캐시 무효화, 메트릭 집계 구현
- `product_metrics` 테이블 적재

> **관련 소스**
> - [commerce-collector](apps/commerce-collector/) - Kafka 컨슈머 애플리케이션
> - [OrderEventConsumer](apps/commerce-collector/src/main/java/com/loopers/interfaces/OrderEventConsumer.java) - 주문 이벤트 컨슈머
> - [RankingEventConsumer](apps/commerce-collector/src/main/java/com/loopers/interfaces/RankingEventConsumer.java) - 랭킹 이벤트 컨슈머
> - [ProductMetrics](apps/commerce-collector/src/main/java/com/loopers/domain/metrics/ProductMetrics.java) - 상품 메트릭 엔티티
> - [KafkaConfig](modules/kafka/src/main/java/com/loopers/config/kafka/KafkaConfig.java) - Kafka 설정

### Round 9: Redis ZSET 기반 실시간 랭킹
- Redis Sorted Set을 활용한 일간 랭킹 시스템
- 가중치 기반 점수 계산 (조회: 0.1, 좋아요: 0.2, 주문: 0.7)
- 일간 키 전략 및 TTL 관리
- 랭킹 API 구현 (`GET /api/v1/rankings`)

> **관련 소스**
> - [RankingRepository](apps/commerce-api/src/main/java/com/loopers/domain/ranking/RankingRepository.java) - 랭킹 저장소 인터페이스
> - [RankingRepositoryImpl](apps/commerce-api/src/main/java/com/loopers/infrastructure/ranking/RankingRepositoryImpl.java) - Redis ZSET 구현
> - [RankingQueryService](apps/commerce-api/src/main/java/com/loopers/application/ranking/RankingQueryService.java) - 랭킹 조회 서비스
> - [RankingV1Controller](apps/commerce-api/src/main/java/com/loopers/interfaces/api/ranking/RankingV1Controller.java) - 랭킹 API
> - [RankingEventHandler](apps/commerce-collector/src/main/java/com/loopers/application/eventhandler/RankingEventHandler.java) - ZSET 업데이트 핸들러

### Round 10: Spring Batch 기반 주간/월간 랭킹
- commerce-batch 모듈 구축
- Chunk-Oriented Processing (Reader → Processor → Writer)
- 주간/월간 Materialized View 설계
  - `mv_product_rank_weekly`: TOP 100 주간 랭킹
  - `mv_product_rank_monthly`: TOP 100 월간 랭킹
- 스케줄러를 통한 자동 배치 실행
- 랭킹 API 확장 (일간/주간/월간)

> **관련 소스**
> - [commerce-batch](apps/commerce-batch/) - Spring Batch 애플리케이션
> - [WeeklyRankingJobConfig](apps/commerce-batch/src/main/java/com/loopers/application/batch/config/WeeklyRankingJobConfig.java) - 주간 랭킹 Job
> - [MonthlyRankingJobConfig](apps/commerce-batch/src/main/java/com/loopers/application/batch/config/MonthlyRankingJobConfig.java) - 월간 랭킹 Job
> - [ProductMetricsReader](apps/commerce-batch/src/main/java/com/loopers/application/batch/reader/ProductMetricsReader.java) - ItemReader
> - [WeeklyRankingProcessor](apps/commerce-batch/src/main/java/com/loopers/application/batch/processor/WeeklyRankingProcessor.java) - ItemProcessor
> - [WeeklyRankingWriter](apps/commerce-batch/src/main/java/com/loopers/application/batch/writer/WeeklyRankingWriter.java) - ItemWriter
> - [WeeklyProductRanking](apps/commerce-batch/src/main/java/com/loopers/domain/ranking/WeeklyProductRanking.java) - 주간 MV 엔티티
> - [RankingBatchScheduler](apps/commerce-batch/src/main/java/com/loopers/application/batch/scheduler/RankingBatchScheduler.java) - 배치 스케줄러

## 빠른 시작

### 환경 설정

```bash
# 인프라 실행 (MySQL, Redis, Kafka)
docker-compose -f ./docker/infra-compose.yml up

# 모니터링 실행 (Prometheus, Grafana)
docker-compose -f ./docker/monitoring-compose.yml up
```

### 빌드 및 테스트

```bash
# 전체 빌드
./gradlew build

# 전체 테스트 실행
./gradlew clean test

# 특정 모듈 테스트
./gradlew :apps:commerce-api:test
./gradlew :apps:commerce-batch:test
./gradlew :apps:commerce-collector:test
```

### 애플리케이션 실행

```bash
# commerce-api 실행 (메인 REST API)
./gradlew :apps:commerce-api:bootRun

# commerce-collector 실행 (Kafka 컨슈머)
./gradlew :apps:commerce-collector:bootRun

# commerce-batch 특정 Job 실행
./gradlew :apps:commerce-batch:bootRun --args="--job.name=weeklyRankingJob"
./gradlew :apps:commerce-batch:bootRun --args="--job.name=monthlyRankingJob"
```

## API 엔드포인트

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/v1/users` | 회원가입 |
| GET | `/api/v1/users/me` | 내 정보 조회 |
| POST | `/api/v1/points/charge` | 포인트 충전 |
| GET | `/api/v1/points` | 보유 포인트 조회 |
| GET | `/api/v1/products` | 상품 목록 조회 |
| GET | `/api/v1/products/{productId}` | 상품 상세 조회 |
| POST | `/api/v1/like/products/{productId}` | 상품 좋아요 등록 |
| DELETE | `/api/v1/like/products/{productId}` | 상품 좋아요 취소 |
| POST | `/api/v1/orders` | 주문 요청 |
| GET | `/api/v1/orders` | 주문 목록 조회 |
| GET | `/api/v1/rankings` | 랭킹 조회 (일간/주간/월간) |

> 모든 API는 `X-USER-ID` 헤더로 사용자를 식별합니다.

## 인프라 접속 정보

| 서비스 | 주소 | 비고 |
|--------|------|------|
| MySQL | localhost:3306 | user: application, db: loopers |
| Redis Master | localhost:6379 | 쓰기용 |
| Redis Replica | localhost:6380 | 읽기용 |
| Kafka | localhost:19092 | - |
| Kafka UI | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin |

## 테스트 전략

- **단위 테스트**: 순수 도메인 로직 검증 (Spring 컨텍스트 없음)
- **통합 테스트**: `*IntegrationTest` - Testcontainers 활용
- **E2E 테스트**: `*E2ETest` - 전체 HTTP API 테스트

## 라이선스

이 프로젝트는 Loopers BE L2 과정의 학습용 프로젝트입니다.
