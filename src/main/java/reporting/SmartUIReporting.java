package reporting;

import java.io.File;

/**
 * Enhanced reporting integration for SmartUI visual comparisons (simplified version)
 */
public class SmartUIReporting {
    
    /**
     * Attach visual comparison results to report
     */
    public static void attachVisualComparison(Object test,
                                            String baselinePath,
                                            String actualPath,
                                            String diffPath,
                                            double diffPercent,
                                            boolean passed) {

        if (test == null) {
            System.err.println("Test instance is null, cannot attach visual comparison.");
            return;
        }

        try {
            String statusMessage = passed ? "PASSED" : "FAILED";
            String logMessage = String.format("Visual comparison %s (diff: %.2f%%)", statusMessage, diffPercent);
            System.out.println(logMessage);

            // Attach images to report
            if (fileExists(baselinePath)) {
                System.out.println("Baseline Image: " + baselinePath);
            }
            if (fileExists(actualPath)) {
                System.out.println("Actual Image: " + actualPath);
            }
            if (fileExists(diffPath) && new File(diffPath).length() > 0) {
                System.out.println("Difference Image: " + diffPath);
            }

        } catch (Exception e) {
            System.err.println("Error attaching images to report: " + e.getMessage());
        }
    }

    /**
     * Create a detailed visual comparison report
     */
    public static void createDetailedReport(Object test, String testName,
                                            String baselinePath, String actualPath, String diffPath,
                                            double diffPercent, double tolerance, boolean passed,
                                            String screenshotStrategy, String comparisonMethod) {
        if (test == null) {
            System.err.println("Test instance is null, cannot create detailed report for: " + testName);
            return;
        }

        String statusMessage = passed ? "PASSED" : "FAILED";
        String logMessage = String.format("Visual Comparison for '%s' - Status: %s (Diff: %.2f%%, Tolerance: %.2f%%)",
                testName, statusMessage, diffPercent, tolerance);

        System.out.println(logMessage);
        System.out.println("SmartUI Details:");
        System.out.println("Screenshot Strategy: " + screenshotStrategy);
        System.out.println("Comparison Method: " + comparisonMethod);
        System.out.println("Tolerance: " + String.format("%.2f%%", tolerance));
        System.out.println("Actual Difference: " + String.format("%.2f%%", diffPercent));

        try {
            if (fileExists(baselinePath)) {
                System.out.println("Baseline Image: " + baselinePath);
            }
            if (fileExists(actualPath)) {
                System.out.println("Actual Image: " + actualPath);
            }
            if (fileExists(diffPath) && new File(diffPath).length() > 0) {
                System.out.println("Difference Image: " + diffPath);
            }
        } catch (Exception e) {
            System.err.println("Error attaching images to report for test: " + testName + " - " + e.getMessage());
        }
    }

    /**
     * Report baseline creation
     */
    public static void reportBaselineCreation(Object test, String baselinePath) {
        if (test == null) {
            System.err.println("Test instance is null, cannot report baseline creation.");
            return;
        }
        System.out.println("Baseline image created: " + baselinePath);
        try {
            if (fileExists(baselinePath)) {
                System.out.println("New Baseline: " + baselinePath);
            }
        } catch (Exception e) {
            System.err.println("Error attaching new baseline image to report: " + e.getMessage());
        }
    }

    /**
     * Report baseline update
     */
    public static void reportBaselineUpdate(Object test, String newBaselinePath, String oldBaselineHistoryPath) {
        if (test == null) {
            System.err.println("Test instance is null, cannot report baseline update.");
            return;
        }
        System.out.println("Baseline updated due to visual differences.");
        System.out.println("New Baseline: " + newBaselinePath);
        System.out.println("Old Baseline Archived: " + oldBaselineHistoryPath);
        try {
            if (fileExists(newBaselinePath)) {
                System.out.println("Updated Baseline: " + newBaselinePath);
            }
            if (fileExists(oldBaselineHistoryPath)) {
                System.out.println("Archived Baseline: " + oldBaselineHistoryPath);
            }
        } catch (Exception e) {
            System.err.println("Error attaching baseline update images to report: " + e.getMessage());
        }
    }

    /**
     * Report an error during visual comparison
     */
    public static void reportError(Object test, String testName, Exception error) {
        if (test == null) {
            System.err.println("Test instance is null, cannot report error for: " + testName);
            return;
        }
        System.err.println("Error during SmartUI visual comparison for " + testName + ": " + error.getMessage());
        System.err.println("Stack trace: " + error);
    }

    private static boolean fileExists(String path) {
        return path != null && new File(path).exists();
    }
}