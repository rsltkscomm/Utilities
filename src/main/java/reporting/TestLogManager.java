package reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive Log4j Manager for Test Automation Framework
 * Supports configurable logging levels: SUCCESS, INFO, WARNING, ERROR
 * Integrates with config.properties for dynamic configuration
 */
public class TestLogManager {
    
    private static final Logger logger = LogManager.getLogger(TestLogManager.class);
    private static final Logger successLogger = LogManager.getLogger("SUCCESS");
    private static final Logger infoLogger = LogManager.getLogger("INFO");
    private static final Logger warningLogger = LogManager.getLogger("WARNING");
    private static final Logger errorLogger = LogManager.getLogger("ERROR");
    private static final Logger testExecutionLogger = LogManager.getLogger("TestExecution");
    
    private static Properties config;
    private static String currentLogLevel;
    private static boolean isInitialized = false;
    
    // Custom log levels
    public static final Level SUCCESS = Level.forName("SUCCESS", 350);
    public static final Level TEST_EXECUTION = Level.forName("TEST_EXECUTION", 450);
    
    static {
        initializeLogManager();
    }
    
    /**
     * Initialize the LogManager with configuration from config.properties
     */
    private static void initializeLogManager() {
        try {
            config = new Properties();
            FileInputStream fis = new FileInputStream("src/main/resources/config.properties");
            config.load(fis);
            fis.close();
            
            currentLogLevel = config.getProperty("logging.level", "ALL");
            isInitialized = true;
            
            logger.info("LogManager initialized successfully with log level: " + currentLogLevel);
        } catch (IOException e) {
            errorLogger.error("Failed to load config.properties: " + e.getMessage());
            currentLogLevel = "ALL"; // Default to ALL if config fails to load
        }
    }
    
    /**
     * Reload configuration from config.properties
     */
    public static void reloadConfiguration() {
        initializeLogManager();
    }
    
    /**
     * Check if a specific log level should be printed based on configuration
     */
    private static boolean shouldLog(String level) {
        if (!isInitialized) {
            initializeLogManager();
        }
        
        if ("ALL".equalsIgnoreCase(currentLogLevel)) {
            return true;
        }
        
        return level.equalsIgnoreCase(currentLogLevel);
    }
    
    /**
     * Check if a specific log category is enabled
     */
    private static boolean isCategoryEnabled(String category) {
        if (!isInitialized) {
            initializeLogManager();
        }
        
        return Boolean.parseBoolean(config.getProperty("logging." + category.toLowerCase() + ".enabled", "true"));
    }
    
    // ===========================================
    // SUCCESS LOGGING METHODS
    // ===========================================
    
    /**
     * Log success message
     */
    public static void success(String message) {
        if (shouldLog("SUCCESS") && isCategoryEnabled("success")) {
            successLogger.info("✅ SUCCESS: " + message);
        }
    }
    
    /**
     * Log success message with parameters
     */
    public static void success(String message, Object... params) {
        if (shouldLog("SUCCESS") && isCategoryEnabled("success")) {
            successLogger.info("✅ SUCCESS: " + String.format(message, params));
        }
    }
    
    /**
     * Log test step success
     */
    public static void testStepSuccess(String testStep) {
        if (shouldLog("SUCCESS") && isCategoryEnabled("success")) {
            successLogger.info("✅ TEST STEP SUCCESS: " + testStep);
        }
    }
    
    /**
     * Log element interaction success
     */
    public static void elementSuccess(String elementName, String action) {
        if (shouldLog("SUCCESS") && isCategoryEnabled("success")) {
            successLogger.info("✅ ELEMENT SUCCESS: " + elementName + " - " + action);
        }
    }
    
    // ===========================================
    // INFO LOGGING METHODS
    // ===========================================
    
    /**
     * Log info message
     */
    public static void info(String message) {
        if (shouldLog("INFO") && isCategoryEnabled("info")) {
            infoLogger.info("ℹ️ INFO: " + message);
        }
    }
    
