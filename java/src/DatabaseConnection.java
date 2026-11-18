import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens the SQLite database and creates it from the SQL scripts on first run.
 * Run the program from the project root so that "apartment.db" and "sql/" are found.
 * The path can be overridden with  -Ddb.path=other.db  (used by the tests).
 */
public class DatabaseConnection {

    private static final String DB_PATH = System.getProperty("db.path", "apartment.db");

    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return con;
    }

    /** Creates tables (and loads sample data) if the database is empty. */
    public static void initialiseIfNeeded(boolean loadSampleData) throws SQLException, IOException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='apartment'");
            boolean exists = rs.next();
            rs.close();
            if (!exists) {
                runScript(st, Paths.get("sql", "schema.sql"));
                if (loadSampleData) {
                    runScript(st, Paths.get("sql", "sample_data.sql"));
                }
                System.out.println("Database created: " + DB_PATH);
            }
        }
    }

    private static void runScript(Statement st, Path file) throws IOException, SQLException {
        String script = new String(Files.readAllBytes(file));
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\n")) {
            String t = line.trim();
            if (!t.startsWith("--")) {
                cleaned.append(line).append("\n");
            }
        }
        for (String sql : cleaned.toString().split(";")) {
            if (!sql.trim().isEmpty()) {
                st.execute(sql.trim());
            }
        }
    }
}
