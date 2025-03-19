import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    // UI components
    private JFrame frame;
    private JTextField messageField;
    private JTextArea chatArea;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    
    // Network components
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String serverAddress;
    
    public ChatClient() {
        // Tạo giao diện
        frame = new JFrame("Ứng dụng Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        
        // Panel chính
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Vùng hiển thị tin nhắn
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        
        // Vùng nhập tin nhắn
        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        JButton sendButton = new JButton("Gửi");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // Danh sách người dùng
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        
        // Thêm các thành phần vào panel chính
        mainPanel.add(chatScroll, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(userScroll, BorderLayout.EAST);
        
        // Thêm panel chính vào frame
        frame.getContentPane().add(mainPanel);
        
        // Hiển thị frame
        frame.setVisible(true);
        
        // Yêu cầu thông tin đăng nhập
        promptForLogin();
    }
    
    private void promptForLogin() {
        JPanel loginPanel = new JPanel(new GridLayout(2, 2));
        JTextField serverField = new JTextField("localhost");
        JTextField usernameField = new JTextField();
        
        loginPanel.add(new JLabel("Địa chỉ server:"));
        loginPanel.add(serverField);
        loginPanel.add(new JLabel("Tên đăng nhập:"));
        loginPanel.add(usernameField);
        
        int result = JOptionPane.showConfirmDialog(frame, loginPanel, "Đăng nhập", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            serverAddress = serverField.getText().trim();
            username = usernameField.getText().trim();
            
            if (!serverAddress.isEmpty() && !username.isEmpty()) {
                connectToServer();
            } else {
                JOptionPane.showMessageDialog(frame, "Vui lòng nhập đầy đủ thông tin!");
                promptForLogin();
            }
        } else {
            System.exit(0);
        }
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(serverAddress, 9876);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Bắt đầu thread đọc tin nhắn từ server
            new Thread(new MessageReader()).start();
            
            messageField.setEnabled(true);
        } catch (Exception e) {
            chatArea.append("Không thể kết nối đến server: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(frame, "Không thể kết nối đến server!");
            promptForLogin();
        }
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            messageField.setText("");
        }
    }
    
    // Thread đọc tin nhắn từ server
    private class MessageReader implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("LOGIN")) {
                        out.println(username);
                    } else if (line.startsWith("ERROR")) {
                        JOptionPane.showMessageDialog(frame, line.substring(6));
                        frame.dispose();
                        new ChatClient();
                        break;
                    } else if (line.startsWith("SUCCESS")) {
                        chatArea.append("=== " + line.substring(8) + " ===\n");
                    } else if (line.startsWith("HISTORY")) {
                        int count = Integer.parseInt(line.substring(8));
                        chatArea.append("=== Lịch sử tin nhắn ===\n");
                        for (int i = 0; i < count; i++) {
                            chatArea.append(in.readLine() + "\n");
                        }
                        chatArea.append("=== Kết thúc lịch sử ===\n");
                    } else {
                        chatArea.append(line + "\n");
                    }
                }
            } catch (IOException e) {
                chatArea.append("Mất kết nối đến server!\n");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Bỏ qua
                }
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClient();
            }
        });
    }
}
