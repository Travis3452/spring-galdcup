# Galdcup (갈드컵) 백엔드

게임·선수·캐릭터처럼 비교하면 의견이 갈리는 주제를 기간제 투표와 게시글·댓글로 다루는 한국어 커뮤니티의 REST·WebSocket 서버입니다. 투표 결과를 기록하고 댓글 맥락을 AI로 참고 분석한다는 기존 기획 의도를 유지합니다. AI 예측이 실제 득표 결과나 전체 여론을 보증하지는 않습니다.

- [프로젝트 설명서·기획 및 설계](https://www.notion.so/3350e2be9aa9802baf55e83fd005ad72)
- [API 명세 및 가이드](https://app.notion.com/p/API-3350e2be9aa9805db346e5138dda67bd)
- 기존 운영 안내 주소: [galdcup.live](https://galdcup.live). 이번 문서 작업에서 운영 서버의 접속·설정 상태는 확인하지 않았습니다.

## 구현된 주요 기능

- Google OAuth 인가 코드 교환, JWT Access/Refresh 용도 검증, Redis Refresh Token 저장·회전, 로그아웃.
- 사용자 조회·닉네임 변경·탈퇴, 서비스 등급 신청과 ADMIN의 승인·거절.
- 주제별 게시판 생성·검색·인기 목록·상세, OPEN/CLOSED 상태, 인기글 추천수 기준, 게시판별 관리자·부관리자·위임·권한 신청.
- 게시글 CRUD, 카테고리·인기글 필터, 제목+본문·작성자 검색, 좋아요·싫어요. 댓글·대댓글 CRUD 및 댓글 논리 삭제.
- 게시판별 기간제 투표, 2~10개 이미지 선택지, 회원별 중복 투표 방지, 수동·자동 마감, 지난 투표 조회.
- 진행 중 선택지별 득표수는 숨기고 총참여자 수를 STOMP로 전송. Redis Lua 집계와 DB 반영·커밋 후 확인 처리를 분리.
- Gemini 투표 주제 추천과 요청 기반 여론 분석. 분석은 게시판의 사용자별 최신 댓글 최대 50개를 조회하고 총 1,500자 이내로 구성한 입력을 사용.
- 개인 Gemini 키와 페르소나로 글 또는 댓글을 생성하는 AI 용병. 사용자당 1개, 생성 시 24시간 유효·30분 간격 설정, Redis 쿨다운 기반 실행. 실제 첫 실행·처리 간격은 스케줄러와 처리 시간에 영향받음.
- Bucket4j의 Redis 기반 IP·사용자별 호출 제한, HTML 정제, 공통 예외 응답, 실행 시간 로그.

현재 REST 매핑은 **56개(일반 55개 + 개발 전용 로그인 1개)**입니다. Spring 제공 Actuator·문서 경로와 WebSocket handshake는 이 수에 포함하지 않았습니다. 상세 요청·응답은 위 API 문서를 확인합니다.

## 기술 스택

| 구분 | 코드에서 확인한 구성 |
| --- | --- |
| 언어·빌드 | Java 17, Gradle Wrapper 9.2.1 |
| 서버 | Spring Boot **4.0.0**, Spring MVC, Spring Security, Validation, OAuth2 Client |
| 영속성 | Spring Data JPA; 기본 Oracle JDBC·OracleDialect, dev MySQL JDBC·MySQLDialect |
| Redis·실시간 | Spring Data Redis, Lettuce, Lua, STOMP 내장 Simple Broker |
| 인증·제어 | JJWT 0.12.6, Bucket4j 8.11.1, OWASP Java HTML Sanitizer |
| AI | Google GenAI SDK 1.0.0; 코드의 모델 식별자 `gemini-3.5-flash-lite` |
| 문서·테스트 | springdoc 2.8.5 선언, JUnit Jupiter·Mockito |
| 클라이언트 | Vue 3·Vite·Pinia·Tailwind; 상세 버전은 프론트엔드 README |
| 이미지 | 프론트엔드에서 Supabase Storage로 직접 업로드 |

Redis 7.2는 CI 테스트 이미지 버전입니다. 운영 DB·Redis 버전과 Gemini 모델의 실제 제공 여부, Boot 4와 선언된 springdoc의 런타임 호환성은 별도 확인이 필요합니다.

## 시스템 구조

```text
브라우저(Vue SPA)
 ├─ REST /api → Security JWT → RateLimit → Controller → Service/Validator
 │                                                    ├─ JPA → Oracle / dev MySQL
 │                                                    ├─ Redis: 인증·캐시·집계·제한
 │                                                    └─ Google OAuth / Gemini
 ├─ STOMP /ws-galdcup ← /topic/votes/{voteSessionId}: 총참여자 수
 └─ Supabase Storage: 이미지 직접 업로드 → 공개 URL을 API에 전달
```

`src/main/java/com/example/galdcup/` 아래에 `auth`, `user`, `board`(게시판·카테고리·글·댓글·투표), `gemini`, `userAiAgent`, `common`을 둡니다. 각 도메인은 Controller·Service·Validator·Repository·DTO로 나뉩니다.

```text
src/main/java/com/example/galdcup/  # 애플리케이션 및 도메인 코드
src/main/resources/
├── application.yml                # 기본 Oracle·외부 설정
├── application-dev.yml            # dev MySQL 및 개발 옵션
└── scripts/                       # 투표 접수·스냅샷·확인 Lua
src/test/java/                     # 단위·Redis·컨텍스트 테스트
.github/workflows/deploy.yml       # 검증 후 JAR 배포
Dockerfile                        # 별도의 다단계 컨테이너 빌드
build.gradle                      # 의존성·테스트 태스크
```

RDB에는 `users`, `role_requests`, `boards`, `board_policies`, `board_manager_requests`, `board_policy_sub_managers`, `post_categories`, `posts`, `comments`, `post_reactions`, `vote_sessions`, `vote_options`, `user_ai_agent` 매핑이 있습니다. 개별 투표 기록은 Redis 모델이며 RDB에 사용자별 투표 테이블을 저장하는 구조가 아닙니다. 게시판 삭제 API는 CLOSED로 전환하고 게시글 삭제는 Repository 삭제, 댓글 삭제는 논리 삭제입니다.

## 인증·권한·API 규칙

- Access Token: 응답 JSON의 `accessToken`, 클라이언트 메모리 저장, API에는 `Authorization: Bearer <access-token>` 전송. 설정상 유효기간 1시간.
- Refresh Token: HttpOnly `refreshToken` 쿠키, Redis의 사용자별 토큰과 비교 후 회전. 설정상 7일. 쿠키만으로 일반 API의 Bearer 인증을 대신하지 않음.
- `token_use=access/refresh`를 각각 검증하며 용도 정보 없는 이전 토큰도 거부.
- 서비스 등급 `USER / MANAGER / ADMIN`과 개별 게시판 관리자·부관리자 관계는 구분. ADMIN 등급이 모든 게시판의 소유자 검증을 자동 통과하지 않음.
- 게시판 생성은 MANAGER 또는 ADMIN. 정책·카테고리·투표 생성/마감은 해당 게시판 주관리자. 관리자 글 삭제는 해당 게시판 주관리자 또는 부관리자.
- GET은 전역에서 허용하지만 `/users/me`, 권한 신청 조회, AI 용병 조회 등은 메서드별 인증·인가가 추가됨. AI 추천 GET도 사용자별 호출 제한 때문에 로그인 필요.
- `/api/test/login`은 `@Profile("dev & !prod")` 조건에서만 등록. 운영 프로필에는 노출하지 않음.
- 페이지 요청 `page`는 0부터 시작. `@EnableSpringDataWebSupport(VIA_DTO)`로 `{ content, page: { size, totalElements, totalPages, number } }` 형태를 사용. 전체 응답을 감싸는 공통 success/data 래퍼는 없음.
- 오류는 `status, error, message, timestamp`와 입력 검증 시 `details`를 반환. `path`는 현재 공통 오류 본문에 없음. 일부 누락 리소스는 `IllegalArgumentException`으로 400이며 모든 미존재 요청이 404인 것은 아님.
- 전체 IP 버킷은 용량 200·분당 100 보충, 주석이 아닌 구현 기준. 표시된 사용자별 내부 요청은 용량/분당 15, 외부 AI 요청은 3. 초과 시 429.

## 개발 환경과 설정

JDK 17, Gradle Wrapper, 준비된 MySQL(dev) 또는 Oracle(기본), Redis가 필요합니다. Docker Compose·DB 마이그레이션 스크립트·Nginx 설정·systemd unit은 저장소에 없습니다. 운영용 기본 DDL은 `validate`이며 스키마를 자동 설치하지 않습니다.

아래 명령은 **백엔드 저장소 루트** 기준입니다. 통합 작업공간에서는 먼저 `cd backend/galdcup`으로 이동합니다.

### 환경변수

실제 비밀값은 문서·Git에 기록하지 않습니다. **Spring Boot가 `.env.develop`·`.env.deploy`를 자동으로 읽는 구성은 없습니다.** IDE 실행 설정, 셸 환경 또는 외부 서비스의 환경변수로 주입해야 합니다.

| 변수 | 용도 |
| --- | --- |
| `SPRING_DATASOURCE_URL`, `ORACLE_PASSWORD` | 기본 Oracle JDBC URL·비밀번호 |
| `REDIS_HOST`, `REDIS_PORT`, `REDISPASSWORD` | 기본 Redis 접속. 다른 인스턴스여도 접근 가능한 주소·인증 설정 사용 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | OAuth 인가 코드 교환. Redirect URI는 프론트엔드 콜백과 동일 |
| `JWT_SECRET_KEY` | JWT HMAC 서명 키. UTF-8 기준 최소 32바이트 필요 |
| `AES256_KEY` | 사용자 Gemini 키 암호화. 정확히 32바이트를 표현하는 64자리 16진수 |
| `GEMINI_AI_KEY` | 서버 공용 추천·여론 분석용 Gemini 키 |
| `CORS_ALLOWED_ORIGINS` | 허용 프론트엔드 원점. REST는 쉼표 분리 배열로 처리 |
| `FRONTEND_URL` | 기본 YAML의 프론트엔드 URL 설정 항목 |
| `COOKIE_SECURE`, `COOKIE_SAMESITE` | 기본 프로필 Refresh 쿠키 옵션. HTTPS·배치 도메인에 맞게 설정 |
| `JPA_DDL_AUTO` | 기본 프로필 스키마 처리, 기본 `validate` |
| `HIKARI_MAX`, `HIKARI_IDLE`, `BATCH_FETCH_SIZE` | 기본 프로필 풀·배치 설정, 기본 20·10·100 |
| `SPRING_PROFILES_ACTIVE` | 활성 프로필 선택; 개발은 dev, 운영에서 dev 혼용 금지 |

dev YAML은 MySQL 로컬 접속과 고정 개발 자격값, `ddl-auto: update`, Redis localhost:6379, 쿠키 Secure=false·SameSite=Lax를 정의합니다. 공유 DB를 사용하지 말고 접속 정보는 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` 등 표준 Spring 환경변수로 덮어씁니다. dev에서도 기본 YAML의 Redis 비밀번호 설정은 상속되므로 로컬 Redis 인증에 맞게 `REDISPASSWORD`를 설정합니다. dev DDL을 제한하려면 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` 등 표준 설정으로 덮어씁니다.

프론트엔드 환경변수 `VITE_*`는 별도의 정적 빌드 설정입니다. 여기에 서버 Secret을 넣지 않습니다.

### 실행

환경변수와 격리된 개발 DB·Redis를 먼저 준비합니다.

```sh
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

기본 포트는 8080입니다. dev는 Swagger UI·API docs를 켜고 기본 프로필은 끕니다. Swagger 설정의 CookieAuth 설명은 실제 Bearer 필터와 일치하지 않아, 현재 인증 계약은 Notion API 문서를 우선 확인합니다.

### 빌드·테스트

```sh
./gradlew test
./gradlew test bootJar
java -jar build/libs/galdcup-0.0.1-SNAPSHOT.jar
```

JAR 실행도 DB·Redis·키 설정이 필요합니다. Windows에서는 `./gradlew` 대신 `.\gradlew.bat`를 사용합니다. 캐시가 준비되어 있다면 `--offline`으로 의존성 네트워크 조회 없이 실행할 수 있습니다.

| 태스크 | 범위·준비 사항 |
| --- | --- |
| `test` | DB·Redis 접속 없는 단위 테스트. Redis 및 전체 컨텍스트 태그 제외 |
| `redisIntegrationTest` | 전용 **127.0.0.1** Redis에 실제 쓰기. `GALDCUP_VOTE_TEST_REDIS_PORT` 필수, 1024 초과·65535 이하·6379 제외 |
| `applicationContextTest` | 전체 앱 기동. 격리 DB·Redis·필수 환경변수 및 `GALDCUP_RUN_CONTEXT_TESTS=true` 필요 |

이미 준비한 전용 테스트 Redis의 포트가 16379인 경우:

```powershell
$env:GALDCUP_VOTE_TEST_REDIS_PORT = '16379'
.\gradlew.bat test redisIntegrationTest bootJar
```

운영 Redis를 테스트 대상으로 사용하지 않습니다. 기본 `build`나 `bootJar`만으로 Redis 통합·전체 기동 검증까지 모두 수행되지는 않습니다. 테스트 리포트는 `build/reports/tests/`, XML은 `build/test-results/`에 생성됩니다. 테스트 태스크에 테스트 미발견 실패와 HTML·XML 리포트가 설정되어 있습니다.

## CI/CD·운영

[백엔드 워크플로](.github/workflows/deploy.yml)는 master PR·push에서 JDK 17과 전용 Redis 7.2-alpine(16379)을 준비해 `clean test redisIntegrationTest bootJar`를 실행합니다. **verify가 성공한 master push만 동일 SHA의 검증된 JAR을 내려받아 전송하고 systemd galdcup 서비스를 재시작**합니다. PR은 배포하지 않으며 전체 컨텍스트 테스트는 CI에 포함되지 않습니다. 로컬 빌드·실행은 배포를 트리거하지 않습니다.

서버 접속용 Secret 이름은 `REMOTE_IP`, `REMOTE_USER`, `SSH_PRIVATE_KEY`입니다. 실행 환경변수·systemd unit·Nginx 설정은 외부 관리 대상입니다. CI에 자동 헬스체크·롤백 단계는 없습니다. 프론트엔드 CI는 별도로 빌드·전송·Nginx reload하며 테스트 단계가 없습니다.

[Dockerfile](Dockerfile)은 Java 17 다단계 빌드 중 `clean test bootJar`를 수행하고 포트 8080에서 JAR을 실행합니다. Redis 통합 테스트는 포함하지 않으며 현재 GitHub 배포는 Docker가 아닌 JAR·systemd 방식입니다. 이미지를 실제 빌드·배포해 검증한 것은 아닙니다.

로그는 코드상 root INFO·프로젝트 DEBUG와 컨트롤러/스케줄러 실행 시간 로그를 사용합니다. 파일 회전·보관 정책과 Redis AOF/RDB·백업·복구 설정은 저장소에서 확인되지 않습니다. Redis는 캐시 외에 투표 원장·Refresh Token·호출 제한도 보관하므로 실제 영속성·복구 정책을 별도 관리해야 합니다.

## API 상세 명세 및 핵심 트러블슈팅 안내

본 프로젝트의 상세한 RESTful API 명세서와 성능 최적화, 동시성 제어, 다중 계층 트래픽 제어(Bucket4j), 도메인 파편화(CORS) 해결 등 핵심 기술적 도전 과제 및 지표 분석 리포트는 아래 Notion 페이지에서 모두 확인할 수 있다.

* 👉 [Galdcup 전체 API 명세 및 트러블슈팅 문서 확인하기 (Notion)](https://www.notion.so/3350e2be9aa9802baf55e83fd005ad72)
```
