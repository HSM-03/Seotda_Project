package seotda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    // 🔹 로그인 성공 여부 확인
    public boolean login(String userId, String userPw) {
        String sql = "SELECT * FROM user WHERE user_id = ? AND user_pw = ?";

        try (
            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, userId);
            pstmt.setString(2, userPw);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();   // 결과가 있으면 true (로그인 성공)
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;  // 에러 시 일단 실패 처리
        }
    }

    // 🔹 유저 이름 가져오기 (필요하면 사용)
    public String getUserName(String userId) {
        String sql = "SELECT user_name FROM user WHERE user_id = ?";

        try (
            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("user_name");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 🔹 아이디 중복 확인 (회원가입할 때 사용)
    public boolean isIdDuplicate(String userId) {
        String sql = "SELECT user_id FROM user WHERE user_id = ?";

        try (
            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();   // 결과가 있으면 이미 존재하는 ID
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 에러 나면 일단 "중복"으로 보고 가입 못 하게 막기
            return true;
        }
    }

    // 🔹 회원가입 (INSERT)
    public boolean register(String userId, String userPw, String userName) {
        String sql = "INSERT INTO user (user_id, user_pw, user_name) VALUES (?, ?, ?)";

        try (
            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, userId);
            pstmt.setString(2, userPw);
            pstmt.setString(3, userName);

            int rows = pstmt.executeUpdate();   // 영향받은 행 수
            return rows == 1;   // 1줄 들어갔으면 성공

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
