import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginDialog extends JDialog {
    private JTextField tfUsername;
    private JPasswordField pfPassword;
    private boolean succeeded;
    private int loggedInUserId;
    private String loggedInUsername;
    // --- NEW: Field to store the profile picture path ---
    private String profilePicPath;

    public LoginDialog(JFrame parent) {
        super(parent, "Login", true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;

        cs.gridx = 0; cs.gridy = 0; cs.gridwidth = 1; panel.add(new JLabel("Username:"), cs);
        cs.gridx = 1; cs.gridy = 0; cs.gridwidth = 2; tfUsername = new JTextField(20); panel.add(tfUsername, cs);
        cs.gridx = 0; cs.gridy = 1; cs.gridwidth = 1; panel.add(new JLabel("Password:"), cs);
        cs.gridx = 1; cs.gridy = 1; cs.gridwidth = 2; pfPassword = new JPasswordField(); panel.add(pfPassword, cs);

        JButton btnLogin = new JButton("Login");
        btnLogin.addActionListener(this::performLogin);
        JButton btnRegister = new JButton("Register");
        btnRegister.addActionListener(e -> new RegisterDialog(this).setVisible(true));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JPanel bp = new JPanel();
        bp.add(btnLogin);
        bp.add(btnRegister);
        bp.add(btnCancel);

        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void performLogin(ActionEvent e) {
        String username = tfUsername.getText().trim();
        String password = new String(pfPassword.getPassword());

        // --- MODIFIED: The SQL query now also fetches the profile picture path ---
        String sql = "SELECT user_id, profile_picture_path FROM Users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                this.loggedInUserId = rs.getInt("user_id");
                this.loggedInUsername = username;
                // --- NEW: Store the fetched path ---
                this.profilePicPath = rs.getString("profile_picture_path");
                this.succeeded = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
                this.succeeded = false;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            this.succeeded = false;
            ex.printStackTrace();
        }
    }

    public boolean isSucceeded() { return succeeded; }
    public int getLoggedInUserId() { return loggedInUserId; }
    public String getLoggedInUsername() { return loggedInUsername; }
    // --- NEW: Getter method for the path ---
    public String getProfilePicPath() { return profilePicPath; }
}