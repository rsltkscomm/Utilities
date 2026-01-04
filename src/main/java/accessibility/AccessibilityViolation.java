package accessibility;

/**
 * Represents an accessibility violation
 */
public class AccessibilityViolation {
    
    private final WCAGLevel level;
    private final String criterion;
    private final String criterionName;
    private final String issue;
    private final String element;
    private final String recommendation;
    private final ViolationType type;
    
    public AccessibilityViolation(WCAGLevel level, String criterion, String criterionName,
                                  String issue, String element, String recommendation,
                                  ViolationType type) {
        this.level = level;
        this.criterion = criterion;
        this.criterionName = criterionName;
        this.issue = issue;
        this.element = element;
        this.recommendation = recommendation;
        this.type = type;
    }
    
    public WCAGLevel getLevel() { return level; }
    public String getCriterion() { return criterion; }
    public String getCriterionName() { return criterionName; }
    public String getIssue() { return issue; }
    public String getElement() { return element; }
    public String getRecommendation() { return recommendation; }
    public ViolationType getType() { return type; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (%s): %s - %s", 
            level, criterion, criterionName, issue, element);
    }
    
    /**
     * WCAG level enum
     */
    public enum WCAGLevel {
        A, AA, AAA
    }
    
    /**
     * Violation type enum
     */
    public enum ViolationType {
        MISSING_ALT_TEXT,
        MISSING_FORM_LABEL,
        HEADING_HIERARCHY,
        MISSING_LANGUAGE,
        MISSING_FOCUS_INDICATOR,
        MISSING_SKIP_LINK,
        INVALID_ARIA_LABEL,
        INVALID_ARIA_ROLE,
        INVALID_ARIA_STATE,
        INVALID_ARIA_RELATIONSHIP,
        INVALID_TAB_ORDER,
        KEYBOARD_INACCESSIBLE,
        FOCUS_TRAP_ISSUE,
        INSUFFICIENT_CONTRAST
    }
}

