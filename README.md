# ✈️ TriB: AI 올인원 여행 매니저
> **Project Period:** 2025.09 ~ (In Progress 🚧)

![In Progress](https://img.shields.io/badge/Status-In%20Progress-yellow)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.2-green)

## 📖 Project Overview
트리비(TriB:Buddy)의 핵심 목표는 AI 기반 자동 여행 일정 생성 기능을 중심으로 한 ‘여행 매니저 플랫폼’ 입니다.
- 대화 기반 일정 생성: 채팅방에서 공유된 장소, 숙소, 북마크 정보를 AI가 분석하여 자동으로 최적의 여행 일정을 생성
- 효율적 동선 설계: 단순한 장소 나열이 아닌, 지도 상 이동 시간을 최소화하는 동선 제시
- 부가 기능 연계: 생성된 일정은 지도 위에 시각화되며 길찾기, 예산 관리, 커뮤니티 공유 기능과 유기적으로 연결

**현재 대부분의 기능을 구현 완료 하여 출시 준비 중에 있습니다**

## 아키텍처 구조
<img width="1316" height="898" alt="Image" src="https://github.com/user-attachments/assets/0752face-6e52-4ec7-9481-0f5c9425e557" />
**Micro-level decoupling in Monolithic Architecture**
> AI 모델 추론 부하를 분리하기 위해 별도의 서버를 구성하고 Internal API로 통신합니다.

## 🛠️ Tech Stack
- **Backend:** Java, Spring Boot, JPA
- **Database:** MySQL 8.0, Redis
- **Infra:** AWS EC2, Docker, GitHub Actions
- **AI:** FastAPI (Python), Google Gemini API

## ✅ Feature Status (Development Log)
- [x] MVP 기능 구현 완료
- [] 앱 출시를 위한 애플 로그인 구현
- [] WebSocket 연결 반응 느린 부분 개선
- [] 채팅 목록 안읽은 메세지 수 실시간 업데이트
