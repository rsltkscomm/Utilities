package databaseUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reporting.TestLogManager;
import reporting.ExtentManager;

/**
 * Utility class for database operations with ExtentManager logging
 */
public class DatabaseUtils {

    public static Connection getConnection(String dbUrl, String username, String password) {
        try {
            Connection connection = DriverManager.getConnection(dbUrl, username, password);
            TestLogManager.success("Database connection established");
            ExtentManager.infoTest("Database connection established to: " + dbUrl);
            return connection;
        } catch (SQLException e) {
            TestLogManager.error("Failed to establish database connection", e);
            ExtentManager.failTest("Failed to establish database connection to: " + dbUrl + " -> " + e.getMessage());
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    public static List<Map<String, String>> executeQuery(String dbUrl, String username, String password, String query) {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection(dbUrl, username, password);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

            List<Map<String, String>> results = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, String> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String value = resultSet.getString(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }

            TestLogManager.dataInfo("Query executed", "Rows returned: " + results.size());
            ExtentManager.infoTest("Query executed: " + query + " -> Rows returned: " + results.size());
            return results;

        } catch (SQLException e) {
            TestLogManager.error("Failed to execute query", e);
            ExtentManager.failTest("Failed to execute query: " + query + " -> " + e.getMessage());
            throw new RuntimeException("Failed to execute query", e);
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    public static int executeUpdate(String dbUrl, String username, String password, String query) {
        Connection connection = null;
        Statement statement = null;

        try {
            connection = getConnection(dbUrl, username, password);
            statement = connection.createStatement();
            int rowsAffected = statement.executeUpdate(query);

            TestLogManager.dataInfo("Update query executed", "Rows affected: " + rowsAffected);
            ExtentManager.infoTest("Update query executed: " + query + " -> Rows affected: " + rowsAffected);
            return rowsAffected;

        } catch (SQLException e) {
            TestLogManager.error("Failed to execute update query", e);
            ExtentManager.failTest("Failed to execute update query: " + query + " -> " + e.getMessage());
            throw new RuntimeException("Failed to execute update query", e);
        } finally {
            closeResources(connection, statement, null);
        }
    }

    private static void closeResources(Connection connection, Statement statement, ResultSet resultSet) {
        try {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
            TestLogManager.info("Database resources closed");
            ExtentManager.infoTest("Database resources closed");
        } catch (SQLException e) {
            TestLogManager.error("Failed to close database resources", e);
            ExtentManager.failTest("Failed to close database resources: " + e.getMessage());
        }
    }

    public static String getSingleValue(String dbUrl, String username, String password, String query) {
        List<Map<String, String>> results = executeQuery(dbUrl, username, password, query);

        if (results.isEmpty()) {
            TestLogManager.warning("No results found for query");
            ExtentManager.warningTest("No results found for query: " + query);
            return null;
        }

        Map<String, String> firstRow = results.get(0);
        if (firstRow.isEmpty()) {
            TestLogManager.warning("Empty result row");
            ExtentManager.warningTest("Empty result row for query: " + query);
            return null;
        }

        String value = firstRow.values().iterator().next();
        TestLogManager.dataInfo("Single value retrieved", value);
        ExtentManager.infoTest("Single value retrieved: " + value + " for query: " + query);
        return value;
    }

    public static boolean tableExists(String dbUrl, String username, String password, String tableName) {
        try {
            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE 1=0";
            executeQuery(dbUrl, username, password, query);
            TestLogManager.success("Table exists: " + tableName);
            ExtentManager.infoTest("Table exists: " + tableName);
            return true;
        } catch (Exception e) {
            TestLogManager.warning("Table does not exist: " + tableName);
            ExtentManager.warningTest("Table does not exist: " + tableName);
            return false;
        }
    }

    public static int getTableRowCount(String dbUrl, String username, String password, String tableName) {
        try {
            String query = "SELECT COUNT(*) as count FROM " + tableName;
            String countStr = getSingleValue(dbUrl, username, password, query);
            int count = Integer.parseInt(countStr);
            TestLogManager.dataInfo("Table row count", tableName + " -> " + count);
            ExtentManager.infoTest("Table row count: " + tableName + " -> " + count);
            return count;
        } catch (Exception e) {
            TestLogManager.error("Failed to get table row count: " + tableName, e);
            ExtentManager.failTest("Failed to get table row count: " + tableName + " -> " + e.getMessage());
            return -1;
        }
    }

    public static List<String> getTableColumns(String dbUrl, String username, String password, String tableName) {
        try {
            String query = "SELECT * FROM " + tableName + " WHERE 1=0";
            List<Map<String, String>> results = executeQuery(dbUrl, username, password, query);

            List<String> columns = results.isEmpty() ? new ArrayList<>() : new ArrayList<>(results.get(0).keySet());
            TestLogManager.dataInfo("Table columns", tableName + " -> " + columns);
            ExtentManager.infoTest("Table columns: " + tableName + " -> " + columns);
            return columns;
        } catch (Exception e) {
            TestLogManager.error("Failed to get table columns: " + tableName, e);
            ExtentManager.failTest("Failed to get table columns: " + tableName + " -> " + e.getMessage());
            throw new RuntimeException("Failed to get table columns", e);
        }
    }

    public static boolean validateTableData(String dbUrl, String username, String password, String tableName, String condition) {
        try {
            String query = "SELECT COUNT(*) as count FROM " + tableName + " WHERE " + condition;
            String countStr = getSingleValue(dbUrl, username, password, query);
            int count = Integer.parseInt(countStr);
            boolean isValid = count > 0;

            TestLogManager.dataInfo("Table data validation", tableName + " -> " + (isValid ? "Valid" : "Invalid"));
            ExtentManager.infoTest("Table data validation: " + tableName + " -> " + (isValid ? "Valid" : "Invalid"));
            return isValid;
        } catch (Exception e) {
            TestLogManager.error("Failed to validate table data: " + tableName, e);
            ExtentManager.failTest("Failed to validate table data: " + tableName + " -> " + e.getMessage());
            return false;
        }
    }

    public static boolean compareQueryResults(String dbUrl1, String username1, String password1, String query1,
                                              String dbUrl2, String username2, String password2, String query2) {
        try {
            List<Map<String, String>> results1 = executeQuery(dbUrl1, username1, password1, query1);
            List<Map<String, String>> results2 = executeQuery(dbUrl2, username2, password2, query2);

            boolean areEqual = results1.equals(results2);
            TestLogManager.dataInfo("Query results comparison", "Equal: " + areEqual);
            ExtentManager.infoTest("Query results comparison: Equal -> " + areEqual);
            return areEqual;
        } catch (Exception e) {
            TestLogManager.error("Failed to compare query results", e);
            ExtentManager.failTest("Failed to compare query results: " + e.getMessage());
            return false;
        }
    }

    public static void loadDriver(String driverClassName) {
        try {
            Class.forName(driverClassName);
            TestLogManager.success("Database driver loaded: " + driverClassName);
            ExtentManager.infoTest("Database driver loaded: " + driverClassName);
        } catch (ClassNotFoundException e) {
            TestLogManager.error("Failed to load database driver: " + driverClassName, e);
            ExtentManager.failTest("Failed to load database driver: " + driverClassName + " -> " + e.getMessage());
            throw new RuntimeException("Failed to load database driver", e);
        }
    }

    public static boolean testConnection(String dbUrl, String username, String password) {
        Connection connection = null;
        try {
            connection = getConnection(dbUrl, username, password);
            TestLogManager.success("Database connection test passed");
            ExtentManager.infoTest("Database connection test passed for: " + dbUrl);
            return true;
        } catch (Exception e) {
            TestLogManager.error("Database connection test failed", e);
            ExtentManager.failTest("Database connection test failed for: " + dbUrl + " -> " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                    ExtentManager.infoTest("Test connection closed for: " + dbUrl);
                } catch (SQLException e) {
                    TestLogManager.error("Failed to close test connection", e);
                    ExtentManager.failTest("Failed to close test connection: " + e.getMessage());
                }
            }
        }
    }

    public static String getMySqlClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    public static String getSqlServerClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    public static String getOracleClassName() {
        return "oracle.jdbc.driver.OracleDriver";
    }
}
