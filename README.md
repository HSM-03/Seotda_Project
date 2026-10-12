# 🎴 섯다 (Seotda) - Java Socket 1:1 온라인 카드 게임

Java Socket(TCP) 통신으로 두 명이 실시간으로 대결하는 섯다 게임입니다.
서버가 패를 나누고 족보와 승패를 판정하며, 클라이언트는 JavaFX로 만든 화면에서 카드를 뒤집어 결과를 확인합니다.

<br>

## 🛠 Tech Stack

<img src="https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/Java%20Socket-007396?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/JavaFX-007396?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/Eclipse-2C2255?style=for-the-badge&logo=eclipseide&logoColor=white">

| 구분 | 사용 기술 |
|---|---|
| 통신 | Java Socket (TCP), DataInputStream / DataOutputStream |
| 클라이언트 UI | JavaFX, FXML, CSS, 애니메이션(TranslateTransition, ScaleTransition), MediaPlayer(BGM) |
| 데이터베이스 | MySQL, JDBC (PreparedStatement) |
| 개발 환경 | Eclipse, JDK 17, JavaFX SDK |

<br>

## ✨ 주요 기능

- **회원가입 / 로그인**: MySQL에 회원 정보를 저장하고, 아이디 중복 확인과 로그인 검증 처리
- **1:1 온라인 대전**: 두 명이 서버에 접속하면 자동으로 게임 시작
- **패 분배와 족보 판정**: 서버에서 20장 덱을 섞어 2장씩 나눠주고 족보와 승패 계산
- **카드 뒤집기 연출**: 두 장을 모두 뒤집으면 서버에 알리고, 그 뒤에 내 족보·결과·상대 패가 공개됨
- **다시하기**: 두 플레이어가 모두 다시하기를 누르면 같은 연결로 다음 판 진행
- **UI 연출**: 카드 이동·뒤집기 애니메이션, 로그인 화면 배경음악

<br>

## 🏗 구조

```
TCPIP_Project
├── SeotdaServer          # 게임 서버
│   └── src/server
│       ├── SeotdaServer.java    # 접속 대기, 게임 진행, 다시하기 처리
│       ├── PlayerHandler.java   # 클라이언트 1명과의 송수신 담당
│       ├── SeotdaRule.java      # 족보 판정, 승패 비교
│       ├── Deck.java / Card.java / Player.java / HandRank.java
│
└── seotda                # 게임 클라이언트 (JavaFX)
    └── src/seotda
        ├── MainApp.java             # 화면 전환
        ├── LoginController.java     # 로그인 화면 + BGM
        ├── RegisterController.java  # 회원가입 화면
        ├── MainController.java      # 게임 화면, 서버 통신, 카드 애니메이션
        ├── UserDAO.java / DBUtil.java  # MySQL 연동
        ├── *.fxml, style.css        # 화면 레이아웃과 스타일
        └── cards/, Sound/, ...      # 카드 이미지, 배경음악
```

클라이언트는 서버 메시지를 **별도 수신 스레드**에서 받고, 화면 갱신은 `Platform.runLater`로 JavaFX UI 스레드에 넘겨서 처리합니다.

<br>

## 📡 통신 프로토콜

서버와 클라이언트는 `writeUTF` / `readUTF`로 문자열 메시지를 주고받습니다.

| 방향 | 메시지 | 의미 |
|---|---|---|
| 서버 → 클라 | `JOIN_OK` | 접속 성공 |
| 서버 → 클라 | `CARD a,b` | 내 카드 2장 전달 |
| 클라 → 서버 | `OPEN` | 카드 2장을 모두 뒤집음 |
| 서버 → 클라 | `RANK 족보이름` | 내 족보 |
| 서버 → 클라 | `RESULT WIN/LOSE/DRAW` | 승패 결과 |
| 서버 → 클라 | `OPP_CARD a,b` | 상대 카드 공개 |
| 서버 → 클라 | `ASK_RETRY` | 다시하기 여부 질문 |
| 클라 → 서버 | `RETRY` / `EXIT` | 다시하기 / 종료 선택 |

```
클라1      서버      클라2
  │ ─ 접속 ─▶│◀─ 접속 ─ │
  │◀ CARD ── │ ── CARD ▶│
  │ ─ OPEN ─▶│◀─ OPEN ─ │
  │◀ RANK, RESULT, OPP_CARD ▶│
  │◀ ASK_RETRY ─────────▶│
  │ ─ RETRY/EXIT ▶│◀ RETRY/EXIT ─│
```

<br>

## 🀄 구현한 족보

| 순위 | 족보 |
|---|---|
| 광땡 | 38광땡 > 13광땡 > 18광땡 |
| 땡 | 장땡(10땡) > 9땡 > … > 1땡 |
| 특수 | 알리(1·2) > 독사(1·4) > 구삥(1·9) > 장삥(1·10) > 장사(4·10) > 세륙(4·6) |
| 끗 | 9끗 > 8끗 > … > 1끗 > 망통 |
| 특수 규칙 | 47 암행어사(13·18광땡 잡음), 37 땡잡이(1~9땡 잡음), 구사·멍텅구리 구사(재경기) |

<br>

## ▶ 실행 방법

**1. DB 준비 (MySQL)**

```sql
CREATE DATABASE seotda_db;
USE seotda_db;

CREATE TABLE user (
    user_id   VARCHAR(50) PRIMARY KEY,
    user_pw   VARCHAR(100) NOT NULL,
    user_name VARCHAR(50)  NOT NULL
);
```

`seotda/src/seotda/DBUtil.java`의 접속 정보를 본인 MySQL 환경에 맞게 수정합니다.

**2. 라이브러리 설정**

- JDK 17 이상
- [JavaFX SDK](https://openjfx.io/)
- [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/)

Eclipse에서 `seotda` 프로젝트의 Build Path에 위 라이브러리를 추가합니다.

**3. 실행 순서**

1. `SeotdaServer` 프로젝트의 `SeotdaServer.java` 실행 (포트 5000)
2. `seotda` 프로젝트의 `MainApp.java`를 **두 번** 실행해 클라이언트 2개 띄우기
3. 각각 로그인하면 게임 시작

<br>

## 📸 실행 화면

<!-- 스크린샷을 찍어서 아래처럼 넣어주세요 -->
<!-- <img src="images/login.png" width="45%"> <img src="images/game.png" width="45%"> -->

<br>

## 💡 배운 점 / 개선하고 싶은 점

- 서버와 클라이언트가 같은 순서로 메시지를 주고받도록 **간단한 텍스트 프로토콜을 직접 설계**해본 경험
- 네트워크 수신은 별도 스레드에서, 화면 갱신은 UI 스레드에서 처리해야 한다는 점을 JavaFX로 익힘
- 개선할 점
  - 현재 서버는 한 번에 2명만 받는 구조 → 스레드 풀을 써서 여러 방을 동시에 돌리는 구조로 확장
  - 베팅(콜, 다이, 따당 등) 기능 추가
  - 비밀번호를 해시로 저장하고, DB 접속 정보를 설정 파일로 분리
