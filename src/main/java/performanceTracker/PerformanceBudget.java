package performanceTracker;

/**
 * Performance Budget data model
 */
public class PerformanceBudget {
    private String name;
    private double threshold;
    private String description;
    
    // Constructors
    public PerformanceBudget() {}
    
    public PerformanceBudget(String name, double threshold, String description) {
        this.name = name;
        this.threshold = threshold;
        this.description = description;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

