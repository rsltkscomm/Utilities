package utils;

import reporting.TestLogManager;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for cross-platform operations.
 * This provides platform-independent methods for common operations.
 */
public class CrossPlatformUtils {
    
    public enum OperatingSystem {
        WINDOWS, MAC, LINUX, UNKNOWN
    }
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String USER_DIR = System.getProperty("user.dir");
    
    /**
     * Gets the current operating system.
     * @return Operating system enum
     */
    public static OperatingSystem getCurrentOS() {
        if (OS_NAME.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (OS_NAME.contains("mac")) {
            return OperatingSystem.MAC;
        } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }
    
    /**
     * Gets the project data directory.
     * @return Path to project data directory
     */
    public static Path getProjectDataDirectory() {
        return Paths.get(USER_DIR, "src", "main", "resources", "data");
    }
    
    /**
     * Gets the project download directory.
     * @return Path to project download directory
     */
    public static Path getProjectDownloadDirectory() {
        return Paths.get(USER_DIR, "src", "main", "resources", "data", "downloadedFile");
    }
    
    /**
     * Gets OS-specific environment variables.
     * @return Map of environment variables
     */
    public static Map<String, String> getOSEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        
        // Common environment variables
        envVars.put("JAVA_HOME", System.getenv("JAVA_HOME"));
        envVars.put("PATH", System.getenv("PATH"));
        envVars.put("USER_HOME", USER_HOME);
        envVars.put("USER_DIR", USER_DIR);
        envVars.put("OS_NAME", OS_NAME);
        
        // OS-specific variables
        OperatingSystem os = getCurrentOS();
        switch (os) {
            case WINDOWS:
                envVars.put("APPDATA", System.getenv("APPDATA"));
                envVars.put("LOCALAPPDATA", System.getenv("LOCALAPPDATA"));
                envVars.put("TEMP", System.getenv("TEMP"));
                break;
            case MAC:
                envVars.put("HOME", System.getenv("HOME"));
                envVars.put("TMPDIR", System.getenv("TMPDIR"));
                break;
            case LINUX:
                envVars.put("HOME", System.getenv("HOME"));
                envVars.put("TMPDIR", System.getenv("TMPDIR"));
                envVars.put("XDG_CONFIG_HOME", System.getenv("XDG_CONFIG_HOME"));
                break;
        }
        
        return envVars;
    }
    
    /**
     * Gets environment variables (alias for getOSEnvironmentVariables).
     * @return Map of environment variables
     */
    public static Map<String, String> getEnvironmentVariables() {
        return getOSEnvironmentVariables();
    }
    
    /**
     * Gets the browser executable path for the given browser.
     * @param browserName Browser name (chrome, firefox, edge)
     * @return Path to browser executable or null if not found
     */
    public static Path getBrowserExecutablePath(String browserName) {
        OperatingSystem os = getCurrentOS();
        String browser = browserName.toLowerCase();
        
        switch (os) {
            case WINDOWS:
                return getWindowsBrowserPath(browser);
            case MAC:
                return getMacBrowserPath(browser);
            case LINUX:
                return getLinuxBrowserPath(browser);
            default:
                TestLogManager.warning("Unknown operating system: " + os);
                return null;
        }
    }
    
