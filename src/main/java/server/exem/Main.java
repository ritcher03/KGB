package server.exem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class Main {

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static JTextArea chatArea;
    private static JTextField inputField;
    private static JTextField usernameField;
    private static JPasswordField passwordField;
    private static JComboBox<String> roomComboBox;
    private static JLabel onlineCountLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame loginFrame = new JFrame("Přihlášení");
            loginFrame.setSize(300, 150);
            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loginFrame.setLayout(new GridLayout(3, 2));

            JLabel usernameLabel = new JLabel("Uživatelské jméno:");
            usernameField = new JTextField();
            JLabel passwordLabel = new JLabel("Heslo:");
            passwordField = new JPasswordField();
            JButton loginButton = new JButton("Přihlásit se");

            loginFrame.add(usernameLabel);
            loginFrame.add(usernameField);
            loginFrame.add(passwordLabel);
            loginFrame.add(passwordField);
            loginFrame.add(new JLabel()); // Empty cell
            loginFrame.add(loginButton);

            loginButton.addActionListener(e -> {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                try {
                    // Připojení k serveru
                    socket = new Socket("192.168.0.115", 12345);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Odeslání přihlašovacích údajů
                    out.println(username);
                    out.println(password);

                    // Čtení odpovědi ze serveru
                    String serverResponse = in.readLine();
                    if ("Přihlášení selhalo!".equals(serverResponse)) {
                        JOptionPane.showMessageDialog(loginFrame, "Špatné jméno nebo heslo.");
                        socket.close();
                    } else {
                        loginFrame.dispose();
                        createChatWindow();
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    JOptionPane.showMessageDialog(loginFrame, "Server je Offline.");
                }
            });

            loginFrame.setVisible(true);
        });
    }

    private static void createChatWindow() {
        JFrame chatFrame = new JFrame("Chat");
        chatFrame.setSize(600, 400);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        chatFrame.add(scrollPane, BorderLayout.CENTER);

        inputField = new JTextField();
        chatFrame.add(inputField, BorderLayout.SOUTH);

        inputField.addActionListener(e -> sendMessage());

        // Vlákno pro čtení zpráv od serveru
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    if (serverMessage.startsWith("/onlineCount ")) {
                        int count = Integer.parseInt(serverMessage.substring(13));
                        SwingUtilities.invokeLater(() -> onlineCountLabel.setText("Online uživatelé: " + count));
                    } else {
                        chatArea.append(serverMessage + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Panel pro výběr místnosti a online počítadlo
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        onlineCountLabel = new JLabel("Online uživatelé: 0");
        topPanel.add(onlineCountLabel, BorderLayout.NORTH);

        JPanel roomPanel = new JPanel();
        roomPanel.add(new JLabel("Místnost:"));
        roomComboBox = new JComboBox<>(new String[]{"Global1", "Global2", "Global3"});
        roomPanel.add(roomComboBox);
        topPanel.add(roomPanel, BorderLayout.CENTER);

        chatFrame.add(topPanel, BorderLayout.NORTH);

        roomComboBox.addActionListener(e -> {
            String selectedRoom = (String) roomComboBox.getSelectedItem();
            out.println("/room " + selectedRoom); // Notify server of room change
        });

        chatFrame.setVisible(true);
    }

    private static void sendMessage() {
        String message = inputField.getText();
        if (!message.trim().isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }
}