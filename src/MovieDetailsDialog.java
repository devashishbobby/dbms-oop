import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MovieDetailsDialog extends JDialog {

    private int movieId;
    private int currentUserId;
    private JPanel reviewsPanel;
    private JLabel posterLabel;
    private String currentPosterUrl;
    private JLabel averageScoreLabel;
    private JLabel genresLabel;
    private JTextArea synopsisArea;
    private Map<Integer, JButton> likeButtons = new HashMap<>();
    private JLabel externalScoreLabel;
    private JLabel runtimeLabel;
    private JLabel ageRatingLabel;
    private JLabel episodesLabel;
    private JLabel statusLabel;
    private JLabel studiosLabel;
    private JLabel producersLabel;

    public MovieDetailsDialog(JFrame parent, int movieId, String movieTitle, int currentUserId) {
        super(parent, "Details for: " + movieTitle, true);
        this.movieId = movieId;
        this.currentUserId = currentUserId;

        setSize(850, 700);
        setLocationRelativeTo(parent);

        // UI Components
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topDetailsPanel = new JPanel(new BorderLayout(15, 15));
        posterLabel = new JLabel("Loading poster...");
        posterLabel.setPreferredSize(new Dimension(220, 310));
        posterLabel.setHorizontalAlignment(JLabel.CENTER);
        posterLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        topDetailsPanel.add(posterLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new BorderLayout(10, 10));
        JPanel metaInfoPanel = new JPanel();
        metaInfoPanel.setLayout(new BoxLayout(metaInfoPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("<html><body style='width: 450px;'><b>" + movieTitle + "</b></body></html>");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        metaInfoPanel.add(titleLabel);
        metaInfoPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        averageScoreLabel = new JLabel("Avg. User Rating: Loading...");
        metaInfoPanel.add(averageScoreLabel);
        externalScoreLabel = new JLabel("External Score: Loading...");
        metaInfoPanel.add(externalScoreLabel);
        metaInfoPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Initialize all labels
        genresLabel = new JLabel();
        runtimeLabel = new JLabel();
        ageRatingLabel = new JLabel();
        episodesLabel = new JLabel();
        statusLabel = new JLabel();
        studiosLabel = new JLabel();
        producersLabel = new JLabel();

        // Add them to the panel
        metaInfoPanel.add(genresLabel);
        metaInfoPanel.add(runtimeLabel);
        metaInfoPanel.add(ageRatingLabel);
        metaInfoPanel.add(episodesLabel);
        metaInfoPanel.add(statusLabel);
        metaInfoPanel.add(studiosLabel);
        metaInfoPanel.add(producersLabel);
        
        infoPanel.add(metaInfoPanel, BorderLayout.NORTH);

        synopsisArea = new JTextArea("Synopsis: Loading...");
        synopsisArea.setWrapStyleWord(true);
        synopsisArea.setLineWrap(true);
        synopsisArea.setEditable(false);
        synopsisArea.setBackground(getBackground());
        JScrollPane synopsisScrollPane = new JScrollPane(synopsisArea);
        synopsisScrollPane.setBorder(BorderFactory.createTitledBorder("Synopsis / Overview"));
        infoPanel.add(synopsisScrollPane, BorderLayout.CENTER);

        topDetailsPanel.add(infoPanel, BorderLayout.CENTER);
        contentPanel.add(topDetailsPanel, BorderLayout.NORTH);

        reviewsPanel = new JPanel();
        reviewsPanel.setLayout(new BoxLayout(reviewsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(reviewsPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("User Reviews"));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JButton addReviewButton = new JButton("Add Your Review");
        JButton editPosterButton = new JButton("Edit Poster");
        JButton closeButton = new JButton("Close");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addReviewButton);
        buttonPanel.add(editPosterButton);
        buttonPanel.add(closeButton);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Action Listeners
        closeButton.addActionListener(e -> dispose());
        addReviewButton.addActionListener(e -> {
            AddReviewDialog addReviewDlg = new AddReviewDialog(this, this.movieId, this.currentUserId);
            addReviewDlg.setVisible(true);
            if (addReviewDlg.isReviewSubmitted()) {
                loadMovieDetails();
            }
        });
        editPosterButton.addActionListener(e -> {
            String newUrl = (String) JOptionPane.showInputDialog(this, "Enter new poster URL:", "Edit Poster", JOptionPane.PLAIN_MESSAGE, null, null, this.currentPosterUrl);
            if (newUrl != null) {
                updatePosterLink(newUrl.trim());
            }
        });

        loadMovieDetails();
    }

    private void loadMovieDetails() {
        String sql = "SELECT m.*, AVG(r.rating) as avg_rating FROM Movies m LEFT JOIN Reviews r ON m.movie_id = r.movie_id WHERE m.movie_id = ? " +
                     "GROUP BY m.movie_id, m.title, m.release_year, m.director, m.poster_link, m.media_type, m.genres, m.synopsis, m.runtime, m.age_rating, m.external_score, m.episodes, m.status, m.studios, m.producers";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.movieId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                this.currentPosterUrl = rs.getString("poster_link");
                loadPosterImage(this.currentPosterUrl);
                
                double avgRating = rs.getDouble("avg_rating");
                averageScoreLabel.setText(avgRating > 0 ? String.format("Avg. User Rating: %.2f / 5.0", avgRating) : "Avg. User Rating: Not rated");
                
                double extScore = rs.getDouble("external_score");
                externalScoreLabel.setText(extScore > 0 ? String.format("External Score: %.2f / 10.0", extScore) : "External Score: N/A");
                externalScoreLabel.setVisible(extScore > 0);

                setLabelText(genresLabel, "Genres", rs.getString("genres"), true);
                setLabelText(runtimeLabel, "Runtime", rs.getString("runtime"), false);
                setLabelText(ageRatingLabel, "Age Rating", rs.getString("age_rating"), false);
                synopsisArea.setText(rs.getString("synopsis"));
                synopsisArea.setCaretPosition(0);

                int episodes = rs.getInt("episodes");
                setLabelText(episodesLabel, "Episodes", episodes > 0 ? String.valueOf(episodes) : null, false);
                setLabelText(statusLabel, "Status", rs.getString("status"), false);
                setLabelText(studiosLabel, "Studios", rs.getString("studios"), false);
                setLabelText(producersLabel, "Producers", rs.getString("producers"), true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load details. See terminal.", "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        loadMovieReviews();
    }
    
    private void setLabelText(JLabel label, String prefix, String value, boolean useHtml) {
        if (value != null && !value.trim().isEmpty()) {
            label.setText(useHtml ? "<html><body style='width: 450px;'><b>" + prefix + ":</b> " + value + "</body></html>" : prefix + ": " + value);
            label.setVisible(true);
        } else {
            label.setVisible(false);
        }
    }
    
    private void updatePosterLink(String newUrl) {
        String sql = "UPDATE Movies SET poster_link = ? WHERE movie_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newUrl.isEmpty() ? null : newUrl);
            pstmt.setInt(2, this.movieId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Poster updated!");
            this.currentPosterUrl = newUrl;
            loadPosterImage(this.currentPosterUrl);
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Failed to update poster.", "DB Error", JOptionPane.ERROR_MESSAGE); }
    }
    
    private void loadPosterImage(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            posterLabel.setText("No Poster"); posterLabel.setIcon(null); return;
        }
        posterLabel.setText("Loading..."); posterLabel.setIcon(null);
        new SwingWorker<ImageIcon, Void>() {
            protected ImageIcon doInBackground() throws Exception {
                Image image = ImageIO.read(new URL(urlString));
                return image != null ? new ImageIcon(image.getScaledInstance(220, 310, Image.SCALE_SMOOTH)) : null;
            }
            protected void done() {
                try {
                    ImageIcon imageIcon = get();
                    posterLabel.setText(imageIcon == null ? "No Poster" : "");
                    posterLabel.setIcon(imageIcon);
                } catch (Exception e) { posterLabel.setText("Load Failed"); }
            }
        }.execute();
    }

    private void loadMovieReviews() {
        reviewsPanel.removeAll();
        likeButtons.clear();
        String sql = "SELECT r.review_id, u.username, r.rating, r.review_text, r.user_id, " +
                     "(SELECT COUNT(*) FROM Review_Likes rl WHERE rl.review_id = r.review_id) as like_count, " +
                     "(SELECT COUNT(*) FROM Review_Likes rl WHERE rl.review_id = r.review_id AND rl.user_id = ?) as user_liked " +
                     "FROM Reviews r JOIN Users u ON r.user_id = u.user_id " +
                     "WHERE r.movie_id = ? ORDER BY like_count DESC, r.created_at DESC";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.currentUserId);
            pstmt.setInt(2, this.movieId);
            ResultSet rs = pstmt.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                int reviewId = rs.getInt("review_id");
                String username = rs.getString("username");
                double rating = rs.getDouble("rating");
                String reviewText = rs.getString("review_text");
                int reviewUserId = rs.getInt("user_id");
                int likeCount = rs.getInt("like_count");
                boolean userHasLiked = rs.getInt("user_liked") > 0;
                JPanel reviewCard = new JPanel(new BorderLayout(5, 5));
                reviewCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), new EmptyBorder(10, 5, 10, 5)));
                String headerText = String.format("<html><b>%s</b> (Rating: %.1f/5.0)</html>", username, rating);
                reviewCard.add(new JLabel(headerText), BorderLayout.NORTH);
                JTextArea reviewContent = new JTextArea(reviewText);
                reviewContent.setWrapStyleWord(true);
                reviewContent.setLineWrap(true);
                reviewContent.setEditable(false);
                reviewContent.setBackground(getBackground());
                reviewCard.add(reviewContent, BorderLayout.CENTER);
                JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton likeButton = new JButton(userHasLiked ? "Unlike" : "Like");
                JLabel likesLabel = new JLabel(String.valueOf(likeCount));
                if (reviewUserId == this.currentUserId) {
                    likeButton.setEnabled(false);
                } else {
                    likeButton.addActionListener(e -> toggleLike(reviewId, userHasLiked));
                }
                actionsPanel.add(likesLabel);
                actionsPanel.add(likeButton);
                if (reviewUserId == this.currentUserId) {
                    JButton editButton = new JButton("Edit");
                    JButton deleteButton = new JButton("Delete");
                    actionsPanel.add(editButton);
                    actionsPanel.add(deleteButton);
                    editButton.addActionListener(e -> {
                        AddReviewDialog dlg = new AddReviewDialog(this, movieId, currentUserId, reviewId, rating, reviewText);
                        dlg.setVisible(true);
                        if (dlg.isReviewSubmitted()) loadMovieDetails();
                    });
                    deleteButton.addActionListener(e -> {
                        if (JOptionPane.showConfirmDialog(this, "Are you sure?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            deleteReview(reviewId);
                        }
                    });
                }
                reviewCard.add(actionsPanel, BorderLayout.SOUTH);
                reviewsPanel.add(reviewCard);
            }
            if (!found) {
                reviewsPanel.add(new JLabel("No reviews yet. Be the first to write one!"));
            }
            reviewsPanel.revalidate();
            reviewsPanel.repaint();
        } catch (SQLException ex) {
            reviewsPanel.add(new JLabel("Error loading reviews."));
        }
    }
    
    private void toggleLike(int reviewId, boolean userHasLiked) {
        String sql = userHasLiked ? "DELETE FROM Review_Likes WHERE review_id = ? AND user_id = ?" : "INSERT INTO Review_Likes (like_id, review_id, user_id) VALUES (like_id_seq.NEXTVAL, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reviewId);
            pstmt.setInt(2, this.currentUserId);
            pstmt.executeUpdate();
            loadMovieReviews();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Action failed.", "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteReview(int reviewId) {
        String sql = "DELETE FROM Reviews WHERE review_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reviewId);
            if (pstmt.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(this, "Review deleted.", "DB Error", JOptionPane.ERROR_MESSAGE);
                loadMovieDetails();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting review.", "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}