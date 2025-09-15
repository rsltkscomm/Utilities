package advanced;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Advanced visual testing utilities for screenshot comparison and visual regression testing.
 */
public class VisualTestingUtil {
    
    private final WebDriver driver;
    private final String screenshotDirectory;
    
    public VisualTestingUtil(WebDriver driver) {
        this.driver = driver;
        this.screenshotDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("screenshots").toString();
        createScreenshotDirectory();
    }
    
    /**
     * Captures a full page screenshot.
     * @param fileName Name of the screenshot file
     * @return Path to the saved screenshot
     */
    public Path captureFullPageScreenshot(String fileName) {
        try {
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            byte[] screenshot = takesScreenshot.getScreenshotAs(OutputType.BYTES);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fullFileName = fileName + "_" + timestamp + ".png";
            Path screenshotPath = Paths.get(screenshotDirectory, fullFileName);
            
            Files.write(screenshotPath, screenshot);
            TestLogManager.info("Screenshot captured: " + screenshotPath);
            
            return screenshotPath;
            
        } catch (IOException e) {
            TestLogManager.error("Failed to capture screenshot: " + fileName, e);
            throw new RuntimeException("Screenshot capture failed", e);
        }
    }
    
    /**
     * Captures a screenshot of a specific element.
     * @param element WebElement to capture
     * @param fileName Name of the screenshot file
     * @return Path to the saved screenshot
     */
    public Path captureElementScreenshot(WebElement element, String fileName) {
        try {
            byte[] screenshot = element.getScreenshotAs(OutputType.BYTES);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fullFileName = fileName + "_element_" + timestamp + ".png";
            Path screenshotPath = Paths.get(screenshotDirectory, fullFileName);
            
            Files.write(screenshotPath, screenshot);
            TestLogManager.info("Element screenshot captured: " + screenshotPath);
            
            return screenshotPath;
            
        } catch (IOException e) {
            TestLogManager.error("Failed to capture element screenshot: " + fileName, e);
            throw new RuntimeException("Element screenshot capture failed", e);
        }
    }
    