    /**
     * Logs system information.
     */
    public static void logSystemInfo() {
        TestLogManager.info("=== System Information ===");
        TestLogManager.info("Operating System: " + getCurrentOS());
        TestLogManager.info("OS Name: " + OS_NAME);
        TestLogManager.info("Java Version: " + System.getProperty("java.version"));
        TestLogManager.info("Java Home: " + System.getProperty("java.home"));
        TestLogManager.info("User Home: " + USER_HOME);
        TestLogManager.info("User Directory: " + USER_DIR);
        TestLogManager.info("Architecture: " + System.getProperty("os.arch"));
        TestLogManager.info("Available Processors: " + Runtime.getRuntime().availableProcessors());
        TestLogManager.info("Total Memory: " + Runtime.getRuntime().totalMemory() / (1024 * 1024) + " MB");
        TestLogManager.info("Free Memory: " + Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
    }
    
    /**
     * Creates a directory if it doesn't exist.
     * @param path Path to create
     * @return true if successful, false otherwise
     */
    public static boolean createDirectoryIfNotExists(Path path) {
        try {
            if (!path.toFile().exists()) {
                boolean created = path.toFile().mkdirs();
                if (created) {
                    TestLogManager.info("Created directory: " + path);
                } else {
                    TestLogManager.warning("Failed to create directory: " + path);
                }
                return created;
            }
            return true;
        } catch (Exception e) {
            TestLogManager.error("Error creating directory: " + path, e);
            return false;
        }
    }
    
    /**
     * Checks if a file exists.
     * @param path Path to check
     * @return true if exists, false otherwise
     */
    public static boolean fileExists(Path path) {
        return path.toFile().exists();
    }
    
    /**
     * Checks if a directory exists.
     * @param path Path to check
     * @return true if exists and is directory, false otherwise
     */
    public static boolean directoryExists(Path path) {
        File file = path.toFile();
        return file.exists() && file.isDirectory();
    }
    
    /**
     * Gets the file separator for the current OS.
     * @return File separator
     */
    public static String getFileSeparator() {
        return File.separator;
    }
    
    /**
     * Gets the path separator for the current OS.
     * @return Path separator
     */
    public static String getPathSeparator() {
        return File.pathSeparator;
    }
    
    private static Path getWindowsBrowserPath(String browser) {
        String programFiles = System.getenv("PROGRAMFILES");
        String programFilesX86 = System.getenv("PROGRAMFILES(X86)");
        
        switch (browser) {
            case "chrome":
                // Try different possible locations
                String[] chromePaths = {
                    programFiles + "\\Google\\Chrome\\Application\\chrome.exe",
                    programFilesX86 + "\\Google\\Chrome\\Application\\chrome.exe",
                    System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe"
                };
                return findFirstExistingPath(chromePaths);
                
            case "firefox":
                String[] firefoxPaths = {
                    programFiles + "\\Mozilla Firefox\\firefox.exe",
                    programFilesX86 + "\\Mozilla Firefox\\firefox.exe"
                };
                return findFirstExistingPath(firefoxPaths);
                
            case "edge":
                String[] edgePaths = {
                    programFiles + "\\Microsoft\\Edge\\Application\\msedge.exe",
                    programFilesX86 + "\\Microsoft\\Edge\\Application\\msedge.exe"
                };
                return findFirstExistingPath(edgePaths);
                
            default:
                return null;
        }
    }
    
    private static Path getMacBrowserPath(String browser) {
        switch (browser) {
            case "chrome":
                return Paths.get("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            case "firefox":
                return Paths.get("/Applications/Firefox.app/Contents/MacOS/firefox");
            case "safari":
                return Paths.get("/Applications/Safari.app/Contents/MacOS/Safari");
            default:
                return null;
        }
    }
    
    private static Path getLinuxBrowserPath(String browser) {
        switch (browser) {
            case "chrome":
            case "chromium":
                String[] chromePaths = {
                    "/usr/bin/google-chrome",
                    "/usr/bin/chromium-browser",
                    "/usr/bin/chromium",
                    "/snap/bin/chromium"
                };
                return findFirstExistingPath(chromePaths);
                
            case "firefox":
                String[] firefoxPaths = {
                    "/usr/bin/firefox",
                    "/usr/bin/firefox-esr",
                    "/snap/bin/firefox"
                };
                return findFirstExistingPath(firefoxPaths);
                
            default:
                return null;
        }
    }
    
    private static Path findFirstExistingPath(String[] paths) {
        for (String pathStr : paths) {
            if (pathStr != null) {
                Path path = Paths.get(pathStr);
                if (path.toFile().exists()) {
                    return path;
                }
            }
        }
        return null;
    }
}