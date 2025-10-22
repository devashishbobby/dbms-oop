import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfileDialog extends JDialog {

    private int userId;
    private String username;
    private JLabel profilePictureLabel;

    public ProfileDialog(Frame parent, int userId, String username) {
        super(parent, "Profile for " + username, true);
        this.userId = userId;
        this.username = username;

        setSize(600, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(15, 15));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Profile Picture Label
        profilePictureLabel = new JLabel();
        profilePictureLabel.setPreferredSize(new Dimension(100, 100));
        profilePictureLabel.setHorizontalAlignment(JLabel.CENTER);
        profilePictureLabel.setVerticalAlignment(JLabel.CENTER);
        profilePictureLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        topPanel.add(profilePictureLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel usernameLabel = new JLabel("Username: " + this.username);
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        infoPanel.add(usernameLabel);
        
        JButton changePictureButton = new JButton("Change Picture");
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        infoPanel.add(changePictureButton);

        topPanel.add(infoPanel, BorderLayout.CENTER);
        
        JTextArea reviewsArea = new JTextArea("Loading reviews...");
        reviewsArea.setEditable(false);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(reviewsArea), BorderLayout.CENTER);

        changePictureButton.addActionListener(e -> selectAndSaveProfilePicture());

        // Load all profile data
        loadUserProfile(infoPanel, reviewsArea);
    }

    private void loadUserProfile(JPanel infoPanel, JTextArea reviewsArea) {
        // --- CORRECTED: This query no longer asks for the non-existent 'join_date' column ---
        String sql = "SELECT email, profile_picture_path FROM Users WHERE user_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, this.userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                infoPanel.add(new JLabel("Email: " + rs.getString("email")));
                
                // Load the profile picture
                String imagePath = rs.getString("profile_picture_path");
                displayProfilePicture(imagePath);
            }
        } catch (SQLException ex) {
            infoPanel.add(new JLabel("Could not load user details."));
        }
        
        loadUserReviews(reviewsArea);
    }
    
    private void selectAndSaveProfilePicture() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "jpg", "png", "gif", "jpeg");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                File destDir = new File("profile_pics");
                if (!destDir.exists()) {
                    destDir.mkdir();
                }

                String newFileName = this.userId + "_" + selectedFile.getName();
                File destFile = new File(destDir, newFileName);

                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                String dbPath = destFile.getPath().replace("\\", "/");
                updateProfilePicturePath(dbPath);
                displayProfilePicture(dbPath);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not save image.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void updateProfilePicturePath(String path) throws SQLException {
        String sql = "UPDATE Users SET profile_picture_path = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, path);
            pstmt.setInt(2, this.userId);
            pstmt.executeUpdate();
        }
    }

    private void displayProfilePicture(String imagePath) {
        ImageIcon profileIcon;
        if (imagePath != null && new File(imagePath).exists()) {
            profileIcon = new ImageIcon(imagePath);
            Image scaledImage = profileIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            profilePictureLabel.setIcon(new ImageIcon(scaledImage));
            profilePictureLabel.setText(""); // Clear text when image is present
        } else {
            profilePictureLabel.setText("No Image");
            profilePictureLabel.setIcon(null);
        }
    }

    private void loadUserReviews(JTextArea reviewsArea) {
        String sql = "SELECT m.title, r.rating, r.review_text FROM Reviews r JOIN Movies m ON r.movie_id = m.movie_id WHERE r.user_id = ? ORDER BY r.created_at DESC";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.userId);
            ResultSet rs = pstmt.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                sb.append("Movie: ").append(rs.getString("title")).append("\n");
                sb.append("Rating: ").append(rs.getDouble("rating")).append("/5.0\n");
                sb.append("\"").append(rs.getString("review_text")).append("\"\n");
                sb.append("----------------------------------------------------\n");
            }
            if (!found) {
                sb.append("You have not written any reviews yet.");
            }
            reviewsArea.setText(sb.toString());
        } catch (SQLException ex) {
            reviewsArea.setText("Error loading reviews.");
            ex.printStackTrace();
        }
    }
}