    /**
     * Compares two screenshots for visual differences.
     * @param baselinePath Path to baseline screenshot
     * @param currentPath Path to current screenshot
     * @return VisualComparisonResult containing comparison details
     */
    public VisualComparisonResult compareScreenshots(String baselinePath, String currentPath) {
        try {
            BufferedImage baseline = ImageIO.read(new File(baselinePath));
            BufferedImage current = ImageIO.read(new File(currentPath));
            
            return compareImages(baseline, current, baselinePath, currentPath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to compare screenshots", e);
            return new VisualComparisonResult(false, 0, "Error reading images: " + e.getMessage());
        }
    }
    
    /**
     * Compares two screenshots with tolerance for minor differences.
     * @param baselinePath Path to baseline screenshot
     * @param currentPath Path to current screenshot
     * @param tolerance Tolerance level (0.0 to 1.0)
     * @return VisualComparisonResult containing comparison details
     */
    public VisualComparisonResult compareScreenshotsWithTolerance(String baselinePath, String currentPath, double tolerance) {
        try {
            BufferedImage baseline = ImageIO.read(new File(baselinePath));
            BufferedImage current = ImageIO.read(new File(currentPath));
            
            return compareImagesWithTolerance(baseline, current, baselinePath, currentPath, tolerance);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to compare screenshots with tolerance", e);
            return new VisualComparisonResult(false, 0, "Error reading images: " + e.getMessage());
        }
    }
    
    /**
     * Highlights differences between two screenshots.
     * @param baselinePath Path to baseline screenshot
     * @param currentPath Path to current screenshot
     * @param outputPath Path to save the difference image
     * @return Path to the difference image
     */
    public Path highlightDifferences(String baselinePath, String currentPath, String outputPath) {
        try {
            BufferedImage baseline = ImageIO.read(new File(baselinePath));
            BufferedImage current = ImageIO.read(new File(currentPath));
            
            BufferedImage difference = createDifferenceImage(baseline, current);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fullFileName = outputPath + "_difference_" + timestamp + ".png";
            Path diffPath = Paths.get(screenshotDirectory, fullFileName);
            
            ImageIO.write(difference, "PNG", diffPath.toFile());
            TestLogManager.info("Difference image created: " + diffPath);
            
            return diffPath;
            
        } catch (IOException e) {
            TestLogManager.error("Failed to create difference image", e);
            throw new RuntimeException("Difference image creation failed", e);
        }
    }
    
    /**
     * Performs visual regression testing.
     * @param testName Name of the test
     * @param baselineFileName Baseline screenshot filename
     * @return VisualRegressionResult containing test results
     */
    public VisualRegressionResult performVisualRegressionTest(String testName, String baselineFileName) {
        TestLogManager.info("Starting visual regression test: " + testName);
        
        // Capture current screenshot
        Path currentScreenshot = captureFullPageScreenshot(testName + "_current");
        
        // Look for baseline screenshot
        Path baselinePath = Paths.get(screenshotDirectory, baselineFileName);
        
        if (!baselinePath.toFile().exists()) {
            TestLogManager.warning("Baseline screenshot not found: " + baselineFileName);
            return new VisualRegressionResult(false, "Baseline screenshot not found", currentScreenshot, null);
        }
        
        // Compare screenshots
        VisualComparisonResult comparison = compareScreenshots(baselinePath.toString(), currentScreenshot.toString());
        
        if (!comparison.isMatch()) {
            // Create difference image
            Path diffImage = highlightDifferences(baselinePath.toString(), currentScreenshot.toString(), testName);
            return new VisualRegressionResult(false, "Visual differences detected", currentScreenshot, diffImage);
        }
        
        TestLogManager.success("Visual regression test passed: " + testName);
        return new VisualRegressionResult(true, "No visual differences detected", currentScreenshot, null);
    }
    
    private VisualComparisonResult compareImages(BufferedImage baseline, BufferedImage current, String baselinePath, String currentPath) {
        if (baseline.getWidth() != current.getWidth() || baseline.getHeight() != current.getHeight()) {
            return new VisualComparisonResult(false, 0, "Image dimensions do not match");
        }
        
        int totalPixels = baseline.getWidth() * baseline.getHeight();
        int differentPixels = 0;
        
        for (int x = 0; x < baseline.getWidth(); x++) {
            for (int y = 0; y < baseline.getHeight(); y++) {
                if (baseline.getRGB(x, y) != current.getRGB(x, y)) {
                    differentPixels++;
                }
            }
        }
        
        double differencePercentage = (double) differentPixels / totalPixels * 100;
        boolean isMatch = differentPixels == 0;
        
        return new VisualComparisonResult(isMatch, differencePercentage, 
                String.format("Found %d different pixels (%.2f%%)", differentPixels, differencePercentage));
    }
    
    private VisualComparisonResult compareImagesWithTolerance(BufferedImage baseline, BufferedImage current, 
                                                           String baselinePath, String currentPath, double tolerance) {
        if (baseline.getWidth() != current.getWidth() || baseline.getHeight() != current.getHeight()) {
            return new VisualComparisonResult(false, 0, "Image dimensions do not match");
        }
        
        int totalPixels = baseline.getWidth() * baseline.getHeight();
        int differentPixels = 0;
        
        for (int x = 0; x < baseline.getWidth(); x++) {
            for (int y = 0; y < baseline.getHeight(); y++) {
                if (!pixelsMatch(baseline.getRGB(x, y), current.getRGB(x, y), tolerance)) {
                    differentPixels++;
                }
            }
        }
        
        double differencePercentage = (double) differentPixels / totalPixels * 100;
        boolean isMatch = differencePercentage <= (tolerance * 100);
        
        return new VisualComparisonResult(isMatch, differencePercentage, 
                String.format("Found %d different pixels (%.2f%%) with tolerance %.2f", 
                        differentPixels, differencePercentage, tolerance));
    }
    
    private boolean pixelsMatch(int rgb1, int rgb2, double tolerance) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;
        
        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;
        
        double distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));
        double maxDistance = Math.sqrt(3 * Math.pow(255, 2));
        
        return (distance / maxDistance) <= tolerance;
    }
    
    private BufferedImage createDifferenceImage(BufferedImage baseline, BufferedImage current) {
        BufferedImage difference = new BufferedImage(baseline.getWidth(), baseline.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < baseline.getWidth(); x++) {
            for (int y = 0; y < baseline.getHeight(); y++) {
                int baselineRGB = baseline.getRGB(x, y);
                int currentRGB = current.getRGB(x, y);
                
                if (baselineRGB != currentRGB) {
                    // Highlight differences in red
                    difference.setRGB(x, y, 0xFF0000);
                } else {
                    // Keep original pixel
                    difference.setRGB(x, y, baselineRGB);
                }
            }
        }
        
        return difference;
    }
    
    private void createScreenshotDirectory() {
        try {
            Path dir = Paths.get(screenshotDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created screenshot directory: " + screenshotDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create screenshot directory", e);
        }
    }
    
    /**
     * Result class for visual comparison.
     */
    public static class VisualComparisonResult {
        private final boolean isMatch;
        private final double differencePercentage;
        private final String message;
        
        public VisualComparisonResult(boolean isMatch, double differencePercentage, String message) {
            this.isMatch = isMatch;
            this.differencePercentage = differencePercentage;
            this.message = message;
        }
        
        public boolean isMatch() { return isMatch; }
        public double getDifferencePercentage() { return differencePercentage; }
        public String getMessage() { return message; }
    }
    
    /**
     * Result class for visual regression testing.
     */
    public static class VisualRegressionResult {
        private final boolean passed;
        private final String message;
        private final Path currentScreenshot;
        private final Path differenceImage;
        
        public VisualRegressionResult(boolean passed, String message, Path currentScreenshot, Path differenceImage) {
            this.passed = passed;
            this.message = message;
            this.currentScreenshot = currentScreenshot;
            this.differenceImage = differenceImage;
        }
        
        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
        public Path getCurrentScreenshot() { return currentScreenshot; }
        public Path getDifferenceImage() { return differenceImage; }
    }
}

