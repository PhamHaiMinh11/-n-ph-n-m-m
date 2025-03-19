// ChatDatabase.java - Phần Database lưu trữ tin nhắn
import java.sql.*;
import java.util.*;

public class ChatDatabase {
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:chatapp.db";
    
    public ChatDatabase() {
        try {
            // Kết nối database
            connection = DriverManager.getConnection(DB_URL);
            
            // Tạo bảng nếu chưa tồn tại
            Statement stmt = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                          "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                          "recipient TEXT NOT NULL," +
                          "content TEXT NOT NULL," +
                          "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(sql);
            System.out.println("Database đã được khởi tạo");
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối database: " + e.getMessage());
        }
    }
    
    // Lưu tin nhắn vào database
    public void saveMessage(String recipient, String content) {
        try {
            String sql = "INSERT INTO messages (recipient, content) VALUES (?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, recipient);
            pstmt.setString(2, content);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Lỗi lưu tin nhắn: " + e.getMessage());
        }
    }
    
    // Lấy tin nhắn gần đây từ database
    public List<String> getRecentMessages(int limit) {
        List<String> messages = new ArrayList<>();
        try {
            String sql = "SELECT content FROM messages WHERE recipient = 'ALL' ORDER BY timestamp DESC LIMIT ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(rs.getString("content"));
            }
            Collections.reverse(messages); // Hiển thị theo thứ tự cũ đến mới
        } catch (SQLException e) {
            System.out.println("Lỗi đọc tin nhắn: " + e.getMessage());
        }
        return messages;
    }
    
    // Đóng kết nối database
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Lỗi đóng kết nối database: " + e.getMessage());
        }
    }
}
