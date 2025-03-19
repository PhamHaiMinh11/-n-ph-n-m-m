import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 9876;
    private static HashSet<ClientHandler> clients = new HashSet<>();
    private static ChatDatabase database = new ChatDatabase();

    public static void main(String[] args) throws Exception {
        System.out.println("Chat server đang chạy trên cổng " + PORT);
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                Socket socket = listener.accept();
                System.out.println("Kết nối mới từ: " + socket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                handler.start();
            }
        } finally {
            listener.close();
        }
    }

    // Gửi tin nhắn đến tất cả các client
    public static void broadcastMessage(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clients) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
        // Lưu tin nhắn vào database
        database.saveMessage("ALL", message);
    }

    // Xóa client khỏi danh sách khi ngắt kết nối
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client đã ngắt kết nối: " + client.getUsername());
    }

    // Kiểm tra tên người dùng đã tồn tại chưa
    public static boolean isUsernameTaken(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    // Lấy lịch sử chat từ database
    public static List<String> getChatHistory() {
        return database.getRecentMessages(10);
    }
}

// ClientHandler.java - Xử lý mỗi kết nối client
class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            // Thiết lập luồng vào ra
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Xử lý đăng nhập
            while (true) {
                out.println("LOGIN");
                username = in.readLine();
                
                if (username == null || username.isEmpty()) {
                    continue;
                }
                
                if (ChatServer.isUsernameTaken(username)) {
                    out.println("ERROR Tên đăng nhập đã được sử dụng. Vui lòng chọn tên khác.");
                } else {
                    break;
                }
            }
            
            out.println("SUCCESS Đăng nhập thành công!");
            
            // Thông báo người dùng mới
            ChatServer.broadcastMessage("SERVER: " + username + " đã tham gia chat", this);
            
            // Gửi lịch sử chat cho người dùng mới
            List<String> history = ChatServer.getChatHistory();
            out.println("HISTORY " + history.size());
            for (String message : history) {
                out.println(message);
            }
            
            // Xử lý tin nhắn
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals("/quit")) {
                    break;
                }
                ChatServer.broadcastMessage(username + ": " + inputLine, null);
            }
        } catch (IOException e) {
            System.out.println("Lỗi xử lý client: " + e.getMessage());
        } finally {
            // Xử lý ngắt kết nối
            try {
                ChatServer.broadcastMessage("SERVER: " + username + " đã rời chat", this);
                ChatServer.removeClient(this);
                socket.close();
            } catch (IOException e) {
                System.out.println("Lỗi đóng socket: " + e.getMessage());
            }
        }
    }
}
