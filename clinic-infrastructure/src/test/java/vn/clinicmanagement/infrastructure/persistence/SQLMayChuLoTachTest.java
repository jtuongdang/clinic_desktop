package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLMayChuLoTachTest {

    @Test
    void splitsGoSeparatedBatchesInOrderAcrossUnixAndWindowsNewlines() {
        String script = "CREATE TABLE SyntheticSection (id INT PRIMARY KEY);\n"
                + "GO\n"
                + "INSERT INTO SyntheticSection (id) VALUES (1);\r\n"
                + "GO\r\n"
                + "UPDATE SyntheticSection SET id = 2 WHERE id = 1;";

        List<String> batches = SQLMayChuLoTach.split(script);

        assertEquals(
                List.of(
                        "CREATE TABLE SyntheticSection (id INT PRIMARY KEY);",
                        "INSERT INTO SyntheticSection (id) VALUES (1);",
                        "UPDATE SyntheticSection SET id = 2 WHERE id = 1;"
                ),
                batches
        );
    }

    @Test
    void recognizesStandaloneGoCaseInsensitivelyWithSurroundingWhitespace() {
        String script = "SELECT 1 AS stepOne;\n"
                + "   go   \n"
                + "SELECT 2 AS stepTwo;\n"
                + "\tGo\t\r\n"
                + "SELECT 3 AS stepThree;\n"
                + "  gO\n"
                + "SELECT 4 AS stepFour;";

        List<String> batches = SQLMayChuLoTach.split(script);

        assertEquals(
                List.of(
                        "SELECT 1 AS stepOne;",
                        "SELECT 2 AS stepTwo;",
                        "SELECT 3 AS stepThree;",
                        "SELECT 4 AS stepFour;"
                ),
                batches
        );
    }

    @Test
    void discardsBlankBatchesWithoutLosingNonEmptySql() {
        String script = "   \n"
                + "GO\n"
                + "   \t  \r\n"
                + "  go  \n"
                + "\n"
                + "CREATE VIEW SyntheticSummary AS\n"
                + "SELECT 1 AS totalCount;\n"
                + "\n"
                + "GO\n"
                + "GO\n"
                + "   \n"
                + "SELECT totalCount FROM SyntheticSummary;\n"
                + "  GO  \n"
                + "   \n";

        List<String> batches = SQLMayChuLoTach.split(script);

        assertEquals(
                List.of(
                        "CREATE VIEW SyntheticSummary AS\nSELECT 1 AS totalCount;",
                        "SELECT totalCount FROM SyntheticSummary;"
                ),
                batches
        );
    }

    @Test
    void doesNotSplitOnGoEmbeddedWithinSqlStatements() {
        String script = "SELECT 'GO' AS statusLabel, 'go' AS lowerLabel\n"
                + "FROM SyntheticQueue\n"
                + "WHERE code = 'GO';\n"
                + "GO\n"
                + "INSERT INTO SyntheticAudit (note) VALUES ('Ready to go');";

        List<String> batches = SQLMayChuLoTach.split(script);

        assertEquals(
                List.of(
                        "SELECT 'GO' AS statusLabel, 'go' AS lowerLabel\nFROM SyntheticQueue\nWHERE code = 'GO';",
                        "INSERT INTO SyntheticAudit (note) VALUES ('Ready to go');"
                ),
                batches
        );
    }

    @Test
    void returnsEmptyListForNullOrBlankScript() {
        assertTrue(SQLMayChuLoTach.split(null).isEmpty());
        assertTrue(SQLMayChuLoTach.split("").isEmpty());
        assertTrue(SQLMayChuLoTach.split("   \n\t\r\n  ").isEmpty());
        assertTrue(SQLMayChuLoTach.split(" \n GO \r\n \t go \n ").isEmpty());
    }
}
