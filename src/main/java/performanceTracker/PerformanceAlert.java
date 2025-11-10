package performanceTracker;

/**
 * Performance Alert data model
 */
public class PerformanceAlert {
    private String id;
    private String type;
    private String severity;
    private String message;
    private long timestamp;
    private String budgetName;
    private double currentValue;
    private double threshold;
    
    // Constructors
    public PerformanceAlert() {}
    
    public PerformanceAlert(String id, String type, String severity, String message, long timestamp) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getBudgetName() { return budgetName; }
    public void setBudgetName(String budgetName) { this.budgetName = budgetName; }
    
    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
}

