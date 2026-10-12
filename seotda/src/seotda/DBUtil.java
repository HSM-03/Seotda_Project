package seotda;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {

    // DB 접속 정보 (너 환경에 맞게 수정!)
    private static final String URL =
            "jdbc:mysql://localhost:3306/seotda_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
    private static final String USER = "your_ID";      // MySQL 아이디
    private static final String PASSWORD = "your_PASSWORD";  // MySQL 비밀번호

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");  // 드라이버 로딩
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
