import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AnimeImporter {

    public static void main(String[] args) {
        String csvFilePath = "anime_dataset.csv";

        // --- UPDATED: Define ALL necessary column indexes based on your header ---
        // id,name,genres,type,episodes,status,aired_from,aired_to,duration_per_ep,score,
        // scored_by,rank,rating,studios,producers,image,trailer,synopsis
        int COL_NAME = 1;
        int COL_GENRES = 2;
        int COL_EPISODES = 4;
        int COL_STATUS = 5;
        int COL_AIRED_FROM = 6;
        int COL_DURATION = 8;        // For runtime
        int COL_SCORE = 9;           // For external_score
        int COL_RATING = 12;         // For age_rating
        int COL_STUDIOS = 13;
        int COL_PRODUCERS = 14;
        int COL_IMAGE = 15;
        int COL_SYNOPSIS = 17;

        // --- UPDATED: SQL Query now includes ALL new columns for anime ---
        String sql = "INSERT INTO Movies (movie_id, title, release_year, director, poster_link, media_type, genres, synopsis, runtime, age_rating, external_score, episodes, status, studios, producers) VALUES (movie_id_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int itemsImported = 0;
        int linesSkipped = 0;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader lineReader = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            lineReader.readLine(); // Skip header

            System.out.println("🚀 Starting ANIME import process with FULL details...");

            String lineText;
            while ((lineText = lineReader.readLine()) != null) {
                String[] data = lineText.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                try {
                    // Skip rows that don't have enough columns
                    if (data.length <= COL_SYNOPSIS) {
                        linesSkipped++;
                        continue;
                    }
                    
                    // --- UPDATED: Read data from ALL relevant columns ---
                    String title = data[COL_NAME].trim().replaceAll("^\"|\"$", "");
                    String airedFrom = data[COL_AIRED_FROM].trim().replaceAll("^\"|\"$", "");
                    String posterLink = data[COL_IMAGE].trim().replaceAll("^\"|\"$", "");
                    String genres = data[COL_GENRES].trim().replaceAll("^\"|\"$", "");
                    String episodesStr = data[COL_EPISODES].trim().replaceAll("^\"|\"$", "");
                    String status = data[COL_STATUS].trim().replaceAll("^\"|\"$", "");
                    String runtime = data[COL_DURATION].trim().replaceAll("^\"|\"$", "");
                    String scoreStr = data[COL_SCORE].trim().replaceAll("^\"|\"$", "");
                    String ageRating = data[COL_RATING].trim().replaceAll("^\"|\"$", "");
                    String studios = data[COL_STUDIOS].trim().replaceAll("^\"|\"$", "");
                    String producers = data[COL_PRODUCERS].trim().replaceAll("^\"|\"$", "");
                    String synopsis = data[COL_SYNOPSIS].trim().replaceAll("^\"|\"$", "");

                    // Extract year and skip if invalid
                    int year;
                    if (airedFrom.length() >= 4 && airedFrom.substring(0, 4).matches("\\d{4}")) {
                        year = Integer.parseInt(airedFrom.substring(0, 4));
                    } else {
                        linesSkipped++;
                        continue;
                    }

                    // Handle potentially non-numeric or "Unknown" data
                    int episodes = (episodesStr.isEmpty() || !episodesStr.matches("\\d+")) ? 0 : Integer.parseInt(episodesStr);
                    double externalScore = (scoreStr.isEmpty() || scoreStr.equalsIgnoreCase("UNKNOWN")) ? 0.0 : Double.parseDouble(scoreStr);

                    // --- UPDATED: Set ALL parameters for the INSERT statement ---
                    pstmt.setString(1, title);
                    pstmt.setInt(2, year);
                    pstmt.setString(3, "N/A"); // Director placeholder
                    pstmt.setString(4, posterLink);
                    pstmt.setString(5, "anime"); // media_type
                    pstmt.setString(6, genres);
                    pstmt.setString(7, synopsis);
                    pstmt.setString(8, runtime);
                    pstmt.setString(9, ageRating);
                    pstmt.setDouble(10, externalScore);
                    pstmt.setInt(11, episodes);
                    pstmt.setString(12, status);
                    pstmt.setString(13, studios);
                    pstmt.setString(14, producers);

                    pstmt.addBatch();
                    itemsImported++;

                } catch (Exception e) {
                    System.err.println("Skipping line due to parsing error: " + lineText + " | Error: " + e.getMessage());
                    linesSkipped++;
                }
            }

            System.out.println("Executing batch insert... Please wait.");
            pstmt.executeBatch();
            conn.commit();

            System.out.println("\n✅ Anime import complete!");
            System.out.println("Total anime imported: " + itemsImported);
            System.out.println("Total lines skipped: " + linesSkipped);

        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database error during import: " + e.getMessage());
        }
    }
}