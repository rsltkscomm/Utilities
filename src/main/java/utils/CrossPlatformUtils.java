package utils;

import reporting.TestLogManager;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for cross-platform operations.
 * Provides OS-agnostic methods for common operations.
 */
public class CrossPlatformUtils {
    
    public enum OperatingSystem {
        WINDOWS, MACOS, LINUX, UNKNOWN
    }
    
    private static final Map<String, String> OS_SPECIFIC_PATHS = new HashMap<>();
    private static final Map<String, String> OS_SPECIFIC_BROWSER_PATHS = new HashMap<>();
    
    static {
        initializeOSSpecificPaths();
        initializeBrowserPaths();
    }
    
    /**
     * Gets the current operating system.
     * @return OperatingSystem enum value
     */
    public static OperatingSystem getCurrentOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains("mac")) {
            return OperatingSystem.MACOS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }
    
    /**
     * Gets the current operating system (alias for getCurrentOS).
     * @return OperatingSystem enum value
     */
    public static OperatingSystem getCurrentOperatingSystem() {
        return getCurrentOS();
    }
    
    /**
     * Gets environment variables for the current OS.
     * @return Map of environment variables
     */
    public static Map<String, String> getEnvironmentVariables() {
        return getOSEnvironmentVariables();
    }
    
    /**
     * Gets the default browser for the current OS.
     * @return Default browser name
     */
    public static String getDefaultBrowser() {
        OperatingSystem os = getCurrentOS();
        switch (os) {
            case WINDOWS:
                return "chrome";
            case MACOS:
                return "chrome";
            case LINUX:
                return "firefox";
            default:
                return "chrome";
        }
    }
    
    /**
     * Gets OS-specific path separator.
     * @return Path separator for current OS
     */
    public static String getPathSeparator() {
        return File.separator;
    }
    
    /**
     * Gets OS-specific line separator.
     * @return Line separator for current OS
     */
    public static String getLineSeparator() {
        return System.lineSeparator();
    }
    
    /**
     * Creates a cross-platform path.
     * @param pathParts Path components
     * @return Cross-platform Path object
     */
    public static Path createPath(String... pathParts) {
        return Paths.get("", pathParts);
    }
    
    /**
     * Gets the user's home directory.
     * @return Path to user home directory
     */
    public static Path getUserHome() {
        return Paths.get(System.getProperty("user.home"));
    }
    
    /**
     * Gets the current working directory.
     * @return Path to current working directory
     */
    public static Path getCurrentDirectory() {
        return Paths.get(System.getProperty("user.dir"));
    }
    
    /**
     * Gets the temporary directory for the current OS.
     * @return Path to temp directory
     */
    public static Path getTempDirectory() {
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }
    
    /**
     * Gets OS-specific download directory.
     * @return Path to download directory
     */
    public static Path getDownloadDirectory() {
        OperatingSystem os = getCurrentOS();
        String downloadPath = OS_SPECIFIC_PATHS.get(os.name());
        
        if (downloadPath != null) {
            return Paths.get(downloadPath);
        }
        
        // Fallback to user home/Downloads
        return getUserHome().resolve("Downloads");
    }
    
    /**
     * Gets the project's data directory.
     * @return Path to project data directory
     */
    public static Path getProjectDataDirectory() {
        return getCurrentDirectory()
                .resolve("src")
                .resolve("main")
                .resolve("resources")
                .resolve("data");
    }
    
    /**
     * Gets the project's download directory.
     * @return Path to project download directory
     */
    public static Path getProjectDownloadDirectory() {
        return getProjectDataDirectory().resolve("downloadedFile");
    }
    
    /**
     * Gets OS-specific browser executable path.
     * @param browser Browser name (chrome, firefox, edge)
     * @return Path to browser executable or null if not found
     */
    public static Path getBrowserExecutablePath(String browser) {
        OperatingSystem os = getCurrentOS();
        String key = os.name() + "_" + browser.toLowerCase();
        String path = OS_SPECIFIC_BROWSER_PATHS.get(key);
        
        if (path != null) {
            Path browserPath = Paths.get(path);
            if (browserPath.toFile().exists()) {
                return browserPath;
            }
        }
        
        return null;
    }
    
    /**
     * Checks if a command is available in the system PATH.
     * @param command Command to check
     * @return true if command is available, false otherwise
     */
    public static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(getCommandForOS(command));
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets OS-specific command for checking command availability.
     * @param command Command to check
     * @return OS-specific command array
     */
    private static String[] getCommandForOS(String command) {
        OperatingSystem os = getCurrentOS();
        
        switch (os) {
            case WINDOWS:
                return new String[]{"where", command};
            case MACOS:
            case LINUX:
                return new String[]{"which", command};
            default:
                return new String[]{"which", command};
        }
    }
    
    /**
     * Gets OS-specific environment variables.
     * @return Map of environment variables
     */
    public static Map<String, String> getOSEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        
        OperatingSystem os = getCurrentOS();
        switch (os) {
            case WINDOWS:
                envVars.put("PATH", System.getenv("PATH"));
                envVars.put("USERPROFILE", System.getenv("USERPROFILE"));
                envVars.put("APPDATA", System.getenv("APPDATA"));
                break;
            case MACOS:
                envVars.put("PATH", System.getenv("PATH"));
                envVars.put("HOME", System.getenv("HOME"));
                envVars.put("USER", System.getenv("USER"));
                break;
            case LINUX:
                envVars.put("PATH", System.getenv("PATH"));
                envVars.put("HOME", System.getenv("HOME"));
                envVars.put("USER", System.getenv("USER"));
                envVars.put("XDG_CONFIG_HOME", System.getenv("XDG_CONFIG_HOME"));
                break;
        }
        
        return envVars;
    }
    
    /**
     * Gets OS-specific system information.
     * @return Map of system information
     */
    public static Map<String, String> getSystemInfo() {
        Map<String, String> systemInfo = new HashMap<>();
        
        systemInfo.put("os.name", System.getProperty("os.name"));
        systemInfo.put("os.version", System.getProperty("os.version"));
        systemInfo.put("os.arch", System.getProperty("os.arch"));
        systemInfo.put("java.version", System.getProperty("java.version"));
        systemInfo.put("java.vendor", System.getProperty("java.vendor"));
        systemInfo.put("user.name", System.getProperty("user.name"));
        systemInfo.put("user.home", System.getProperty("user.home"));
        systemInfo.put("user.dir", System.getProperty("user.dir"));
        
        return systemInfo;
    }
    
    /**
     * Logs system information for debugging.
     */
    public static void logSystemInfo() {
        TestLogManager.info("=== System Information ===");
        OperatingSystem os = getCurrentOS();
        TestLogManager.info("Operating System: " + os);
        
        Map<String, String> systemInfo = getSystemInfo();
        systemInfo.forEach((key, value) -> 
            TestLogManager.info(key + ": " + value)
        );
        
        TestLogManager.info("Download Directory: " + getDownloadDirectory());
        TestLogManager.info("Project Data Directory: " + getProjectDataDirectory());
        TestLogManager.info("=== End System Information ===");
    }
    
    private static void initializeOSSpecificPaths() {
        // Windows paths
        OS_SPECIFIC_PATHS.put("WINDOWS", System.getenv("USERPROFILE") + "\\Downloads");
        
        // macOS paths
        OS_SPECIFIC_PATHS.put("MACOS", System.getProperty("user.home") + "/Downloads");
        
        // Linux paths
        OS_SPECIFIC_PATHS.put("LINUX", System.getProperty("user.home") + "/Downloads");
    }
    
    private static void initializeBrowserPaths() {
        OperatingSystem os = getCurrentOS();
        
        switch (os) {
            case WINDOWS:
                OS_SPECIFIC_BROWSER_PATHS.put("WINDOWS_chrome", 
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
                OS_SPECIFIC_BROWSER_PATHS.put("WINDOWS_firefox", 
                    "C:\\Program Files\\Mozilla Firefox\\firefox.exe");
                OS_SPECIFIC_BROWSER_PATHS.put("WINDOWS_edge", 
                    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
                break;
                
            case MACOS:
                OS_SPECIFIC_BROWSER_PATHS.put("MACOS_chrome", 
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
                OS_SPECIFIC_BROWSER_PATHS.put("MACOS_firefox", 
                    "/Applications/Firefox.app/Contents/MacOS/firefox");
                OS_SPECIFIC_BROWSER_PATHS.put("MACOS_edge", 
                    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
                break;
                
            case LINUX:
                OS_SPECIFIC_BROWSER_PATHS.put("LINUX_chrome", "/usr/bin/google-chrome");
                OS_SPECIFIC_BROWSER_PATHS.put("LINUX_firefox", "/usr/bin/firefox");
                OS_SPECIFIC_BROWSER_PATHS.put("LINUX_edge", "/usr/bin/microsoft-edge");
                break;
        }
    }
}
