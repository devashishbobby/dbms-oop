import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddReviewDialog extends JDialog {

    private int movieId;
    private int userId;
    private int reviewId = -1; // -1 indicates a new review. A positive value means we are editing.
    private boolean reviewSubmitted = false;

    private JComboBox<Double> ratingComboBox;
    private JTextArea reviewTextArea;

    // --- CONSTRUCTOR FOR ADDING A NEW REVIEW ---
    public AddReviewDialog(Dialog parent, int movieId, int userId) {
        super(parent, "Write a Review", true);
        this.movieId = movieId;
        this.userId = userId;
        initComponents();
    }

    // --- NEW: CONSTRUCTOR FOR EDITING AN EXISTING REVIEW ---
    public AddReviewDialog(Dialog parent, int movieId, int userId, int reviewId, double currentRating, String currentText) {
        super(parent, "Edit Your Review", true);
        this.movieId = movieId;
        this.userId = userId;
        this.reviewId = reviewId; // Set the review ID for editing
        initComponents();

        // Pre-fill the fields with the existing review data
        ratingComboBox.setSelectedItem(currentRating);
        reviewTextArea.setText(currentText);
    }
    
    // Helper method to build the UI, used by both constructors
    private void initComponents() {
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new BorderLayout(5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        formPanel.add(new JLabel("Your Rating:"), BorderLayout.WEST);
        Double[] ratings = {5.0, 4.5, 4.0, 3.5, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5};
        ratingComboBox = new JComboBox<>(ratings);
        formPanel.add(ratingComboBox, BorderLayout.CENTER);

        reviewTextArea = new JTextArea();
        reviewTextArea.setWrapStyleWord(true);
        reviewTextArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(reviewTextArea);

        JButton submitButton = new JButton("Submit");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        cancelButton.addActionListener(e -> dispose());
        submitButton.addActionListener(e -> submitReview());
    }

    private void submitReview() {
        Double rating = (Double) ratingComboBox.getSelectedItem();
        String reviewText = reviewTextArea.getText().trim();

        if (reviewText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please write something in your review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- NEW: Smart logic to decide whether to INSERT or UPDATE ---
        if (this.reviewId == -1) {
            // This is a new review, so we INSERT.
            insertNewReview(rating, reviewText);
        } else {
            // This is an existing review, so we UPDATE.
            updateExistingReview(rating, reviewText);
        }
    }
    
    private void insertNewReview(double rating, String reviewText) {
        String checkSql = "SELECT 1 FROM Reviews WHERE user_id = ? AND movie_id = ?";
        String insertSql = "INSERT INTO Reviews (review_id, rating, review_text, user_id, movie_id) VALUES (review_id_seq.NEXTVAL, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnector.getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, this.userId);
                checkStmt.setInt(2, this.movieId);
                if (checkStmt.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "You have already reviewed this movie.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setDouble(1, rating);
                insertStmt.setString(2, reviewText);
                insertStmt.setInt(3, this.userId);
                insertStmt.setInt(4, this.movieId);
                insertStmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Review submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                reviewSubmitted = true;
                dispose();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateExistingReview(double rating, String reviewText) {
        String sql = "UPDATE Reviews SET rating = ?, review_text = ? WHERE review_id = ?";
        
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setDouble(1, rating);
            pstmt.setString(2, reviewText);
            pstmt.setInt(3, this.reviewId);
            
            pstmt.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Review updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            reviewSubmitted = true;
            dispose();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isReviewSubmitted() {
        return reviewSubmitted;
    }
}