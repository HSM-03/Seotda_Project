package seotda;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    // 🔹 로그인한 유저 이름 저장용 (어디서든 꺼내 쓰기)
    private static String loggedUserName;

    // 🔹 로그인한 유저 이름 저장
    public static void setLoggedUserName(String name) {
        loggedUserName = name;
    }

    // 🔹 로그인한 유저 이름 가져오기
    public static String getLoggedUserName() {
        return loggedUserName;
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLogin();   // 앱 실행 시 로그인 화면 먼저 열림
    }

    // ====================== 화면 전환 메서드 ======================

    // 로그인 화면 열기
    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("login-view.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 700);

        primaryStage.setScene(scene);
        primaryStage.setTitle("로그인");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // 메인 게임 화면 열기
    public static void showMain() throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(loader.load(), 650, 850); //800 450 

        // CSS 적용 (선택)
        try {
            scene.getStylesheets().add(
                MainApp.class.getResource("style.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("style.css 로딩 실패: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.setTitle("Seotda Game");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // 애플리케이션 시작
    public static void main(String[] args) {
        launch(args);
    }
}
