package ai.ml;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import reporting.TestLogManager;

import java.util.*;

/**
 * ML-based element detection that learns from successful locators
 */
public class MLElementDetector {
    
    private final Map<String, LocatorSuccess> locatorHistory;
    private final Map<String, List<String>> successfulLocators;
    
    public MLElementDetector() {
        this.locatorHistory = new HashMap<>();
        this.successfulLocators = new HashMap<>();
    }
    
    /**
     * Learn from successful locator
     */
    public void recordSuccess(String elementDescription, String locator, String locatorType) {
        String key = elementDescription.toLowerCase();
        
        LocatorSuccess success = locatorHistory.computeIfAbsent(key, 
            k -> new LocatorSuccess(elementDescription));
        success.recordSuccess(locator, locatorType);
        
        successfulLocators.computeIfAbsent(key, k -> new ArrayList<>()).add(locator);
        
        TestLogManager.info("Learned successful locator for: " + elementDescription);
    }
    
    /**
     * Learn from failed locator
     */
    public void recordFailure(String elementDescription, String locator) {
        String key = elementDescription.toLowerCase();
        
        LocatorSuccess success = locatorHistory.computeIfAbsent(key,
            k -> new LocatorSuccess(elementDescription));
        success.recordFailure(locator);
    }
    
    /**
     * Suggest locator based on learning
     */
    public String suggestLocator(String elementDescription) {
        String key = elementDescription.toLowerCase();
        
        LocatorSuccess success = locatorHistory.get(key);
        if (success == null) {
            return null;
        }
        
        // Get most successful locator
        return success.getMostSuccessfulLocator();
    }
    
    /**
     * Get locator suggestions with confidence
     */
    public List<LocatorSuggestion> getLocatorSuggestions(String elementDescription) {
        List<LocatorSuggestion> suggestions = new ArrayList<>();
        
        String key = elementDescription.toLowerCase();
        LocatorSuccess success = locatorHistory.get(key);
        
        if (success != null) {
            Map<String, LocatorStats> stats = success.getLocatorStats();
            
            for (Map.Entry<String, LocatorStats> entry : stats.entrySet()) {
                double successRate = entry.getValue().getSuccessRate();
                if (successRate > 0.5) {
                    suggestions.add(new LocatorSuggestion(
                        entry.getKey(),
                        entry.getValue().getLocatorType(),
                        successRate,
                        entry.getValue().getSuccessCount()
                    ));
                }
            }
            
            // Sort by success rate
            suggestions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        }
        
        return suggestions;
    }
    
    /**
     * Adapt to UI changes
     */
    public String adaptLocator(String elementDescription, String originalLocator, Page page) {
        // Get suggestions
        List<LocatorSuggestion> suggestions = getLocatorSuggestions(elementDescription);
        
        // Try suggested locators
        for (LocatorSuggestion suggestion : suggestions) {
            try {
                Locator locator = page.locator(suggestion.getLocator());
                if (locator.isVisible()) {
                    // Success - record it
                    recordSuccess(elementDescription, suggestion.getLocator(), 
                        suggestion.getLocatorType());
                    return suggestion.getLocator();
                }
            } catch (Exception e) {
                // Try next suggestion
            }
        }
        
        return null;
    }
    
    /**
     * Locator success tracker
     */
    private static class LocatorSuccess {
        private final String elementDescription;
        private final Map<String, LocatorStats> locatorStats;
        
        public LocatorSuccess(String elementDescription) {
            this.elementDescription = elementDescription;
            this.locatorStats = new HashMap<>();
        }
        
        public void recordSuccess(String locator, String locatorType) {
            LocatorStats stats = locatorStats.computeIfAbsent(locator,
                k -> new LocatorStats(locator, locatorType));
            stats.recordSuccess();
        }
        
        public void recordFailure(String locator) {
            LocatorStats stats = locatorStats.computeIfAbsent(locator,
                k -> new LocatorStats(locator, "unknown"));
            stats.recordFailure();
        }
        
        public String getMostSuccessfulLocator() {
            return locatorStats.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().getSuccessRate()))
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        public Map<String, LocatorStats> getLocatorStats() {
            return new HashMap<>(locatorStats);
        }
    }
    
    /**
     * Locator statistics
     */
    private static class LocatorStats {
        private final String locator;
        private final String locatorType;
        private int successCount;
        private int failureCount;
        
        public LocatorStats(String locator, String locatorType) {
            this.locator = locator;
            this.locatorType = locatorType;
            this.successCount = 0;
            this.failureCount = 0;
        }
        
        public void recordSuccess() {
            successCount++;
        }
        
        public void recordFailure() {
            failureCount++;
        }
        
        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total > 0 ? (double) successCount / total : 0.0;
        }
        
        public String getLocator() { return locator; }
        public String getLocatorType() { return locatorType; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
    }
    
    /**
     * Locator suggestion
     */
    public static class LocatorSuggestion {
        private final String locator;
        private final String locatorType;
        private final double confidence;
        private final int successCount;
        
        public LocatorSuggestion(String locator, String locatorType, 
                               double confidence, int successCount) {
            this.locator = locator;
            this.locatorType = locatorType;
            this.confidence = confidence;
            this.successCount = successCount;
        }
        
        public String getLocator() { return locator; }
        public String getLocatorType() { return locatorType; }
        public double getConfidence() { return confidence; }
        public int getSuccessCount() { return successCount; }
    }
}

