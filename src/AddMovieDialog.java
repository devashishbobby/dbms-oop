import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddMovieDialog extends JDialog {

    private JTextField titleField;
    private JTextField yearField;
    private JTextField directorField;
    // --- NEW: Text field for the poster URL ---
    private JTextField posterLinkField;
    private boolean movieAdded = false;

    public AddMovieDialog(Frame parent) {
        super(parent, "Add a New Movie", true);
        setSize(450, 250); // Made slightly taller to fit the new field
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // --- UI Components ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.insets = new Insets(5, 5, 5, 5);

        // Title
        cs.gridx = 0; cs.gridy = 0; cs.gridwidth = 1; formPanel.add(new JLabel("Title:"), cs);
        cs.gridx = 1; cs.gridy = 0; cs.gridwidth = 2; titleField = new JTextField(20); formPanel.add(titleField, cs);

        // Release Year
        cs.gridx = 0; cs.gridy = 1; cs.gridwidth = 1; formPanel.add(new JLabel("Release Year:"), cs);
        cs.gridx = 1; cs.gridy = 1; cs.gridwidth = 2; yearField = new JTextField(20); formPanel.add(yearField, cs);

        // Director
        cs.gridx = 0; cs.gridy = 2; cs.gridwidth = 1; formPanel.add(new JLabel("Director:"), cs);
        cs.gridx = 1; cs.gridy = 2; cs.gridwidth = 2; directorField = new JTextField(20); formPanel.add(directorField, cs);

        // --- NEW: Poster Link field ---
        cs.gridx = 0; cs.gridy = 3; cs.gridwidth = 1; formPanel.add(new JLabel("Poster URL:"), cs);
        cs.gridx = 1; cs.gridy = 3; cs.gridwidth = 2; posterLinkField = new JTextField(20); formPanel.add(posterLinkField, cs);

        // Buttons
        JButton submitButton = new JButton("Submit");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        // --- Layout ---
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        cancelButton.addActionListener(e -> dispose());
        submitButton.addActionListener(e -> submitNewMovie());
    }

    private void submitNewMovie() {
        String title = titleField.getText().trim();
        String yearStr = yearField.getText().trim();
        String director = directorField.getText().trim();
        // --- NEW: Get the poster link from the new field ---
        String posterLink = posterLinkField.getText().trim();

        // --- Input Validation ---
        if (title.isEmpty() || yearStr.isEmpty() || director.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title, Year, and Director fields are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int year;
        try {
            year = Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for the year.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- MODIFIED: The SQL now includes the poster_link column ---
        String sql = "INSERT INTO Movies (movie_id, title, release_year, director, poster_link) VALUES (movie_id_seq.NEXTVAL, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setInt(2, year);
            pstmt.setString(3, director);
            // --- NEW: Set the value for the poster link parameter ---
            pstmt.setString(4, posterLink.isEmpty() ? null : posterLink); // Save null if the field is empty

            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Media added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            movieAdded = true;
            dispose();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public boolean isMovieAdded() {
        return movieAdded;
    }
}