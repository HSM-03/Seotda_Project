package seotda;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class MainController {

    // ===== FXML =====
    @FXML private Label statusLabel;
    @FXML private Label rankLabel;
    @FXML private Label resultLabel;

    @FXML private ImageView card1Image;   // 내 1번 카드
    @FXML private ImageView card2Image;   // 내 2번 카드
    @FXML private ImageView opCard1Image; // 상대 1번
    @FXML private ImageView opCard2Image; // 상대 2번
    @FXML private ImageView deckImage;    // 가운데 덱

    @FXML private Button startButton;
    @FXML private Button retryButton;     // 사용 안 함 (숨김)
    @FXML private Button exitButton;
    @FXML private Label userNameLabel;

    // ===== 네트워크 =====
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean connected = false;
    private boolean canRetry  = false;    // 서버가 ASK_RETRY 보냈는지

    // ===== 게임 상태 =====
    private String myCard1Key = null;
    private String myCard2Key = null;

    private boolean myCard1Opened = false;
    private boolean myCard2Opened = false;
    private boolean openSent      = false;  // 이번 판에 OPEN을 보냈는지
    
    // 상대 카드 키 & 뒤집힘 여부
    private String oppCard1Key;
    private String oppCard2Key;
    private boolean oppCardsRevealed = false;

    // ====================== 초기화 ======================
    @FXML
    private void initialize() {
    	if (userNameLabel != null) {
            String name = MainApp.getLoggedUserName();
            if (name != null && !name.isEmpty()) {
                userNameLabel.setText(name + " 님 환영합니다!");
            } else {
                userNameLabel.setText(""); // 또는 "손님 님 환영합니다!" 등
            }
        }
        Image back = loadCardImage("back.png");

        double myW = 90, myH = 135;
        double opW = 80, opH = 120;
        double deckW = 90, deckH = 135;

        // 내 카드
        if (card1Image != null) {
            card1Image.setFitWidth(myW);
            card1Image.setFitHeight(myH);
            card1Image.setPreserveRatio(true);
            card1Image.setImage(back);
            card1Image.setVisible(false);
            card1Image.setPickOnBounds(true);
            card1Image.setOnMouseClicked(e -> onMyCardClicked(1));
        }
        if (card2Image != null) {
            card2Image.setFitWidth(myW);
            card2Image.setFitHeight(myH);
            card2Image.setPreserveRatio(true);
            card2Image.setImage(back);
            card2Image.setVisible(false);
            card2Image.setPickOnBounds(true);
            card2Image.setOnMouseClicked(e -> onMyCardClicked(2));
        }

        // 상대 카드
        if (opCard1Image != null) {
            opCard1Image.setFitWidth(opW);
            opCard1Image.setFitHeight(opH);
            opCard1Image.setPreserveRatio(true);
            opCard1Image.setImage(back);
            opCard1Image.setVisible(false);
            opCard1Image.setPickOnBounds(true);
        }
        if (opCard2Image != null) {
            opCard2Image.setFitWidth(opW);
            opCard2Image.setFitHeight(opH);
            opCard2Image.setPreserveRatio(true);
            opCard2Image.setImage(back);
            opCard2Image.setVisible(false);
            opCard2Image.setPickOnBounds(true);
        }

        // 덱
        if (deckImage != null) {
            deckImage.setFitWidth(deckW);
            deckImage.setFitHeight(deckH);
            deckImage.setPreserveRatio(true);
            deckImage.setImage(back);
            deckImage.setVisible(true);
        }

        // 다시하기 버튼은 숨김
        if (retryButton != null) {
            retryButton.setVisible(false);
            retryButton.setManaged(false);
        }

        if (exitButton != null) {
            exitButton.setDisable(true);
        }
        if (startButton != null) {
            startButton.setText("게임 시작");
        }

        if (rankLabel != null)   rankLabel.setText("족보: -");
        if (resultLabel != null) resultLabel.setText("결과: -");
        if (statusLabel != null) statusLabel.setText("서버에 연결되지 않음");
    }

    // ====================== 버튼 이벤트 ======================

    @FXML
    private void handleStart() {
        if (!connected) {
            connectToServer();
        } else {
            handleRetryLogic();
        }
    }

    // 다시하기 (이미 서버에 붙어 있을 때만)
    private void handleRetryLogic() {
        if (!canRetry) {
            Platform.runLater(() ->
                    statusLabel.setText("서버가 아직 다음 판 준비중입니다...")
            );
            return;
        }

        try {
            if (out != null) {
                out.writeUTF("RETRY");
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        canRetry  = false;
        openSent  = false;
        myCard1Opened = false;
        myCard2Opened = false;

        Platform.runLater(() -> {
            statusLabel.setText("상대 플레이어 선택 기다리는 중...");
            startButton.setDisable(true);
            hideAllCards();
            rankLabel.setText("족보: -");
            resultLabel.setText("결과: -");
        });
    }

    @FXML
    private void handleExit() {
        try {
            if (out != null) {
                out.writeUTF("EXIT");
                out.flush();
            }
        } catch (Exception ignored) {}
        closeConnection();
        Platform.exit();
    }

    // ====================== 서버 접속 ======================

    private void connectToServer() {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;

        Platform.runLater(() -> {
            statusLabel.setText("서버 접속 시도중...");
            startButton.setDisable(true);
            exitButton.setDisable(true);
            setCardBack(card1Image);
            setCardBack(card2Image);
            setCardBack(opCard1Image);
            setCardBack(opCard2Image);
            rankLabel.setText("족보: -");
            resultLabel.setText("결과: -");
        });

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 5000);
                in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                connected = true;
                canRetry  = false;

                Platform.runLater(() -> {
                    statusLabel.setText("서버 연결 완료. 다른 플레이어 접속 대기중...");
                    exitButton.setDisable(false);
                });

                while (true) {
                    String msg;
                    try {
                        msg = in.readUTF();
                    } catch (Exception ex) {
                        break;
                    }
                    handleServerMessage(msg);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        statusLabel.setText("서버 연결 오류 또는 서버가 종료됨")
                );
            } finally {
                closeConnection();
            }
        }).start();
    }

    // ====================== 서버 메시지 처리 ======================

    private void handleServerMessage(String msg) {
        System.out.println("서버로부터: " + msg);

        if (msg.startsWith("JOIN_OK")) {
            Platform.runLater(() ->
                    statusLabel.setText("서버 접속 완료. 다른 플레이어 접속 기다리는 중...")
            );

        } else if (msg.startsWith("CARD ")) {
            String data  = msg.substring("CARD ".length());
            String[] arr = data.split(",");
            String c1 = arr.length > 0 ? arr[0].trim() : "";
            String c2 = arr.length > 1 ? arr[1].trim() : "";

            // 새 라운드 상태 초기화
            myCard1Key = c1;
            myCard2Key = c2;
            myCard1Opened = false;
            myCard2Opened = false;
            openSent      = false;
            canRetry      = false;
            // 🔥 상대 카드 상태 리셋
            oppCard1Key = null;
            oppCard2Key = null;
            oppCardsRevealed = false;

            Platform.runLater(() -> {
                statusLabel.setText("카드를 눌러 확인해 주세요");
                startButton.setDisable(true);
                hideAllCards();
                safeDeal(c1, c2);   // 덱에서 카드 4장 날리기
                rankLabel.setText("족보: -");
                resultLabel.setText("결과: -");
            });

        } else if (msg.startsWith("OPP_CARD ")) {
            String data  = msg.substring("OPP_CARD ".length());
            String[] arr = data.split(",");
            oppCard1Key = arr.length > 0 ? arr[0].trim() : "";
            oppCard2Key = arr.length > 1 ? arr[1].trim() : "";

            // 🔥 상대 카드 두 장을 한 번에 자동 뒤집기
            revealOpponentCards();
            
        } else if (msg.startsWith("RANK ")) {
            String data = msg.substring("RANK ".length());
            String text = "족보: " + data;
            Platform.runLater(() -> rankLabel.setText(text));

        } else if (msg.startsWith("RESULT ")) {
            String data = msg.substring("RESULT ".length());

            String korean;
            switch (data) {
                case "WIN":  korean = "승리";  break;
                case "LOSE": korean = "패배";  break;
                case "DRAW": korean = "무승부"; break;
                default:     korean = data;    break;
            }

            String text = "결과: " + korean;

            Platform.runLater(() -> {
                resultLabel.setText(text);
                statusLabel.setText("다음 판을 하려면 '게임 시작'을 누르거나, 종료하시려면 '나가기'를 선택하세요");
            });

        } else if (msg.equals("ASK_RETRY")) {
            // 서버가 다음 판 선택하라고 허용
            canRetry = true;
            Platform.runLater(() -> {
                statusLabel.setText("다음 판을 하려면 '게임 시작'을 누르거나, 종료하시려면 '나가기'를 선택하세요");
                startButton.setText("게임 시작");
                startButton.setDisable(false);   // 이제 다시 누를 수 있음
                exitButton.setDisable(false);
            });
        }
    }

    // ====================== 카드 이미지 유틸 ======================

    private Image loadCardImage(String fileName) {
        return new Image(getClass().getResourceAsStream("cards/" + fileName));
    }

    private void setCardBack(ImageView view) {
        if (view != null) {
            view.setImage(loadCardImage("back.png"));
        }
    }

    private void hideAllCards() {
        if (card1Image != null) card1Image.setVisible(false);
        if (card2Image != null) card2Image.setVisible(false);
        if (opCard1Image != null) opCard1Image.setVisible(false);
        if (opCard2Image != null) opCard2Image.setVisible(false);
    }

    // 카드키 → 파일명 매핑
    private String mapCardKeyToFileName(String key) {
        if (key == null) return "back.png";
        key = key.trim();
        if (key.isEmpty()) return "back.png";

        if (key.contains("광")) {
            String num = key.replace("광", "");
            return num + "gwang.png";
        }

        String base = key;
        String variant = null;

        if (key.contains("_")) {
            String[] parts = key.split("_");
            if (parts.length >= 2) {
                base    = parts[0];
                variant = parts[1];
            }
        }

        switch (base) {
            case "1": return "1.png";
            case "3": return "3.png";
            case "8": return "8.png";
            case "2":
            case "4":
            case "5":
            case "6":
            case "7":
            case "9":
            case "10":
                if (variant == null || variant.isEmpty())
                    return base + "_1.png";
                else
                    return base + "_" + variant + ".png";
            default:
                return "back.png";
        }
    }

    // ====================== 카드 분배 & 애니메이션 ======================

    // 덱 중앙에서 target 자리로 날아감
    private void dealFromDeckTo(ImageView target,
                                String cardKey,
                                boolean faceUp,
                                int delayMillis) {
        if (deckImage == null || target == null) return;

        Image back = loadCardImage("back.png");

        target.setImage(back);
        target.setFitWidth(90);
        target.setFitHeight(135);
        target.setPreserveRatio(true);

        Platform.runLater(() -> {
            javafx.geometry.Bounds deckBounds   = deckImage.localToScene(deckImage.getBoundsInLocal());
            javafx.geometry.Bounds targetBounds = target.localToScene(target.getBoundsInLocal());

            double deckCenterX   = deckBounds.getMinX()   + deckBounds.getWidth()  / 2.0;
            double deckCenterY   = deckBounds.getMinY()   + deckBounds.getHeight() / 2.0;
            double targetCenterX = targetBounds.getMinX() + targetBounds.getWidth() / 2.0;
            double targetCenterY = targetBounds.getMinY() + targetBounds.getHeight()/ 2.0;

            double dx = deckCenterX - targetCenterX;
            double dy = deckCenterY - targetCenterY;

            target.setTranslateX(dx);
            target.setTranslateY(dy);
            target.setVisible(true);

            TranslateTransition tt = new TranslateTransition(Duration.millis(250), target);
            tt.setFromX(dx);
            tt.setFromY(dy);
            tt.setToX(0);
            tt.setToY(0);
            tt.setDelay(Duration.millis(delayMillis));

            tt.setOnFinished(e -> {
                if (!faceUp || cardKey == null) {
                    setCardBack(target);
                } else {
                    updateCardWithFlip(target, cardKey);
                }
            });

            tt.play();
        });
    }

    // P1(나) 1장 → P2 1장 → P1 2장 → P2 2장
    private void dealAllCards(String myC1, String myC2) {
        dealFromDeckTo(card1Image, myC1, false, 0);    // 내 1번 (처음엔 뒷면)
        dealFromDeckTo(opCard1Image, null, false, 200);
        dealFromDeckTo(card2Image, myC2, false, 400);
        dealFromDeckTo(opCard2Image, null, false, 600);
    }

    // 레이아웃 완료 후 분배 시작
    private void safeDeal(String c1, String c2) {
        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                if (card1Image.getScene() != null) {
                    dealAllCards(c1, c2);
                    stop();
                }
            }
        }.start();
    }

    // 내 카드 클릭
    private void onMyCardClicked(int index) {
        if (!connected) return;

        ImageView view;
        String key;

        if (index == 1) {
            if (myCard1Opened) return;
            view = card1Image;
            key  = myCard1Key;
        } else {
            if (myCard2Opened) return;
            view = card2Image;
            key  = myCard2Key;
        }

        if (view == null || key == null || key.isEmpty()) return;

        updateCardWithFlip(view, key);

        if (index == 1) myCard1Opened = true;
        else            myCard2Opened = true;

        // 두 장 다 열었으면 한 번만 OPEN 전송
        if (!openSent && myCard1Opened && myCard2Opened) {
            openSent = true;
            new Thread(() -> {
                try {
                    if (out != null) {
                        out.writeUTF("OPEN");
                        out.flush();
                    }
                    System.out.println("OPEN 전송 완료");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    // 카드 뒤집기 애니메이션 (앞면으로)
    private void updateCardWithFlip(ImageView view, String cardKey) {
        if (view == null) return;

        String fileName = mapCardKeyToFileName(cardKey);
        Image newImage  = loadCardImage(fileName);

        ScaleTransition hide = new ScaleTransition(Duration.millis(150), view);
        hide.setFromX(1.0);
        hide.setToX(0.0);

        hide.setOnFinished(e -> {
            view.setImage(newImage);

            ScaleTransition show = new ScaleTransition(Duration.millis(150), view);
            show.setFromX(0.0);
            show.setToX(1.0);
            show.play();
        });

        hide.play();
    }
    
    private void revealOpponentCards() {
        if (oppCardsRevealed) return;  // 한 번만

        oppCardsRevealed = true;

        Platform.runLater(() -> {
            if (oppCard1Key != null && !oppCard1Key.isEmpty()) {
                updateCardWithFlip(opCard1Image, oppCard1Key);
            }
            if (oppCard2Key != null && !oppCard2Key.isEmpty()) {
                updateCardWithFlip(opCard2Image, oppCard2Key);
            }
        });
    }


    // ====================== 연결 종료 ======================

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}

        socket = null;
        in = null;
        out = null;
        connected = false;
        canRetry  = false;
        openSent  = false;
        myCard1Opened = false;
        myCard2Opened = false;

        Platform.runLater(() -> {
            startButton.setText("게임 시작");
            startButton.setDisable(false);
            exitButton.setDisable(true);
            setCardBack(card1Image);
            setCardBack(card2Image);
            setCardBack(opCard1Image);
            setCardBack(opCard2Image);
            if (deckImage != null) {
                deckImage.setVisible(true);
                setCardBack(deckImage);
            }
            rankLabel.setText("족보: -");
            resultLabel.setText("결과: -");
            statusLabel.setText("서버에 연결되지 않음");
        });
    }
}
