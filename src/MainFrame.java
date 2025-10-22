import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainFrame extends JFrame {

    private int currentUserId;
    private String currentUsername;

    private JTable movieTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel profilePictureLabel;
    private JComboBox<String> mediaTypeComboBox;
    private JComboBox<String> genreComboBox;

    public MainFrame(int userId, String username, String profilePicPath) {
        this.currentUserId = userId;
        this.currentUsername = username;

        setTitle("FilmFolio");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] columnNames = {"Title", "Year", "Director", "Avg. Rating", "Action"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) {
                    return Double.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        movieTable = new JTable(tableModel);
        movieTable.setAutoCreateRowSorter(true);

        movieTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        movieTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));

        movieTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = movieTable.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = movieTable.convertRowIndexToModel(viewRow);
                        String movieTitle = (String) tableModel.getValueAt(modelRow, 0);
                        String selectedType = getSelectedMediaTypeForDB();
                        int movieId = getMovieIdByTitle(movieTitle, selectedType);
                        
                        if (movieId != -1) {
                            MovieDetailsDialog detailsDialog = new MovieDetailsDialog(MainFrame.this, movieId, movieTitle, currentUserId);
                            detailsDialog.setVisible(true);
                        }
                    }
                }
            }
        });

        // UI Setup
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        JPanel searchPanel = new JPanel();
        JButton fetchAllButton = new JButton("Fetch / Filter");
        JButton viewWatchlistButton = new JButton("View My Watchlist");
        JButton addMovieButton = new JButton("Add New Media");
        JButton profileButton = new JButton("My Profile");
        JButton logoutButton = new JButton("Logout");
        buttonPanel.add(fetchAllButton);
        buttonPanel.add(viewWatchlistButton);
        buttonPanel.add(addMovieButton);
        buttonPanel.add(profileButton);
        buttonPanel.add(logoutButton);
        searchField = new JTextField(15);
        JButton searchButton = new JButton("Search");
        JButton clearButton = new JButton("Clear");
        String[] mediaTypes = {"Movies", "Anime"};
        mediaTypeComboBox = new JComboBox<>(mediaTypes);
        genreComboBox = new JComboBox<>();
        populateGenreFilter();
        searchPanel.add(new JLabel("Category:"));
        searchPanel.add(mediaTypeComboBox);
        searchPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        searchPanel.add(new JLabel("Genre:"));
        searchPanel.add(genreComboBox);
        searchPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        searchPanel.add(new JLabel("Search Title:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);
        topPanel.add(buttonPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(movieTable), BorderLayout.CENTER);
        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        profilePictureLabel = new JLabel();
        profilePictureLabel.setPreferredSize(new Dimension(60, 60));
        profilePictureLabel.setHorizontalAlignment(JLabel.CENTER);
        profilePictureLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        profilePictureLabel.setToolTipText("Click to view your profile");
        profilePictureLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel welcomeLabel = new JLabel("Welcome, " + this.currentUsername);
        welcomeLabel.setHorizontalAlignment(JLabel.CENTER);
        westPanel.add(profilePictureLabel, BorderLayout.NORTH);
        westPanel.add(welcomeLabel, BorderLayout.CENTER);
        mainPanel.add(westPanel, BorderLayout.WEST);
        this.add(mainPanel);

        // Action Listeners
        fetchAllButton.addActionListener(e -> fetchAllMedia());
        viewWatchlistButton.addActionListener(e -> viewWatchlist());
        searchButton.addActionListener(e -> performSearch());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            genreComboBox.setSelectedItem("All Genres");
        });
        addMovieButton.addActionListener(e -> {
            AddMovieDialog addDlg = new AddMovieDialog(this);
            addDlg.setVisible(true);
            if (addDlg.isMovieAdded()) fetchAllMedia();
        });
        profileButton.addActionListener(e -> openProfileDialog());
        logoutButton.addActionListener(e -> {
            this.dispose();
            main(null);
        });
        profilePictureLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openProfileDialog();
            }
        });

        genreComboBox.addActionListener(e -> fetchAllMedia());
        mediaTypeComboBox.addActionListener(e -> fetchAllMedia());
        
        loadProfileThumbnail(profilePicPath);
        fetchAllMedia(); 
    }
    
    // --- METHOD MODIFIED to include a blocklist ---
    private void populateGenreFilter() {
        // Create a set of lowercase genre names to block
        Set<String> blockedGenres = new HashSet<>(Arrays.asList(
            "boys love", "ecchi", "erotica", "girls love", "hentai"
        ));

        Set<String> genres = new HashSet<>();
        String sql = "SELECT genres FROM Movies WHERE genres IS NOT NULL";
        
        try (Connection conn = DatabaseConnector.getConnection(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String[] genreArray = rs.getString("genres").split("\\s*,\\s*");
                for (String genre : genreArray) {
                    String trimmedGenre = genre.trim();
                    // Check if the trimmed, lowercase genre is in the blocklist
                    if (!trimmedGenre.isEmpty() && !blockedGenres.contains(trimmedGenre.toLowerCase())) {
                        genres.add(trimmedGenre);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        List<String> sortedGenres = new ArrayList<>(genres);
        Collections.sort(sortedGenres);
        
        genreComboBox.addItem("All Genres");
        for (String genre : sortedGenres) {
            genreComboBox.addItem(genre);
        }
    }
    
    private String getSelectedMediaTypeForDB() {
        return "Movies".equals(mediaTypeComboBox.getSelectedItem()) ? "movie" : "anime";
    }

    private void fetchAllMedia() {
        String selectedType = getSelectedMediaTypeForDB();
        String selectedGenre = (String) genreComboBox.getSelectedItem();
        tableModel.setRowCount(0);

        String sql;
        if (selectedGenre == null || "All Genres".equals(selectedGenre)) {
            sql = "SELECT m.title, m.release_year, m.director, " +
                  "(SELECT AVG(r.rating) FROM Reviews r WHERE r.movie_id = m.movie_id) as avg_rating " +
                  "FROM Movies m WHERE m.media_type = ? ORDER BY m.title";
        } else {
            sql = "SELECT m.title, m.release_year, m.director, " +
                  "(SELECT AVG(r.rating) FROM Reviews r WHERE r.movie_id = m.movie_id) as avg_rating " +
                  "FROM Movies m WHERE m.media_type = ? AND m.genres LIKE ? ORDER BY m.title";
        }
        
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, selectedType);
            if (selectedGenre != null && !"All Genres".equals(selectedGenre)) {
                pstmt.setString(2, "%" + selectedGenre + "%");
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                double avgRating = rs.getDouble("avg_rating");
                Double ratingObject = rs.wasNull() ? null : avgRating;
                tableModel.addRow(new Object[]{rs.getString("title"), rs.getInt("release_year"), rs.getString("director"), ratingObject, "Add"});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            fetchAllMedia();
            return;
        }
        
        String selectedType = getSelectedMediaTypeForDB();
        String selectedGenre = (String) genreComboBox.getSelectedItem();
        tableModel.setRowCount(0);

        String sql;
        if (selectedGenre == null || "All Genres".equals(selectedGenre)) {
            sql = "SELECT m.title, m.release_year, m.director, " +
                  "(SELECT AVG(r.rating) FROM Reviews r WHERE r.movie_id = m.movie_id) as avg_rating " +
                  "FROM Movies m WHERE m.media_type = ? AND UPPER(m.title) LIKE UPPER(?) ORDER BY m.title";
        } else {
            sql = "SELECT m.title, m.release_year, m.director, " +
                  "(SELECT AVG(r.rating) FROM Reviews r WHERE r.movie_id = m.movie_id) as avg_rating " +
                  "FROM Movies m WHERE m.media_type = ? AND UPPER(m.title) LIKE UPPER(?) AND m.genres LIKE ? ORDER BY m.title";
        }

        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, selectedType);
            pstmt.setString(2, "%" + searchTerm + "%");
            if (selectedGenre != null && !"All Genres".equals(selectedGenre)) {
                pstmt.setString(3, "%" + selectedGenre + "%");
            }
            ResultSet rs = pstmt.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                double avgRating = rs.getDouble("avg_rating");
                Double ratingObject = rs.wasNull() ? null : avgRating;
                tableModel.addRow(new Object[]{rs.getString("title"), rs.getInt("release_year"), rs.getString("director"), ratingObject, "Add"});
            }
            if (!found) {
                tableModel.addRow(new Object[]{"No items found matching your search.", null, null, null, ""});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openProfileDialog() {
        ProfileDialog profileDlg = new ProfileDialog(this, currentUserId, currentUsername);
        profileDlg.setVisible(true);
        reloadProfileThumbnail();
    }
    
    private void reloadProfileThumbnail() {
        String sql = "SELECT profile_picture_path FROM Users WHERE user_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                loadProfileThumbnail(rs.getString("profile_picture_path"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    private void loadProfileThumbnail(String imagePath) {
        if (imagePath != null && new File(imagePath).exists()) {
            ImageIcon icon = new ImageIcon(new ImageIcon(imagePath).getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH));
            profilePictureLabel.setIcon(icon);
            profilePictureLabel.setText("");
        } else {
            profilePictureLabel.setIcon(null);
            profilePictureLabel.setText("No Pic");
        }
    }

    private void viewWatchlist() {
        String selectedType = getSelectedMediaTypeForDB();
        String sql = "SELECT m.title, m.release_year, m.director FROM Watchlist w JOIN Movies m ON w.movie_id = m.movie_id WHERE w.user_id = ? AND m.media_type = ? ORDER BY m.title";
        
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setString(2, selectedType);
            ResultSet rs = pstmt.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                tableModel.addRow(new Object[]{rs.getString("title"), rs.getInt("release_year"), rs.getString("director"), null, "Remove"});
            }
            if (!found) {
                tableModel.addRow(new Object[]{"Your " + selectedType + " watchlist is empty.", null, null, null, ""});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {

        try {
        // For a clean, modern light theme:
        UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());

        // Or, for a stunning dark theme (just uncomment this line and comment out the light one):
        // UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf()); 

    } catch (UnsupportedLookAndFeelException e) {
        System.err.println("Failed to initialize LaF. Using default look and feel.");
    }

        SwingUtilities.invokeLater(() -> {
            JFrame tempParentFrame = new JFrame();
            tempParentFrame.setUndecorated(true);
            tempParentFrame.setLocationRelativeTo(null);
            tempParentFrame.setVisible(true);
            LoginDialog loginDlg = new LoginDialog(tempParentFrame);
            loginDlg.setVisible(true);
            tempParentFrame.dispose();
            if (loginDlg.isSucceeded()) {
                int userId = loginDlg.getLoggedInUserId();
                String username = loginDlg.getLoggedInUsername();
                String profilePicPath = loginDlg.getProfilePicPath();
                MainFrame frame = new MainFrame(userId, username, profilePicPath);
                frame.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }

    private int getMovieIdByTitle(String title, String mediaType) {
        String sql = "SELECT movie_id FROM Movies WHERE title = ? AND media_type = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, mediaType);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("movie_id");
            }
        } catch (SQLException ex) { 
            ex.printStackTrace(); 
        }
        return -1;
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return this.button;
        }
        
        public Object getCellEditorValue() {
            if (isPushed) {
                int modelRow = movieTable.convertRowIndexToModel(movieTable.getEditingRow());
                String movieTitle = (String) tableModel.getValueAt(modelRow, 0);
                String selectedType = getSelectedMediaTypeForDB();
                int movieId = getMovieIdByTitle(movieTitle, selectedType);
                if (movieId != -1) {
                    if ("Add".equals(label)) addMovieToWatchlist(movieId);
                    else if ("Remove".equals(label)) {
                        removeMovieFromWatchlist(movieId);
                        tableModel.removeRow(modelRow);
                    }
                }
            }
            isPushed = false;
            return label;
        }
        
        private void addMovieToWatchlist(int movieId) {
            String checkSql = "SELECT 1 FROM Watchlist WHERE user_id = ? AND movie_id = ?";
            String insertSql = "INSERT INTO Watchlist (watchlist_id, user_id, movie_id) VALUES (watchlist_id_seq.NEXTVAL, ?, ?)";
            try (Connection conn = DatabaseConnector.getConnection()) {
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, currentUserId);
                    checkStmt.setInt(2, movieId);
                    if (checkStmt.executeQuery().next()) {
                        JOptionPane.showMessageDialog(button, "This item is already in your watchlist.");
                        return;
                    }
                }
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, currentUserId);
                    insertStmt.setInt(2, movieId);
                    insertStmt.executeUpdate();
                    JOptionPane.showMessageDialog(button, "Added to your watchlist!");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(button, "Error adding to watchlist: " + ex.getMessage());
            }
        }
        
        private void removeMovieFromWatchlist(int movieId) {
            String sql = "DELETE FROM Watchlist WHERE user_id = ? AND movie_id = ?";
            try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentUserId);
                pstmt.setInt(2, movieId);
                if (pstmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(button, "Removed from your watchlist.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(button, "Error removing from watchlist: " + ex.getMessage());
            }
        }
    }
}