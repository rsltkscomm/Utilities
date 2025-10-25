package smartUIComparison;

/**
 * Enum for different image comparison methods
 */
public enum ComparisonMethod {
    PIXEL_BY_PIXEL,
    TEMPLATE_MATCHING,
    FEATURE_DETECTION,
    STRUCTURAL_SIMILARITY;
    
    /**
     * Convert string to ComparisonMethod enum
     */
    public static ComparisonMethod fromString(String method) {
        if (method == null) {
            return PIXEL_BY_PIXEL;
        }
        
        try {
            return ComparisonMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid comparison method: " + method + ", using PIXEL_BY_PIXEL");
            return PIXEL_BY_PIXEL;
        }
    }
}