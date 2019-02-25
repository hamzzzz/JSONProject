package code;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Application {

    protected static final ArrayList<LogEntry> startedList = new ArrayList<>();
    protected static final ArrayList<LogEntry> finishedList = new ArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = Logger.getLogger(Application.class.getName());
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS Alert (id VARCHAR(20), duration INTEGER, type VARCHAR(50), host VARCHAR(50), alert BOOLEAN)";
    private static final String PREPARED_STATEMENT = "INSERT INTO Alert (id, duration, type, host, alert)  Values (?, ?, ?, ?, ?)";
    private static final String STARTED = "STARTED";
    protected static Connection connection;

    public static void main(String[] args) {
        logger.log(Level.INFO, "Starting Application...");
        try (Stream<String> inputStream = Files.lines(Paths.get(args[0]))) {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            connection = DriverManager.getConnection("jdbc:hsqldb:file:alertdb;ifexists=false", "SA", "");
            connection.createStatement().execute(CREATE_TABLE);
            inputStream.forEachOrdered(Application::splitFileIntoGroups);
            startedList.parallelStream().forEachOrdered(Application::processData);
            connection.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error opening File -" + e);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error connecting to HSQL -" + e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error HSQLDB class not found -" + e);
        }
        logger.log(Level.INFO, "Terminating Application...");
    }

    /**
     * Takes a String line adds it to the Started or Finished list for further
     * processing.
     *
     * @param line
     */
    protected static void splitFileIntoGroups(String line) {
        try {
            LogEntry logEntry = objectMapper.readValue(line, LogEntry.class);
            if (logEntry.getState().equals(STARTED)) {
                startedList.add(logEntry);
            } else {
                finishedList.add(logEntry);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error processing line -" + line);
        }
    }

    /**
     * Takes a LogEntry object and applies a filter to the finishedList to find
     * its finishing log entry. The run duration is determined and alert flag
     * set
     *
     * @param logEntry
     */
    private static void processData(LogEntry logEntry) {
        try {
            Stream<LogEntry> result = finishedList
                    .stream()
                    .filter(item -> item.getId().equals(logEntry.getId())
                    && (item.getHost().compareTo(logEntry.getHost()) == item.getType().compareTo(logEntry.getType())))
                    .limit(1);

            LogEntry filteredLogEntry = result.findFirst().get();
            long duration = Duration.between(logEntry.getTimestamp().toInstant(), filteredLogEntry.getTimestamp().toInstant()).toMillis();
            if (duration > 4) {
                saveAlerts(filteredLogEntry, duration, true);
            } else {
                saveAlerts(filteredLogEntry, duration, false);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error processing data -" + e);
        }
    }

    /**
     * Takes in LogEntry object along with duration and alert flag. This date is
     * stored in DB
     *
     * @param logAlertEntry
     * @param duration
     * @param isAlert
     */
    protected static Boolean saveAlerts(LogEntry logAlertEntry, long duration, Boolean isAlert) {
        try {
            PreparedStatement statement = connection.prepareStatement(PREPARED_STATEMENT);
            statement.setString(1, logAlertEntry.getId());
            statement.setLong(2, duration);
            statement.setString(3, logAlertEntry.getType());
            statement.setString(4, logAlertEntry.getHost());
            statement.setBoolean(5, isAlert);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error saving data -" + e);
            return false;
        }
    }
}
