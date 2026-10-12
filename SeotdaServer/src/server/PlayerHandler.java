package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PlayerHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Player player;

    public PlayerHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public void setPlayer(Player p) {
        this.player = p;
    }

    public Player getPlayer() {
        return player;
    }

    // ===== 서버 → 클라이언트 =====
    public void send(String msg) throws IOException {
        out.writeUTF(msg);
        out.flush();
    }

    // ===== 클라이언트 → 서버 =====
    public String read() throws IOException {
        return in.readUTF();
    }

    // 다시하기 여부 읽기 편하게 헬퍼 메서드
    public boolean wantRetry() throws IOException {
        String cmd = read();
        System.out.println("클라이언트로부터 다시하기 응답: " + cmd);
        return "RETRY".equalsIgnoreCase(cmd);   // ✅ RETRY면 true, 그 외( EXIT 등 )은 false
    }
    
    public void close() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
	