# 🏆 Galdcup (갈드컵)
> **소모적인 커뮤니티 말싸움을 가치 있는 데이터와 숫자로 박제하는 대규모 실시간 투표 및 AI 여론 분석 플랫폼**

---

## 🔗 링크 (Links)
* **운영 서버 주소**: [https://galdcup.live](https://galdcup.live)
* **프로젝트 상세 기술 포트폴리오**: [Notion 상세 문서 바로가기](https://www.notion.so/3350e2be9aa9802baf55e83fd005ad72)

---

## 📌 1. 서비스 기획 배경 및 핵심 가치

인터넷 커뮤니티에서 결론 없이 소모적이고 감정적으로 흘러가는 말싸움(갈드컵)의 에너지를 **'기간제 투표 시스템'**으로 흡수하여 숫자로 진짜 민심을 증명하는 문화 공간을 구축하고자 설계했다.

* **결과를 예측할 수 없는 긴장감**: 투표가 진행되는 동안은 실시간 득표수를 공개하지 않아 마지막 순간까지 결과를 예측할 수 없는 긴장감과 몰입감을 유지한다.
* **숫자로 증명하는 진짜 민심**: 단순히 댓글이 많이 달린 쪽이 이기는 구조를 탈피하여, 정밀하게 집계된 투표 데이터를 통해 조작 없는 진짜 여론을 투명하게 증명한다.
* **Gemini AI 기반 실시간 여론 분석**: 수천 개의 댓글을 일일이 읽지 않아도 AI가 현재 사용자들이 가장 뜨겁게 부딪히고 있는 핵심 쟁점과 여론의 온도를 실시간으로 요약 및 분석한다.
* **AI 기반 맞춤형 데이터 생성**: 활력이 떨어진 소외 게시판에 AI가 트렌디한 투표 주제와 풍부한 서사의 토론 가이드 게시글 및 균형 잡힌 댓글 소스를 직접 자동 생성하여 사용자의 유입과 지속적인 참여를 유도한다.

---

## 🛠 2. 기술 스택 (Tech Stack)

| 분류 | 상세 기술 스택 |
| :--- | :--- |
| **Frontend** | Vue.js, STOMP (실시간 웹소켓 통신) |
| **Backend** | Java 17, Spring Boot 3.4.0, Embedded Tomcat |
| **Database** | Oracle Database (영속성 백엔드), Redis 7.2 (고속 인메모리) |
| **AI Engine** | Gemini AI (Google GenAI SDK 1.0.0) |
| **Storage** | Supabase Storage (정적 이미지 파일 저장소) |
| **Network** | NGINX (Reverse Proxy), Ubuntu (OCI) |
| **Security** | Spring Security, JWT, OAuth2, Bucket4j (다중 계층 트래픽 제어) |

---

## 📐 3. 시스템 아키텍처 및 계층별 역할

```
[사용자 UI: Vue.js] ──(NGINX 리버스 프록시)──> [Spring Security / JWT 필터 계층]
                                                           │
                    ┌──────────────────────────────────────┴──────────────────────────────────────┐
                    ▼ (IP 및 회원별 L1/L2 트래픽 제어)                                             ▼ (실시간 투표 브로드캐스팅)
             [Bucket4j + Redis]                                                    [STOMP WebSocket Broker]
                    │                                                                             │
                    ▼                                                                             ▼
         [Spring Boot 핵심 도메인 로직] ───────────────────────────────────────────────> [Gemini AI Engine]
            │                        │                                                  (맥락 분석 및 추천)
            ▼ (안전한 영속 데이터 구조) ▼ (고속 인메모리 캐싱 / Lua Script 원자 연산)
     [Oracle SQL Database]     [Redis In-Memory Database]
```

---

## 📑 4. API 상세 명세 및 핵심 트러블슈팅 안내

본 프로젝트의 상세한 RESTful API 명세서와 성능 최적화, 동시성 제어, 다중 계층 트래픽 제어(Bucket4j), 도메인 파편화(CORS) 해결 등 핵심 기술적 도전 과제 및 지표 분석 리포트는 아래 Notion 페이지에서 모두 확인할 수 있다.

* 👉 [Galdcup 전체 API 명세 및 트러블슈팅 문서 확인하기 (Notion)](https://www.notion.so/3350e2be9aa9802baf55e83fd005ad72)
```
