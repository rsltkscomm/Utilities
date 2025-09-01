package base;

import reporting.ExtentManager;
import reporting.TestLogManager;

import java.io.*;
import java.util.*;

import com.aventstack.extentreports.Status;

import constants.FrameworkConstants;

public class PropertyManager {

    private static final Properties properties = new Properties();
    private static boolean initialized = false;

    private PropertyManager() {
        // prevent instantiation
    }
    /**
     * Initialize once (Singleton).
     */
    public static synchronized void init(String folderPath) {
        if (initialized) return;

        if (folderPath != null) {
            readAllProperties(folderPath);
        }
        setDefaultProperties();
        initialized = true;
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
        properties.setProperty("klovpropertyFile", FrameworkConstants.KLOV_PROPERTIES_FILEPATH);
        System.getProperties().putAll(properties);
    }

    /**
     * Get property with default fallback.
     */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static String get(String key) {
        return properties.getProperty(key);
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
