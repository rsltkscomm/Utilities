package smartUIComparison;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages baseline images for visual comparison
 */
public class BaselineManager {
    
    private static final String BASELINE_DIR = "src/test/resources/baselines/";
    private static final String HISTORY_DIR = "src/test/resources/baselines_history/";
    private static final String ACTUAL_DIR = "target/ui-actuals/";
    private static final String DIFF_DIR = "target/ui-diffs/";
    
    /**
     * Ensure all required directories exist
     */
    public static void ensureDirectoriesExist() {
        createDirectoryIfNotExists(BASELINE_DIR);
        createDirectoryIfNotExists(HISTORY_DIR);
        createDirectoryIfNotExists(ACTUAL_DIR);
        createDirectoryIfNotExists(DIFF_DIR);
    }
    
    /**
     * Create directory if it doesn't exist
     */
    private static void createDirectoryIfNotExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("Created directory: " + path);
            } else {
                System.err.println("Failed to create directory: " + path);
            }
        }
    }
    
    /**
     * Get the baseline image path for a test
     */
    public static String getBaselinePath(String testName) {
        return BASELINE_DIR + testName + ".png";
    }
    
    /**
     * Get the actual screenshot path for a test
     */
    public static String getActualPath(String testName) {
        return ACTUAL_DIR + testName + ".png";
    }
    
    /**
     * Get the difference image path for a test
     */
    public static String getDiffPath(String testName) {
        return DIFF_DIR + testName + "_diff.png";
    }
    
    /**
     * Check if baseline exists for a test
     */
    public static boolean baselineExists(String testName) {
        File baselineFile = new File(getBaselinePath(testName));
        return baselineFile.exists() && baselineFile.length() > 0;
    }
    
    /**
     * Create a new baseline from the actual screenshot
     */
    public static void createBaseline(String testName, String actualImagePath) {
        try {
            File sourceFile = new File(actualImagePath);
            File targetFile = new File(getBaselinePath(testName));
            
            // Ensure target directory exists
            targetFile.getParentFile().mkdirs();
            
            // Copy actual image to baseline location
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Created baseline for test: " + testName);
            
        } catch (IOException e) {
            System.err.println("Error creating baseline for test " + testName + ": " + e.getMessage());
        }
    }
    
    /**
     * Update baseline with history backup
     */
    public static void updateBaselineWithHistory(String testName, String newActualImagePath) {
        try {
            String baselinePath = getBaselinePath(testName);
            String historyPath = getHistoryPath(testName);
            
            // Backup current baseline to history
            File currentBaseline = new File(baselinePath);
            if (currentBaseline.exists()) {
                File historyFile = new File(historyPath);
                historyFile.getParentFile().mkdirs();
                Files.copy(currentBaseline.toPath(), historyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Backed up baseline to history: " + historyPath);
            }
            
            // Update baseline with new image
            createBaseline(testName, newActualImagePath);
            System.out.println("Updated baseline for test: " + testName);
            
        } catch (IOException e) {
            System.err.println("Error updating baseline for test " + testName + ": " + e.getMessage());
        }
    }
    
    /**
     * Get history path for a test
     */
    private static String getHistoryPath(String testName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = dateFormat.format(new Date());
        return HISTORY_DIR + testName + "_" + timestamp + ".png";
    }
    
    /**
     * Get the latest history backup path for a test
     */
    public static String getLatestHistoryBackup(String testName) {
        File historyDir = new File(HISTORY_DIR);
        if (!historyDir.exists()) {
            return null;
        }
        
        File[] historyFiles = historyDir.listFiles((dir, name) -> 
            name.startsWith(testName + "_") && name.endsWith(".png"));
        
        if (historyFiles == null || historyFiles.length == 0) {
            return null;
        }
        
        // Return the most recent file (assuming timestamp format)
        File latestFile = historyFiles[0];
        for (File file : historyFiles) {
            if (file.lastModified() > latestFile.lastModified()) {
                latestFile = file;
            }
        }
        
        return latestFile.getAbsolutePath();
    }
    
    /**
     * Clean up old actual and diff images
     */
    public static void cleanupOldImages() {
        cleanupDirectory(ACTUAL_DIR, 7); // Keep actual images for 7 days
        cleanupDirectory(DIFF_DIR, 3);   // Keep diff images for 3 days
    }
    
    /**
     * Clean up files older than specified days in a directory
     */
    private static void cleanupDirectory(String directoryPath, int daysToKeep) {
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        int deletedCount = 0;
        
        for (File file : files) {
            if (file.isFile() && file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deletedCount++;
                }
            }
        }
        
        if (deletedCount > 0) {
            System.out.println("Cleaned up " + deletedCount + " old files from " + directoryPath);
        }
    }
    
    /**
     * Get baseline directory path
     */
    public static String getBaselineDir() {
        return BASELINE_DIR;
    }
    
    /**
     * Get actual images directory path
     */
    public static String getActualDir() {
        return ACTUAL_DIR;
    }
    
    /**
     * Get difference images directory path
     */
    public static String getDiffDir() {
        return DIFF_DIR;
    }
    
    /**
     * Get history directory path
     */
    public static String getHistoryDir() {
        return HISTORY_DIR;
    }
}