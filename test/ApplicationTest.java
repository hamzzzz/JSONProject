
import code.Application;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import code.LogEntry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

class ApplicationTest {

    private static final Logger logger = Logger.getLogger(ApplicationTest.class.getName());
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS Alert (id VARCHAR(20), duration INTEGER, type VARCHAR(50), host VARCHAR(50), alert BOOLEAN)";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static LogEntry validObject;
    private static LogEntry validObjectWithAdditionalFields;

    @BeforeEach
    void setup() {
        logger.log(Level.INFO, "Begin Testing Application...");
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            Application.connection = DriverManager.getConnection("jdbc:hsqldb:file:alertdbTest;ifexists=false", "SA", "");
            Application.connection.createStatement().execute(CREATE_TABLE);
            validObject = objectMapper.readValue("{\"id\":\"scsmbstgrc\", \"state\":\"STARTED\", \"timestamp\":1491377495218}", LogEntry.class);
            validObjectWithAdditionalFields = objectMapper.readValue("{\"id\":\"scsmbstgra\", \"state\":\"STARTED\", \"type\":\"APPLICATION_LOG\", \"host\":\"12345\", \"timestamp\":1491377495212}", LogEntry.class);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error connecting to HSQL -" + e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error HSQLDB class not found -" + e);
        } catch (JsonParseException e) {
            logger.log(Level.SEVERE, "Error Parsing Test Json String -" + e);
        } catch (JsonMappingException e) {
            logger.log(Level.SEVERE, "Error Mapping Test Json to Pojo -" + e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error class used in tests not found -" + e);
        }
    }

    @BeforeEach
    void resetLists() {
        Application.startedList.clear();
        Application.finishedList.clear();
    }

    @AfterEach
    void closeConnection() {
        try {
            Application.connection.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing connection -" + e);
        }
    }

    @Test
    void splitFileIntoGroupsTest_ValidJsonMissingRequiredField() {
        String testData = "{\"state\":\"STARTED\", \"timestamp\":1491377495218}";
        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());

        Application.splitFileIntoGroups(testData);

        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());
    }

    @Test
    void splitFileIntoGroupsTest_InvalidJson() {
        String testData = "invalid json object";
        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());

        Application.splitFileIntoGroups(testData);

        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());
    }

    @Test
    void splitFileIntoGroupsTest_ValidStartedJson() {
        String testData = "{\"id\":\"scsmbstgrc\", \"state\":\"STARTED\", \"timestamp\":1491377495218}";
        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());

        Application.splitFileIntoGroups(testData);

        assertEquals(1, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());
    }

    @Test
    void splitFileIntoGroupsTest_ValidFinishedJson() {
        String testData = "{\"id\":\"scsmbstgrc\", \"state\":\"FINISHED\", \"timestamp\":1491377495218}";
        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());

        Application.splitFileIntoGroups(testData);

        assertEquals(0, Application.startedList.size());
        assertEquals(1, Application.finishedList.size());
    }

    @Test
    void splitFileIntoGroupsTest_ValidStartedJsonWithAdditionalFields() {
        String testData = "{\"id\":\"scsmbstgra\", \"state\":\"STARTED\", \"type\":\"APPLICATION_LOG\", \"host\":\"12345\", \"timestamp\":1491377495212}";
        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());

        Application.splitFileIntoGroups(testData);

        assertEquals(1, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());
    }

    @Test
    void splitFileIntoGroupsTest_ValidFinishedJsonWithAdditionalFields() {
        String testData = "{\"id\":\"scsmbstgra\", \"state\":\"Finished\", \"type\":\"APPLICATION_LOG\", \"host\":\"12345\", \"timestamp\":1491377495212}";
        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());

        Application.splitFileIntoGroups(testData);

        assertEquals(0, Application.startedList.size());
        assertEquals(1, Application.finishedList.size());
    }

    @Test
    void saveAlertsTest_ValidJson() {
        assertTrue(Application.saveAlerts(validObject, 1, true));
        assertTrue(Application.saveAlerts(validObject, 1, false));

        assertTrue(Application.saveAlerts(validObject, 1000000, true));
        assertTrue(Application.saveAlerts(validObject, 1000000, false));
    }

    @Test
    void saveAlertsTest_ValidJsonWithAdditionalFields() {
        assertTrue(Application.saveAlerts(validObjectWithAdditionalFields, 1, true));
        assertTrue(Application.saveAlerts(validObjectWithAdditionalFields, 1, false));

        assertTrue(Application.saveAlerts(validObjectWithAdditionalFields, 1000000, true));
        assertTrue(Application.saveAlerts(validObjectWithAdditionalFields, 1000000, false));
    }

    @Test
    void saveAlertsTest_InvalidJson() {
        assertFalse(Application.saveAlerts(null, 1, true));
        assertFalse(Application.saveAlerts(null, 1, false));

        assertFalse(Application.saveAlerts(null, 1000000, true));
        assertFalse(Application.saveAlerts(null, 1000000, false));
    }

    @Test
    void mainTest_FullRunThrough() {
        assertEquals(0, Application.startedList.size());
        assertEquals(0, Application.finishedList.size());

        String[] inputArgs = {"src/test/resources/testData.txt"};
        Application.main(inputArgs);

        assertEquals(3, Application.startedList.size());
        assertEquals(3, Application.finishedList.size());

        try {
            Statement statement = Application.connection.createStatement();
            assertEquals(3, statement.executeQuery("SELECT count(*) FROM Alert AS count").getInt("count"));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error executing query -" + e);
        }
    }
}