    /**
     * Log info message with parameters
     */
    public static void info(String message, Object... params) {
        if (shouldLog("INFO") && isCategoryEnabled("info")) {
            infoLogger.info("ℹ️ INFO: " + String.format(message, params));
        }
    }
    
    /**
     * Log test execution info
     */
    public static void testInfo(String testName, String info) {
        if (shouldLog("INFO") && isCategoryEnabled("info")) {
            infoLogger.info("🧪 TEST INFO: [" + testName + "] " + info);
        }
    }
    
    /**
     * Log page navigation info
     */
    public static void navigationInfo(String fromPage, String toPage) {
        if (shouldLog("INFO") && isCategoryEnabled("info")) {
            infoLogger.info("🧭 NAVIGATION: " + fromPage + " → " + toPage);
        }
    }
    
    /**
     * Log data input info
     */
    public static void dataInfo(String fieldName, String value) {
        if (shouldLog("INFO") && isCategoryEnabled("info")) {
            infoLogger.info("📝 DATA INPUT: " + fieldName + " = " + value);
        }
    }
    
    // ===========================================
    // WARNING LOGGING METHODS
    // ===========================================
    
    /**
     * Log warning message
     */
    public static void warning(String message) {
        if (shouldLog("WARNING") && isCategoryEnabled("warning")) {
            warningLogger.warn("⚠️ WARNING: " + message);
        }
    }
    
    /**
     * Log warning message with parameters
     */
    public static void warning(String message, Object... params) {
        if (shouldLog("WARNING") && isCategoryEnabled("warning")) {
            warningLogger.warn("⚠️ WARNING: " + String.format(message, params));
        }
    }
    
    /**
     * Log element not found warning
     */
    public static void elementWarning(String elementName, String reason) {
        if (shouldLog("WARNING") && isCategoryEnabled("warning")) {
            warningLogger.warn("⚠️ ELEMENT WARNING: " + elementName + " - " + reason);
        }
    }
    
    /**
     * Log performance warning
     */
    public static void performanceWarning(String operation, long duration) {
        if (shouldLog("WARNING") && isCategoryEnabled("warning")) {
            warningLogger.warn("⚠️ PERFORMANCE WARNING: " + operation + " took " + duration + "ms");
        }
    }
    
    /**
     * Log timeout warning
     */
    public static void timeoutWarning(String elementName, int timeout) {
        if (shouldLog("WARNING") && isCategoryEnabled("warning")) {
            warningLogger.warn("⚠️ TIMEOUT WARNING: " + elementName + " not found within " + timeout + " seconds");
        }
    }
    
    // ===========================================
    // ERROR LOGGING METHODS
    // ===========================================
    
    /**
     * Log error message
     */
    public static void error(String message) {
        if (shouldLog("ERROR") && isCategoryEnabled("error")) {
            errorLogger.error("❌ ERROR: " + message);
        }
    }
    
    /**
     * Log error message with parameters
     */
    public static void error(String message, Object... params) {
        if (shouldLog("ERROR") && isCategoryEnabled("error")) {
            errorLogger.error("❌ ERROR: " + String.format(message, params));
        }
    }
    
    /**
     * Log error with exception
     */
    public static void error(String message, Throwable throwable) {
        if (shouldLog("ERROR") && isCategoryEnabled("error")) {
            errorLogger.error("❌ ERROR: " + message, throwable);
        }
    }
    
    /**
     * Log test failure
     */
    public static void testFailure(String testName, String reason) {
        if (shouldLog("ERROR") && isCategoryEnabled("error")) {
            errorLogger.error("❌ TEST FAILURE: [" + testName + "] " + reason);
        }
    }
    
    /**
     * Log element interaction error
     */
    public static void elementError(String elementName, String action, String reason) {
        if (shouldLog("ERROR") && isCategoryEnabled("error")) {
            errorLogger.error("❌ ELEMENT ERROR: " + elementName + " - " + action + " - " + reason);
        }
    }
    
