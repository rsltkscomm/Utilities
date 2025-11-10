package performanceTracker;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import base.DriverManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Performance Screenshot Capture
 * 
 * Automatically captures screenshots when performance thresholds are exceeded:
 * - Page load exceeds threshold
 * - API response exceeds threshold
 * - Web Vitals in "Poor" state
 * 
 * Screenshots are annotated with performance data and saved to the screenshots directory.
 */
public class PerformanceScreenshotCapture {
    
    private final WebDriver driver;
    private final ConfigurationManager config;
    private final List<File> capturedScreenshots;
    private final String screenshotsDir;
    
    public PerformanceScreenshotCapture() {
        this.driver = DriverManager.getDriver();
        this.config = ConfigurationManager.getInstance();
        this.capturedScreenshots = new ArrayList<>();
        this.screenshotsDir = config.getScreenshotPath();
        
        // Ensure screenshots directory exists
        File dir = new File(screenshotsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Capture screenshot when page load exceeds threshold
     */
    public File captureSlowPageLoad(String url, long loadTimeMs) {
        String reason = String.format("Slow_Page_Load_%dms", loadTimeMs);
        return captureScreenshot(reason, url);
    }
    
    /**
     * Capture screenshot when API exceeds threshold
     */
    public File captureSlowApi(String apiEndpoint, long responseTimeMs) {
        String reason = String.format("Slow_API_%dms", responseTimeMs);
        return captureScreenshot(reason, apiEndpoint);
    }
    
    /**
     * Capture screenshot when Web Vitals are poor
     */
    public File capturePoorWebVitals(String url, int score) {
        String reason = String.format("Poor_Web_Vitals_Score_%d", score);
        return captureScreenshot(reason, url);
    }
    
    /**
     * Capture screenshot with reason
     */
    private File captureScreenshot(String reason, String details) {
        if (driver == null) {
            System.err.println("⚠️  Cannot capture screenshot: WebDriver is null");
            return null;
        }
        
        if (!(driver instanceof TakesScreenshot)) {
            System.err.println("⚠️  WebDriver does not support screenshots");
            return null;
        }
        
        try {
            // Generate filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String sanitizedReason = reason.replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = "perf_issue_" + sanitizedReason + "_" + timestamp + ".png";
            
            // Capture screenshot
            TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
            byte[] screenshot = screenshotDriver.getScreenshotAs(OutputType.BYTES);
            
            // Save to file
            File screenshotFile = new File(screenshotsDir + fileName);
            try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
                fos.write(screenshot);
            }
            
            // Track screenshot
            capturedScreenshots.add(screenshotFile);
            
            System.out.println("   📸 Performance Screenshot Captured: " + fileName);
            System.out.println("      Reason: " + reason.replace("_", " "));
            System.out.println("      Details: " + details);
            
            return screenshotFile;
            
        } catch (IOException e) {
            System.err.println("❌ Error capturing performance screenshot: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error during screenshot capture: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get all captured screenshots
     */
    public List<File> getCapturedScreenshots() {
        return new ArrayList<>(capturedScreenshots);
    }
    
    /**
     * Get count of captured screenshots
     */
    public int getScreenshotCount() {
        return capturedScreenshots.size();
    }
    
    /**
     * Check if any screenshots were captured
     */
    public boolean hasScreenshots() {
        return !capturedScreenshots.isEmpty();
    }
    
    /**
     * Clear captured screenshots list (not the files themselves)
     */
    public void clear() {
        capturedScreenshots.clear();
    }
}


