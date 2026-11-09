# barofit-report

**barofit** 백엔드의 리포트 서비스 레포입니다. 원래 `backend_baro` 모노레포의
`services/report` 모듈을 폴리레포 전환 과정에서 분리했습니다(커밋 이력 보존).

자세 판정 리포트, 월간 일지, 운동별 점수·피드백을 제공합니다. `barofit-workout`(세션 소유권 확인),
`barofit-routine`(루틴 정보)을 동기 REST로 호출합니다.

## 기술 스택

- Java 21 / Gradle (단일 모듈, 독립 레포)
- Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- PostgreSQL `report` 스키마(`currentSchema=report`), Flyway

## 의존성

[barofit-common](https://github.com/Barofit-team/barofit-common)의 `barofit-runtime`을 참조합니다.
먼저 그 레포에서 `./gradlew publishToMavenLocal`을 실행해둬야 이 레포가 빌드됩니다.

`WORKOUT_BASE_URL`, `ROUTINE_BASE_URL` 환경변수로 다른 서비스를 호출합니다.

## 로컬 실행

```bash
cp .env.example .env   # 레포에 .env.example이 없다면 barofit-infra의 것을 참고
./gradlew bootRun       # 기본 포트 3008
```

## 테스트 / 빌드

```bash
./gradlew test    # Docker 필요(Testcontainers)
./gradlew build   # build/libs/app.jar
```

## Docker 이미지

```bash
DOCKERHUB_USER=<hub-user> ./scripts/build-and-push.sh
```

## 배포

로컬 minikube 배포는 [barofit-infra](https://github.com/Barofit-team/barofit-infra)의
`infra/charts/report` Helm 차트로 합니다.

## API 요약

- `GET /api/v1/reports/*` — 자세 판정 리포트, 월간 일지, 점수·피드백 조회 (JWT 필요)
- Swagger UI: `/swagger-ui.html`, OpenAPI: `/v3/api-docs`

## 관련 레포

- [barofit-common](https://github.com/Barofit-team/barofit-common) — 공유 라이브러리(이 레포가 의존)
- [barofit-identity](https://github.com/Barofit-team/barofit-identity) · [barofit-routine](https://github.com/Barofit-team/barofit-routine) · [barofit-workout](https://github.com/Barofit-team/barofit-workout) · [barofit-analytics](https://github.com/Barofit-team/barofit-analytics)
- [barofit-infra](https://github.com/Barofit-team/barofit-infra) — Helm 차트, 로컬 검증 스크립트(`verify-report.cjs`)
- [backend_baro](https://github.com/Barofit-team/backend_baro) — 분리 이전 모노레포(참고용)
