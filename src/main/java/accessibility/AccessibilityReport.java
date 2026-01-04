package accessibility;

import java.util.*;

/**
 * Accessibility test report
 */
public class AccessibilityReport {
    
    private final List<AccessibilityViolation> violations;
    private final Map<AccessibilityViolation.WCAGLevel, Integer> violationsByLevel;
    private final Map<AccessibilityViolation.ViolationType, Integer> violationsByType;
    private double score;
    private int totalChecks;
    
    public AccessibilityReport() {
        this.violations = new ArrayList<>();
        this.violationsByLevel = new HashMap<>();
        this.violationsByType = new HashMap<>();
        this.score = 100.0;
        this.totalChecks = 0;
    }
    
    /**
     * Add violations
     */
    public void addViolations(List<AccessibilityViolation> newViolations) {
        violations.addAll(newViolations);
        totalChecks += newViolations.size();
        
        // Count by level
        for (AccessibilityViolation violation : newViolations) {
            violationsByLevel.merge(violation.getLevel(), 1, Integer::sum);
            violationsByType.merge(violation.getType(), 1, Integer::sum);
        }
    }
    
    /**
     * Calculate accessibility score
     */
    public void calculateScore() {
        if (totalChecks == 0) {
            score = 100.0;
            return;
        }
        
        // Calculate score based on violations
        // Each violation reduces score, with higher weight for higher WCAG levels
        double penalty = 0.0;
        
        for (AccessibilityViolation violation : violations) {
            double violationPenalty = switch (violation.getLevel()) {
                case A -> 2.0;
                case AA -> 3.0;
                case AAA -> 4.0;
            };
            penalty += violationPenalty;
        }
        
        // Normalize penalty (max 100 points)
        double maxPenalty = totalChecks * 4.0; // Worst case: all AAA violations
        double normalizedPenalty = (penalty / maxPenalty) * 100.0;
        
        score = Math.max(0.0, 100.0 - normalizedPenalty);
    }
    
    /**
     * Get violations by level
     */
    public Map<AccessibilityViolation.WCAGLevel, Integer> getViolationsByLevel() {
        return new HashMap<>(violationsByLevel);
    }
    
    /**
     * Get violations by type
     */
    public Map<AccessibilityViolation.ViolationType, Integer> getViolationsByType() {
        return new HashMap<>(violationsByType);
    }
    
    /**
     * Get all violations
     */
    public List<AccessibilityViolation> getViolations() {
        return new ArrayList<>(violations);
    }
    
    /**
     * Get violations by level
     */
    public List<AccessibilityViolation> getViolations(AccessibilityViolation.WCAGLevel level) {
        List<AccessibilityViolation> filtered = new ArrayList<>();
        for (AccessibilityViolation violation : violations) {
            if (violation.getLevel() == level) {
                filtered.add(violation);
            }
        }
        return filtered;
    }
    
    /**
     * Get score
     */
    public double getScore() {
        return score;
    }
    
    /**
     * Get total checks
     */
    public int getTotalChecks() {
        return totalChecks;
    }
    
    /**
     * Get violation count
     */
    public int getViolationCount() {
        return violations.size();
    }
    
    /**
     * Check if passed (score >= 80)
     */
    public boolean isPassed() {
        return score >= 80.0;
    }
    
    /**
     * Get summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Accessibility Report Summary\n");
        sb.append("============================\n");
        sb.append("Score: ").append(String.format("%.1f", score)).append("/100\n");
        sb.append("Total Checks: ").append(totalChecks).append("\n");
        sb.append("Violations: ").append(violations.size()).append("\n");
        sb.append("\nViolations by Level:\n");
        for (Map.Entry<AccessibilityViolation.WCAGLevel, Integer> entry : violationsByLevel.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}

