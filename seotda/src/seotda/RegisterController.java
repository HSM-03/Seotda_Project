package seotda;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField idField;
    @FXML private PasswordField pwField;
    @FXML private PasswordField pwCheckField;
    @FXML private TextField nameField;

    private UserDAO userDAO = new UserDAO();

    // "가입" 버튼 클릭
    @FXML
    private void handleRegister() {
        String id = idField.getText().trim();
        String pw = pwField.getText().trim();
        String pwCheck = pwCheckField.getText().trim();
        String name = nameField.getText().trim();

        // 1) 입력값 검증
        if (id.isEmpty() || pw.isEmpty() || pwCheck.isEmpty() || name.isEmpty()) {
            showAlert("입력 오류", "모든 항목을 입력하세요.");
            return;
        }

        // 2) 비밀번호 확인
        if (!pw.equals(pwCheck)) {
            showAlert("비밀번호 불일치", "비밀번호와 비밀번호 확인이 다릅니다.");
            return;
        }

        // 3) 아이디 중복 체크
        if (userDAO.isIdDuplicate(id)) {
            showAlert("아이디 중복", "이미 사용 중인 아이디입니다.");
            return;
        }

        // 4) DB에 INSERT
        boolean ok = userDAO.register(id, pw, name);

        if (ok) {
            showAlert("회원가입 완료", "회원가입이 완료되었습니다.\n로그인 화면에서 로그인하세요.");

            // 현재 창 닫기
            Stage stage = (Stage) idField.getScene().getWindow();
            stage.close();

        } else {
            showAlert("오류", "회원가입 중 문제가 발생했습니다.");
        }
    }

    // "취소" 버튼 클릭
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) idField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
