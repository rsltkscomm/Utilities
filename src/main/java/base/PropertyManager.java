package base;

import config.ConfigurationManager;
import reporting.ExtentManager;
import reporting.TestLogManager;

import java.io.*;
import java.util.*;

import com.aventstack.extentreports.Status;

import constants.FrameworkConstants;

public class PropertyManager {

    private static final Properties properties = new Properties();
    private static boolean initialized = false;
    private static ConfigurationManager configManager;

    private PropertyManager() {
        // prevent instantiation
    }
    /**
     * Initialize once (Singleton).
     * Now integrates with ConfigurationManager for backward compatibility
     */
    public static synchronized void init(String folderPath) {
        if (initialized) return;

        // Initialize ConfigurationManager first
//        configManager = ConfigurationManager.getInstance();
        
        if (folderPath != null) {
            readAllProperties(folderPath);
        }
        setDefaultProperties();
        initialized = true;
        TestLogManager.info("PropertyManager initialized with ConfigurationManager integration");
    }

    /**
     * Load a single property file.
     */
    public static void load(String propertyFile) {
        try (InputStream input = new FileInputStream(propertyFile)) {
            properties.load(input);
            System.getProperties().putAll(properties);
            TestLogManager.info("Loaded properties from: " + propertyFile);
        } catch (IOException e) {
            TestLogManager.error("Failed to load property file: " + propertyFile, e);
        }
    }

    /**
     * Load all .properties files from a folder.
     */
    private static void readAllProperties(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".properties"));

        if (files == null || files.length == 0) {
            TestLogManager.warning("No property files found in: " + folderPath);
            return;
        }

        for (File file : files) {
            load(file.getAbsolutePath());
        }
    }

    /**
     * Set default properties.
     */
    private static void setDefaultProperties() {
        properties.setProperty("klovpropertyFile", FrameworkConstants.KLOV_PROPERTIES_PATH);
        System.getProperties().putAll(properties);
    }

    /**
     * Get property with default fallback.
     * Now uses ConfigurationManager for enhanced functionality while maintaining backward compatibility
     */
    public static String get(String key, String defaultValue) {
        // Try ConfigurationManager first if available
        if (configManager != null) {
            String value = configManager.getString(key, defaultValue);
            if (value != null) {
                return value;
            }
        }
        
        // Fallback to legacy properties
        return properties.getProperty(key, defaultValue);
    }

    public static String get(String key) {
        // Try ConfigurationManager first if available
        if (configManager != null) {
            String value = configManager.getString(key);
            if (value != null) {
                return value;
            }
        }
        
        // Fallback to legacy properties
        return properties.getProperty(key);
    }
    
    /**
     * Get boolean property
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        if (configManager != null) {
            return configManager.getBoolean(key, defaultValue);
        }
        
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    /**
     * Get integer property
     */
    public static int getInt(String key, int defaultValue) {
        if (configManager != null) {
            return configManager.getInt(key, defaultValue);
        }
        
        String value = properties.getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            TestLogManager.warning("Invalid integer value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Detect OS-specific root path.
     */
    public static String getPropFileRoot() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getProperty("Root_Windows");
        } else if (os.contains("nix") || os.contains("nux")) {
            return System.getProperty("Root_Linux");
        }
        return null;
    }

    /**
     * Extract resource file to temp path.
     */
    public static String extractResourceToTempFile(String resourcePath, String suffix) {
        File tempFile = null;
        try (InputStream is = PropertyManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + resourcePath);

            tempFile = File.createTempFile("resource_", suffix);
            tempFile.deleteOnExit();

            try (FileOutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer, 0, length);
                }
            }
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            TestLogManager.error("Failed to extract resource: " + resourcePath, e);
    		ExtentManager.getTest().log(Status.INFO, "==== Test Suite Finished ====");
            return null;
        }
    }
}
