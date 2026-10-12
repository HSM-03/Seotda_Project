package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SeotdaServer {

    private static final int PORT = 5000;
    private static List<PlayerHandler> players = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket()) {

            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));

            System.out.println("서버 시작! 포트: " + PORT);

            while (true) {  // 🔥 여러 판 반복
                players = new ArrayList<>();  // 매 판마다 새 리스트

                System.out.println("두 명 접속하면 게임 시작!");

                // 🔥 1) 두 명 접속 대기
                while (players.size() < 2) {
                    Socket client = serverSocket.accept();
                    System.out.println("클라이언트 접속: " + client.getInetAddress());

                    try {
                        PlayerHandler handler = new PlayerHandler(client);
                        players.add(handler);

                        handler.send("JOIN_OK"); // 클라에게 접속 성공 전달
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("서버 종료됨");
                    }
                }

                // 🔥 2) 게임 시작
                startGame();   // ⬅ 한 번 접속한 두 명으로 여러 판 돌리는 메서드

                // 한 판 끝
                System.out.println("=== 한 판 종료, 다음 게임 대기 ===");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ▽ 이 메서드 안에서 "다시하기"가 포함된 여러 판을 처리
    private static void startGame() {
        boolean playing = true;  // 두 플레이어가 둘 다 다시하기를 누르면 true 유지

        while (playing) {
            System.out.println("두 명 접속 완료. 게임 시작!");

            // Deck 만들고 섞기
            Deck deck = new Deck();
            deck.shuffle();

            // 플레이어 객체 생성
            Player p1 = new Player("Player1");
            Player p2 = new Player("Player2");

            // 두 명에게 카드 두 장씩 나눔
            p1.setCards(deck.draw(), deck.draw());
            p2.setCards(deck.draw(), deck.draw());

            // PlayerHandler에 플레이어 정보 연결
            players.get(0).setPlayer(p1);
            players.get(1).setPlayer(p2);

            // 족보 판정
            HandRank r1 = SeotdaRule.evaluate(p1.getCard1(), p1.getCard2());
            HandRank r2 = SeotdaRule.evaluate(p2.getCard1(), p2.getCard2());

            // 승자 판정
            int cmp = SeotdaRule.compareHands(
                    p1.getCard1(), p1.getCard2(),
                    p2.getCard1(), p2.getCard2()
            );

            String result1; // 플레이어1 결과
            String result2; // 플레이어2 결과
            
            if (cmp > 0) {
                result1 = "WIN";
                result2 = "LOSE";
            } else if (cmp < 0) {
                result1 = "LOSE";
                result2 = "WIN";
            } else {
                result1 = "DRAW";
                result2 = "DRAW";
            }

            try {
                // 1) 우선 각 클라이언트에게 자기 카드 2장만 전송
                players.get(0).send("CARD " + p1.getCard1() + "," + p1.getCard2());
                players.get(1).send("CARD " + p2.getCard1() + "," + p2.getCard2());

                // 2) 두 클라이언트가 모두 자신의 카드를 다 뒤집었다고 알릴 때까지 대기
                String open1 = players.get(0).read();  // 기대값: "OPEN"
                String open2 = players.get(1).read();  // 기대값: "OPEN"
                
                System.out.println("플레이어1 OPEN: " + open1);
                System.out.println("플레이어2 OPEN: " + open2);

                // 3) 이제 족보/결과 전송
                players.get(0).send("RANK " + r1.getName());
                players.get(0).send("RESULT " + result1);
                
                // 🔥 플레이어1에게 "상대(플레이어2)의 카드" 알려주기
                players.get(0).send("OPP_CARD " + p2.getCard1() + "," + p2.getCard2());

                players.get(1).send("RANK " + r2.getName());
                players.get(1).send("RESULT " + result2);
                
                // 🔥 플레이어2에게 "상대(플레이어1)의 카드" 알려주기
                players.get(1).send("OPP_CARD " + p1.getCard1() + "," + p1.getCard2());

                System.out.println("게임 결과 + 상대 패 정보 전송 완료.");


                // ===== 여기서부터 "다시하기" 처리 =====

                // 클라들에게 "다시하기/종료 선택해라" 신호 보내기
                players.get(0).send("ASK_RETRY");
                players.get(1).send("ASK_RETRY");
                System.out.println("ASK_RETRY 전송, 응답 대기중...");
                
                // 클라이언트 선택 받기
                String cmd1 = players.get(0).read();  // "RETRY" 또는 "EXIT" 등
                String cmd2 = players.get(1).read();

                System.out.println("플레이어1 응답: " + cmd1);
                System.out.println("플레이어2 응답: " + cmd2);

                // 클라이언트 선택 받기 (PlayerHandler에 메서드 추가 필요)
                boolean p1Retry = "RETRY".equalsIgnoreCase(cmd1);  // true면 다시하기
                boolean p2Retry = "RETRY".equalsIgnoreCase(cmd2);

                if (p1Retry && p2Retry) {
                    // 둘 다 다시하기 → while(playing) 계속 진행
                    System.out.println("두 플레이어 모두 다시하기 선택 → 다음 판 진행");
                    // playing = true 그대로 두고 루프 반복
                } else {
                    // 둘 중 한 명이라도 종료 선택 → 소켓 닫고 루프 종료
                    System.out.println("누군가 종료 선택 → 게임 종료");
                    players.get(0).close();
                    players.get(1).close();
                    playing = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
                // 예외 나면 안전하게 종료
                try {
                    players.get(0).close();
                    players.get(1).close();
                } catch (Exception ignore) {}
                playing = false;
            }
        }
    }
}
