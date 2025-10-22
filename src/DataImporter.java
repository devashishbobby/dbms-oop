import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataImporter {

    public static void main(String[] args) {
        String csvFilePath = "top1000_movies.csv";

        // --- UPDATED: Define ALL necessary column indexes based on your header ---
        // Poster_Link,Series_Title,Released_Year,Certificate,Runtime,Genre,IMDB_Rating,Overview,Meta_score,Director,Star1,Star2,Star3,Star4,No_of_Votes,Gross
        int COL_POSTER = 0;
        int COL_TITLE = 1;
        int COL_YEAR = 2;
        int COL_CERTIFICATE = 3; // For age_rating
        int COL_RUNTIME = 4;     // For runtime
        int COL_GENRE = 5;       // For genres
        int COL_IMDB_RATING = 6; // For external_score
        int COL_OVERVIEW = 7;    // For synopsis
        int COL_DIRECTOR = 9;

        // --- UPDATED: SQL Query now includes ALL new columns ---
        String sql = "INSERT INTO Movies (movie_id, title, release_year, director, poster_link, media_type, genres, synopsis, runtime, age_rating, external_score) VALUES (movie_id_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int moviesImported = 0;
        int linesSkipped = 0;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader lineReader = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            lineReader.readLine(); // Skip header

            System.out.println("🚀 Starting MOVIE import process with FULL details...");

            String lineText;
            while ((lineText = lineReader.readLine()) != null) {
                String[] data = lineText.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                try {
                    // --- UPDATED: Read data from ALL relevant columns ---
                    String posterLink = data[COL_POSTER].trim().replaceAll("^\"|\"$", "");
                    String title = data[COL_TITLE].trim().replaceAll("^\"|\"$", "");
                    String yearStr = data[COL_YEAR].trim().replaceAll("^\"|\"$", "");
                    String ageRating = data[COL_CERTIFICATE].trim().replaceAll("^\"|\"$", "");
                    String runtime = data[COL_RUNTIME].trim().replaceAll("^\"|\"$", "");
                    String genres = data[COL_GENRE].trim().replaceAll("^\"|\"$", "");
                    String imdbRatingStr = data[COL_IMDB_RATING].trim().replaceAll("^\"|\"$", "");
                    String synopsis = data[COL_OVERVIEW].trim().replaceAll("^\"|\"$", "");
                    String director = data[COL_DIRECTOR].trim().replaceAll("^\"|\"$", "");
                    
                    if (!yearStr.matches("\\d{4}")) { // Ensure it's a 4-digit year
                        linesSkipped++;
                        continue;
                    }
                    int year = Integer.parseInt(yearStr);

                    // Handle potential non-numeric rating
                    double externalScore = 0.0;
                    try {
                        externalScore = Double.parseDouble(imdbRatingStr);
                    } catch (NumberFormatException e) {
                        // Keep externalScore as 0.0 if parsing fails
                    }


                    // --- UPDATED: Set ALL parameters for the INSERT statement ---
                    pstmt.setString(1, title);
                    pstmt.setInt(2, year);
                    pstmt.setString(3, director);
                    pstmt.setString(4, posterLink);
                    pstmt.setString(5, "movie"); // media_type
                    pstmt.setString(6, genres);     // genres
                    pstmt.setString(7, synopsis);   // synopsis
                    pstmt.setString(8, runtime);    // runtime
                    pstmt.setString(9, ageRating);  // age_rating
                    pstmt.setDouble(10, externalScore); // external_score
                    
                    pstmt.addBatch();
                    moviesImported++;

                } catch (Exception e) {
                    System.err.println("Skipping line due to parsing error: " + lineText + " | Error: " + e.getMessage());
                    linesSkipped++;
                }
            }

            System.out.println("Executing batch insert... Please wait.");
            pstmt.executeBatch();
            conn.commit();

            System.out.println("\n✅ Movie import complete!");
            System.out.println("Total movies imported: " + moviesImported);
            System.out.println("Total lines skipped: " + linesSkipped);

        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database error during import: " + e.getMessage());
        }
    }
}