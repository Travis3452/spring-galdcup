# Spring Boot·Prometheus 연동

## 연결 구조

```text
사용자 → 기존 API 포트 8080
Prometheus 127.0.0.1:9090 → Actuator 127.0.0.1:9091
```

`micrometer-registry-prometheus` 버전은 Spring Boot 4.0.0의 의존성 관리에 맡긴다. 기존 Redis Exporter와 Redis 인증 설정은 변경하지 않는다.

관리 서버는 loopback에 바인딩하고 GET health·prometheus만 익명으로 허용한다. Actuator 보안 체인은 실제 관리 서블릿 컨텍스트인지도 검사한다. 일반 API 포트의 `/actuator` 경로는 로그인 여부와 관계없이 거부한다. `env`, `beans`, `heapdump`, 탐색 페이지는 공개하지 않는다. health 응답에는 상세 정보·컴포넌트를 포함하지 않는다.

프록시에서 관리 포트나 Prometheus를 외부로 연결하지 않는다. loopback은 같은 서버의 프로세스가 접근할 수 있는 범위이며 Prometheus 프로세스만 식별·인증하는 방식은 아니다.

## 검증

```sh
./gradlew test bootJar
```

`MonitoringIntegrationTests`는 임의의 로컬 포트에 API·관리 서버를 실제로 띄운다. 업무 도메인·DB·Redis·Google OAuth를 로드하지 않고 다음을 검증한다.

- 관리 포트의 익명 health·Prometheus 조회와 상세 정보 비공개
- API 포트에서 익명·ADMIN·Forwarded 헤더 요청의 지표 접근 차단
- 기존 공개 GET과 JWT 보호 POST, Refresh Token 혼용 거부
- 실제 HTTP 요청의 count·sum·max와 정규화된 URI 템플릿

기존 Redis 통합 테스트는 전용 테스트 Redis를 준비해 `redisIntegrationTest`로 별도 실행한다. GitHub Actions는 기존대로 단위·Redis 통합 테스트를 통과한 JAR만 master push에서 배포한다.

## 서버 적용 순서

1. 기존 JAR과 `/etc/prometheus/prometheus.yml`을 백업한다.
2. 검증된 JAR을 임시 이름으로 전송하고 체크섬을 비교한 뒤 교체한다.
3. 기존 systemd 환경변수를 유지한 채 `galdcup`를 재시작한다.
4. 아래 명령으로 관리 서버의 바인딩·응답과 기존 API의 인증 동작을 확인한다.

```sh
sudo ss -ltnp | grep -E ':(8080|9090|9091)\b'
curl --max-time 5 -fsS http://127.0.0.1:9091/actuator/health
curl --max-time 5 -fsS http://127.0.0.1:9091/actuator/prometheus
curl --max-time 5 -sS -o /dev/null -w '%{http_code}\n' \
  http://127.0.0.1:8080/actuator/prometheus
```

9091은 `127.0.0.1`에만 바인딩되어야 한다. 관리 포트의 scrape는 200, API 포트의 Actuator 요청은 4xx여야 한다. 애플리케이션 기동에 실패하면 백업 JAR을 복원하고 재시작한다.

## Prometheus job

기존 `prometheus`, `galdcup-redis` job을 유지하고 `scrape_configs`에 다음 항목을 추가한다.

```yaml
  - job_name: 'galdcup-spring'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['127.0.0.1:9091']
```

설정을 검사한 뒤 HUP으로 반영한다.

```sh
sudo /usr/local/bin/promtool check config /etc/prometheus/prometheus.yml
sudo systemctl kill -s HUP prometheus
```

30초 수집 주기를 기다린 뒤 target과 지표를 확인한다.

```promql
up{job="galdcup-spring"}
http_server_requests_seconds_count{job="galdcup-spring"}
http_server_requests_seconds_sum{job="galdcup-spring"}
http_server_requests_seconds_max{job="galdcup-spring"}
```

`up=1`은 scrape 성공을 뜻한다. Oracle·Redis를 포함한 서비스 전체 정상 여부와는 구분한다. HTTP timer는 해당 요청이 발생한 뒤 생성되며 실제 수집 결과의 이름·라벨을 확인한다.

## 조회 예시

수집 중인 URI·status·method 라벨을 확인한 뒤 조회한다. 아래 쿼리는 Actuator 요청을 제외한 HTTP 요청을 집계한다.

```promql
# RPS
sum(rate(http_server_requests_seconds_count{job="galdcup-spring",uri!~"/actuator.*"}[5m]))

# 평균 응답 시간(ms). 요청이 없는 구간은 분모가 0이므로 별도로 표시한다.
1000 * sum(rate(http_server_requests_seconds_sum{job="galdcup-spring",uri!~"/actuator.*"}[5m]))
 / sum(rate(http_server_requests_seconds_count{job="galdcup-spring",uri!~"/actuator.*"}[5m]))

# 4xx 비율(%)
100 * sum(rate(http_server_requests_seconds_count{job="galdcup-spring",uri!~"/actuator.*",status=~"4.."}[5m]))
 / sum(rate(http_server_requests_seconds_count{job="galdcup-spring",uri!~"/actuator.*"}[5m]))

# 5xx 비율(%)
100 * sum(rate(http_server_requests_seconds_count{job="galdcup-spring",uri!~"/actuator.*",status=~"5.."}[5m]))
 / sum(rate(http_server_requests_seconds_count{job="galdcup-spring",uri!~"/actuator.*"}[5m]))

# 최근 수집한 rolling maximum 중 최대값(ms)
1000 * max(max_over_time(http_server_requests_seconds_max{job="galdcup-spring",uri!~"/actuator.*"}[5m]))
```

`_max`는 Micrometer TimeWindowMax의 rolling maximum이다. 영구 최대값이나 요청 발생 시각 기준의 정확한 5분 최대값으로 표시하지 않는다. Counter는 재시작 시 초기화되므로 RPS·비율에는 `rate()`를 사용한다. 4xx·5xx 시계열이 아직 없을 때의 빈 결과와 0%도 구분해 처리해야 한다.

메모리를 고려해 HTTP percentile histogram을 추가로 켜지 않는다. 향후 백분위수나 구간별 지표가 필요하면 bucket 수·라벨 수·보관 비용을 먼저 검토한다.

## 배포 후 자원 확인

```sh
free -h
vmstat 1 5
ps -C java,prometheus -o pid,comm,rss,etime
systemctl is-active galdcup prometheus
systemctl is-enabled galdcup prometheus
```

1GB RAM 서버에서는 배포 전후 RSS·Swap과 지속적인 swap-in/out을 함께 확인한다. Prometheus의 7일 보관, loopback 바인딩, 기존 Redis job은 유지한다. Node Exporter·추가 대시보드 도구는 설치하지 않는다.
