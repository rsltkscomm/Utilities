package performanceTracker;

/**
 * Report Naming Utility
 * Provides meaningful names for test reports based on test characteristics
 */
public class ReportNamingUtility {
    
    /**
     * Generate meaningful report name based on test name and test case key
     */
    public static String generateMeaningfulReportName(String testName, String testCaseKey) {
        return generateMeaningfulReportName(testName, testCaseKey, null);
    }
    
    /**
     * Generate meaningful report name based on test name, test case key, and URL
     * @param testName The test method name
     * @param testCaseKey The test case key
     * @param url The application URL (e.g., https://run19.resulticks.com/)
     * @return Meaningful report name with environment identifier
     */
    public static String generateMeaningfulReportName(String testName, String testCaseKey, String url) {
        if (testName == null && testCaseKey == null) {
            return "Unknown_Test";
        }
        
        // Extract environment identifier from URL (run19, run, qa, etc.)
        String environmentId = extractEnvironmentFromUrl(url);
        
        // Extract meaningful parts from test name
        String meaningfulName = extractMeaningfulName(testName);
        
        // Add environment identifier if available
        if (environmentId != null && !environmentId.isEmpty()) {
            meaningfulName = meaningfulName + "_" + environmentId;
        }
        
        // Add test case key if available
        if (testCaseKey != null && !testCaseKey.isEmpty()) {
            meaningfulName = meaningfulName + "_" + testCaseKey;
        }
        
        return meaningfulName;
    }
    
    /**
     * Extract environment identifier from URL
     * Examples:
     * - https://run19.resulticks.com/ -> run19
     * - https://run.resulticks.com/ -> run
     * - https://qa.resulticks.com/ -> qa
     * - https://run19.resul.io/ -> run19
     */
    public static String extractEnvironmentFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // Remove protocol (http:// or https://)
            String urlWithoutProtocol = url.replaceFirst("^https?://", "");
            
            // Extract subdomain (part before first dot)
            int firstDotIndex = urlWithoutProtocol.indexOf('.');
            if (firstDotIndex > 0) {
                String subdomain = urlWithoutProtocol.substring(0, firstDotIndex);
                
                // Clean up common patterns
                subdomain = subdomain.replaceAll("/$", ""); // Remove trailing slash
                subdomain = subdomain.trim();
                
                // Return subdomain if it's not empty
                if (!subdomain.isEmpty()) {
                    return subdomain;
                }
            }
        } catch (Exception e) {
            // If parsing fails, return null
        }
        
        return null;
    }
    
    /**
     * Extract meaningful name from test name
     */
    private static String extractMeaningfulName(String testName) {
        if (testName == null || testName.isEmpty()) {
            return "Test";
        }
        
        // Remove common prefixes and suffixes
        String name = testName
            .replaceAll("^test", "")
            .replaceAll("Test$", "")
            .replaceAll("Demo$", "")
            .replaceAll("Capabilities$", "")
            .replaceAll("Performance$", "")
            .replaceAll("Network$", "");
        
        // Convert camelCase to readable format
        name = camelCaseToReadable(name);
        
        // Handle specific test patterns
        if (name.contains("ResulIo")) {
            return "ResulIo_Performance_Test";
        }
        if (name.contains("Enhanced")) {
            return "Enhanced_Performance_Analysis";
        }
        if (name.contains("Seamless")) {
            return "Seamless_Integration_Test";
        }
        if (name.contains("Functional")) {
            return "Functional_Performance_Test";
        }
        if (name.contains("BrowserLogs")) {
            return "Browser_Logs_Analysis";
        }
        if (name.contains("RealCapture")) {
            return "Real_Network_Capture";
        }
        if (name.contains("Comprehensive")) {
            return "Comprehensive_Web_Performance";
        }
        if (name.contains("CompleteTier2")) {
            return "Complete_Tier2_Demo";
        }
        
        // Default meaningful name
        if (name.isEmpty()) {
            return "Performance_Test";
        }
        
        return name;
    }
    
    /**
     * Convert camelCase to readable format
     */
    private static String camelCaseToReadable(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        // Insert spaces before capital letters
        String readable = camelCase.replaceAll("([a-z])([A-Z])", "$1_$2");
        
        // Replace underscores with spaces and capitalize
        String[] words = readable.split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append("_");
            }
            result.append(capitalizeFirstLetter(words[i]));
        }
        
        return result.toString();
    }
    
    /**
     * Capitalize first letter of a word
     */
    private static String capitalizeFirstLetter(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }
    
    /**
     * Get report type description
     */
    public static String getReportTypeDescription(String testName) {
        if (testName == null) {
            return "Performance Test";
        }
        
        if (testName.contains("Enhanced")) {
            return "Enhanced Performance Analysis";
        }
        if (testName.contains("Network")) {
            return "Network Performance Analysis";
        }
        if (testName.contains("Functional")) {
            return "Functional Performance Test";
        }
        if (testName.contains("BrowserLogs")) {
            return "Browser Console Analysis";
        }
        if (testName.contains("RealCapture")) {
            return "Real Network Capture";
        }
        if (testName.contains("Comprehensive")) {
            return "Comprehensive Web Performance";
        }
        if (testName.contains("Seamless")) {
            return "Seamless Integration Test";
        }
        
        return "Performance Test";
    }
    
    /**
     * Get test category
     */
    public static String getTestCategory(String testName) {
        if (testName == null) {
            return "General";
        }
        
        if (testName.contains("ResulIo")) {
            return "Application Performance";
        }
        if (testName.contains("Enhanced")) {
            return "Advanced Analytics";
        }
        if (testName.contains("Network")) {
            return "Network Analysis";
        }
        if (testName.contains("Functional")) {
            return "Functional Testing";
        }
        if (testName.contains("BrowserLogs")) {
            return "Browser Analysis";
        }
        if (testName.contains("RealCapture")) {
            return "Network Capture";
        }
        if (testName.contains("Comprehensive")) {
            return "Comprehensive Analysis";
        }
        if (testName.contains("Seamless")) {
            return "Integration Testing";
        }
        
        return "Performance Testing";
    }
}