    /**
     * Log assertion error
     */
    public static void assertionError(String expected, String actual) {
        if (shouldLog("ERROR") && isCategoryEnabled("error")) {
            errorLogger.error("❌ ASSERTION ERROR: Expected [" + expected + "] but got [" + actual + "]");
        }
    }
    
    // ===========================================
    // TEST EXECUTION LOGGING METHODS
    // ===========================================
    
    /**
     * Log test start
     */
    public static void testStart(String testName) {
        if (isCategoryEnabled("test.execution")) {
            testExecutionLogger.info("🚀 TEST STARTED: " + testName);
        }
    }
    
    /**
     * Log test end
     */
    public static void testEnd(String testName, String status) {
        if (isCategoryEnabled("test.execution")) {
            testExecutionLogger.info("🏁 TEST ENDED: " + testName + " - Status: " + status);
        }
    }
    
    /**
     * Log test step
     */
    public static void testStep(String step) {
        if (isCategoryEnabled("test.execution")) {
            testExecutionLogger.info("📋 TEST STEP: " + step);
        }
    }
    
    // ===========================================
    // PERFORMANCE LOGGING METHODS
    // ===========================================
    
    /**
     * Log performance metric
     */
    public static void performance(String operation, long duration) {
        if (Boolean.parseBoolean(config.getProperty("logging.performance.enabled", "true"))) {
            long threshold = Long.parseLong(config.getProperty("logging.performance.threshold.ms", "1000"));
            if (duration > threshold) {
                performanceWarning(operation, duration);
            } else {
                info("Performance: " + operation + " completed in " + duration + "ms");
            }
        }
    }
    
    // ===========================================
    // BROWSER LOGGING METHODS
    // ===========================================
    
    /**
     * Log browser action
     */
    public static void browserAction(String action) {
        if (Boolean.parseBoolean(config.getProperty("logging.browser.enabled", "true"))) {
            info("🌐 BROWSER: " + action);
        }
    }
    
    /**
     * Log page load
     */
    public static void pageLoad(String url) {
        if (Boolean.parseBoolean(config.getProperty("logging.browser.enabled", "true"))) {
            info("📄 PAGE LOAD: " + url);
        }
    }
    
    // ===========================================
    // API LOGGING METHODS
    // ===========================================
    
    /**
     * Log API request
     */
    public static void apiRequest(String method, String url) {
        if (Boolean.parseBoolean(config.getProperty("logging.api.enabled", "true"))) {
            info("🌍 API REQUEST: " + method + " " + url);
        }
    }
    
    /**
     * Log API response
     */
    public static void apiResponse(String method, String url, int statusCode) {
        if (Boolean.parseBoolean(config.getProperty("logging.api.enabled", "true"))) {
            info("🌍 API RESPONSE: " + method + " " + url + " - Status: " + statusCode);
        }
    }
    
    // ===========================================
    // UTILITY METHODS
    // ===========================================
    
    /**
     * Get current log level
     */
    public static String getCurrentLogLevel() {
        return currentLogLevel;
    }
    
    /**
     * Set log level dynamically
     */
    public static void setLogLevel(String level) {
        currentLogLevel = level;
        info("Log level changed to: " + level);
    }
    
    /**
     * Get timestamp for logging
     */
    public static String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }
    
    /**
     * Log separator for better readability
     */
    public static void separator(String title) {
        if (shouldLog("INFO") && isCategoryEnabled("info")) {
            infoLogger.info("=".repeat(80));
            infoLogger.info(" " + title);
            infoLogger.info("=".repeat(80));
        }
    }
    
    /**
     * Log test suite start
     */
    public static void testSuiteStart(String suiteName) {
        separator("TEST SUITE STARTED: " + suiteName);
    }
    
    /**
     * Log test suite end
     */
    public static void testSuiteEnd(String suiteName) {
        separator("TEST SUITE ENDED: " + suiteName);
    }
} 