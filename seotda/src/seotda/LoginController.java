package seotda;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField idField;
    @FXML private PasswordField pwField;

    private MediaPlayer bgmPlayer;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void initialize() {
        startBgm();
    }

    @FXML
    private void handleLogin() {
        String id = idField.getText();
        String pw = pwField.getText();

        if (id.isEmpty() || pw.isEmpty()) {
            showAlert("입력 오류", "아이디/비밀번호를 입력하세요.");
            return;
        }

        boolean ok = userDAO.login(id, pw);

        if (ok) {
            // 🔹 DB에서 유저 이름 가져오기
            String userName = userDAO.getUserName(id);
            if (userName == null || userName.isEmpty()) {
                // 혹시 이름이 null이면 아이디라도 넣어두기
                userName = id;
            }

            // 🔹 MainApp에 로그인한 유저 이름 저장
            MainApp.setLoggedUserName(userName);

            // 🔹 메인 화면으로 넘어가기 전에 BGM 끄기
            if (bgmPlayer != null) {
                bgmPlayer.stop();
                bgmPlayer.dispose();
            }

            try {
                MainApp.showMain();   // 메인 게임 화면으로 전환
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            showAlert("로그인 실패", "아이디 또는 비밀번호가 틀렸습니다.");
        }
    }

    // ⭐ 회원가입 버튼 눌렀을 때 실행
    @FXML
    private void handleOpenRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/seotda/register-view.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("회원가입");

            // ✅ 여기서 회원가입 창 크기를 로그인과 똑같이 맞춰줌 (여백 제거 포인트)
            Scene scene = new Scene(root, 1000, 700);
            stage.setScene(scene);

            // 회원가입 창을 로그인 창 위에 모달로 띄우기
            stage.initModality(Modality.WINDOW_MODAL);
            Stage parent = (Stage) idField.getScene().getWindow();
            stage.initOwner(parent);

            stage.setResizable(false);
            stage.centerOnScreen();   // (선택) 화면 중앙 정렬

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("오류", "회원가입 창을 열 수 없습니다.");
        }
    }

    private void startBgm() {
        try {
            String path = getClass()
                    .getResource("/seotda/Sound/LoginBGM.mp3")
                    .toExternalForm();

            Media media = new Media(path);
            bgmPlayer = new MediaPlayer(media);

            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(0.3);
            bgmPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("로그인 BGM 로드 실패");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
