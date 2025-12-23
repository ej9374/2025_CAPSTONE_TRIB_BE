# ✈️ TriB: AI 올인원 여행 매니저
<img width="450" height="250" alt="image" src="https://github.com/user-attachments/assets/da040efe-8479-4dbc-b238-41360ab04160" />

> **Project Period:** 2025.09 ~ 2025.09

## 📖 Project Overview
트리비(TriB:Buddy)의 핵심 목표는 AI 기반 자동 여행 일정 생성 기능을 중심으로 한 ‘여행 매니저 플랫폼’ 입니다.
- 대화 기반 일정 생성: 채팅방에서 공유된 장소, 숙소, 북마크 정보를 AI가 분석하여 자동으로 최적의 여행 일정을 생성
- 효율적 동선 설계: 단순한 장소 나열이 아닌, 지도 상 이동 시간을 최소화하는 동선 제시
- 부가 기능 연계: 생성된 일정은 지도 위에 시각화되며 길찾기, 예산 관리, 커뮤니티 공유 기능과 유기적으로 연결


## 아키텍처 구조
<img width="1316" height="898" alt="Image" src="https://github.com/user-attachments/assets/0752face-6e52-4ec7-9481-0f5c9425e557" />
**모놀로식 아키텍처**
> AI 모델 추론 부하를 분리하기 위해 별도의 서버를 구성하고 Internal API로 통신

## 🛠️ Tech Stack
- **Backend:** Java, Spring Boot, JPA
- **Database:** MySQL, Redis
- **Infra:** AWS EC2, Docker, GitHub Actions, FCM(Firebase Cloud Messaging)
- **AI:** FastAPI (Python), Google Gemini API

## ✅ 주요 기능 (Key Features)

### 🔐 인증 & 사용자 (Auth)
- **간편 로그인:** 카카오, 애플 계정을 연동한 소셜 로그인 (OAuth 2.0)
- **이메일 인증:** Redis를 통한 SMTP 기반의 이메일 인증 프로세스
- **보안 로그인:** JWT 토큰 기반 인증 및 자동 로그인 유지
- **프로필 관리:** 닉네임, 프로필 사진 변경 및 마이페이지

### 💬 친구 & 채팅 (Social & Chat)
- **친구 관리:** 닉네임 검색을 통한 친구 추가 및 차단
- **실시간 채팅:** 1:1 및 그룹 채팅방 생성, WebSocket 기반의 실시간 메시지 전송 및 FCM 기반 푸시 알림
- **채팅방 관리:** 여행 멤버 초대, 채팅방 나가기

### ✈️ 여행 & 지도 (Travel & Map)
- **AI 일정 생성:** 채팅방 대화 내용을 분석하여 여행 코스 자동 추천
- **비동기 알림 처리:** 생성 시간이 소요되는 AI 작업 완료 시, 사용자를 호출하는 시스템 알림(Push Notification) 발송
- **일정 관리:** 날짜별 여행 동선 시각화 및 세부 일정 수정
- **길찾기:** 지도 API를 활용한 장소 간 이동 경로 및 소요 시간 제공

### 📝 커뮤니티 (Community)
- **정보 공유:** 여행 후기 및 꿀팁 게시글 작성 (이미지 업로드 포함)
- **소통:** 게시글 좋아요, 댓글 작성 및 조회, 다중 조건 검색(QueryDSL)
