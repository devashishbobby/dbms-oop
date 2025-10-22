// Place this file in the 'src' folder
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Arrays;

public class RegisterDialog extends JDialog {
    private JTextField tfUsername, tfEmail;
    private JPasswordField pfPassword, pfConfirmPassword;

    public RegisterDialog(Dialog parent) {
        super(parent, "Register New User", true);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.insets = new Insets(5, 5, 5, 5);
        cs.gridx = 0; cs.gridy = 0; cs.gridwidth = 1; panel.add(new JLabel("Username:"), cs);
        cs.gridx = 1; cs.gridy = 0; cs.gridwidth = 2; tfUsername = new JTextField(20); panel.add(tfUsername, cs);
        cs.gridx = 0; cs.gridy = 1; cs.gridwidth = 1; panel.add(new JLabel("Email:"), cs);
        cs.gridx = 1; cs.gridy = 1; cs.gridwidth = 2; tfEmail = new JTextField(20); panel.add(tfEmail, cs);
        cs.gridx = 0; cs.gridy = 2; cs.gridwidth = 1; panel.add(new JLabel("Password:"), cs);
        cs.gridx = 1; cs.gridy = 2; cs.gridwidth = 2; pfPassword = new JPasswordField(); panel.add(pfPassword, cs);
        cs.gridx = 0; cs.gridy = 3; cs.gridwidth = 1; panel.add(new JLabel("Confirm Password:"), cs);
        cs.gridx = 1; cs.gridy = 3; cs.gridwidth = 2; pfConfirmPassword = new JPasswordField(); panel.add(pfConfirmPassword, cs);
        JButton btnRegister = new JButton("Submit");
        btnRegister.addActionListener(e -> performRegistration());
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        JPanel bp = new JPanel();
        bp.add(btnRegister);
        bp.add(btnCancel);
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }
    private void performRegistration() {
        String username = tfUsername.getText().trim();
        if (username.isEmpty() || tfEmail.getText().trim().isEmpty() || pfPassword.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        if (!Arrays.equals(pfPassword.getPassword(), pfConfirmPassword.getPassword())) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        String sql = "INSERT INTO Users (user_id, username, email, password_hash) VALUES (user_id_seq.NEXTVAL, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, tfEmail.getText().trim());
            pstmt.setString(3, new String(pfPassword.getPassword()));
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Registration successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1) JOptionPane.showMessageDialog(this, "Username or email already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            else JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}