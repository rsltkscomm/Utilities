package accessibility;

import com.microsoft.playwright.Page;
import reporting.TestLogManager;

import java.util.*;

/**
 * Checks color contrast ratios for WCAG compliance
 */
public class ColorContrastChecker {
    
    private final Page page;
    private static final double WCAG_AA_NORMAL = 4.5;  // Level AA for normal text
    private static final double WCAG_AA_LARGE = 3.0;    // Level AA for large text
    private static final double WCAG_AAA_NORMAL = 7.0;  // Level AAA for normal text
    private static final double WCAG_AAA_LARGE = 4.5;    // Level AAA for large text
    
    public ColorContrastChecker(Page page) {
        this.page = page;
    }
    
    /**
     * Check all elements for color contrast
     */
    public List<AccessibilityViolation> checkAllElements() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        violations.addAll(checkTextContrast());
        violations.addAll(checkInteractiveElementContrast());
        
        return violations;
    }
    
    /**
     * Check text color contrast
     */
    public List<AccessibilityViolation> checkTextContrast() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const textElements = Array.from(document.querySelectorAll(" +
                "  'p, span, div, h1, h2, h3, h4, h5, h6, a, li, td, th, label' " +
                ")); " +
                "return textElements.filter(el => { " +
                "  const text = el.textContent.trim(); " +
                "  return text.length > 0 && el.offsetParent !== null; " +
                "}).map(el => { " +
                "  const style = window.getComputedStyle(el); " +
                "  const fontSize = parseFloat(style.fontSize); " +
                "  const fontWeight = style.fontWeight; " +
                "  const isLarge = fontSize >= 18 || (fontSize >= 14 && fontWeight >= 700); " +
                "  return { " +
                "    tag: el.tagName.toLowerCase(), " +
                "    text: text.substring(0, 50), " +
                "    fontSize: fontSize, " +
                "    isLarge: isLarge, " +
                "    color: style.color, " +
                "    bgColor: style.backgroundColor " +
                "  }; " +
                "}); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> elements = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> el : elements) {
                String color = (String) el.get("color");
                String bgColor = (String) el.get("bgColor");
                boolean isLarge = (Boolean) el.get("isLarge");
                
                // Calculate contrast ratio (simplified - in production use proper color parsing)
                double contrastRatio = calculateContrastRatio(color, bgColor);
                
                double requiredRatio = isLarge ? WCAG_AA_LARGE : WCAG_AA_NORMAL;
                
                if (contrastRatio < requiredRatio) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.AA,
                        "1.4.3",
                        "Contrast (Minimum)",
                        "Insufficient color contrast",
                        "Element: " + el.get("tag") + ", contrast: " + 
                            String.format("%.2f", contrastRatio) + ":1",
                        "Increase contrast ratio to at least " + requiredRatio + ":1",
                        AccessibilityViolation.ViolationType.INSUFFICIENT_CONTRAST
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error checking text contrast: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Check interactive element contrast
     */
    public List<AccessibilityViolation> checkInteractiveElementContrast() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const interactive = Array.from(document.querySelectorAll(" +
                "  'button, a[href], input, select, textarea' " +
                ")); " +
                "return interactive.filter(el => el.offsetParent !== null).map(el => { " +
                "  const style = window.getComputedStyle(el); " +
                "  return { " +
                "    tag: el.tagName.toLowerCase(), " +
                "    color: style.color, " +
                "    bgColor: style.backgroundColor, " +
                "    borderColor: style.borderColor " +
                "  }; " +
                "}); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> elements = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> el : elements) {
                String color = (String) el.get("color");
                String bgColor = (String) el.get("bgColor");
                
                double contrastRatio = calculateContrastRatio(color, bgColor);
                
                if (contrastRatio < WCAG_AA_NORMAL) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.AA,
                        "1.4.11",
                        "Non-text Contrast",
                        "Insufficient contrast for interactive element",
                        "Element: " + el.get("tag") + ", contrast: " + 
                            String.format("%.2f", contrastRatio) + ":1",
                        "Increase contrast ratio to at least " + WCAG_AA_NORMAL + ":1",
                        AccessibilityViolation.ViolationType.INSUFFICIENT_CONTRAST
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error checking interactive element contrast: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Calculate contrast ratio between two colors
     * Simplified version - in production use proper color parsing library
     */
    private double calculateContrastRatio(String color1, String color2) {
        // Simplified calculation - assumes RGB colors
        // In production, parse color strings properly and calculate luminance
        try {
            // Extract RGB values from color strings (simplified)
            int[] rgb1 = parseColor(color1);
            int[] rgb2 = parseColor(color2);
            
            double lum1 = calculateLuminance(rgb1[0], rgb1[1], rgb1[2]);
            double lum2 = calculateLuminance(rgb2[0], rgb2[1], rgb2[2]);
            
            double lighter = Math.max(lum1, lum2);
            double darker = Math.min(lum1, lum2);
            
            return (lighter + 0.05) / (darker + 0.05);
        } catch (Exception e) {
            // Return default if parsing fails
            return 1.0;
        }
    }
    
    /**
     * Parse color string to RGB (simplified)
     */
    private int[] parseColor(String color) {
        // Simplified - in production use proper CSS color parsing
        if (color == null || color.trim().isEmpty()) {
            return new int[]{255, 255, 255}; // Default to white
        }
        
        // Try to extract RGB values from rgb() or hex format
        if (color.startsWith("rgb")) {
            // Extract from rgb(r, g, b) format
            String values = color.replaceAll("[^0-9,]", "");
            String[] parts = values.split(",");
            if (parts.length >= 3) {
                return new int[]{
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
                };
            }
        } else if (color.startsWith("#")) {
            // Hex format
            String hex = color.substring(1);
            if (hex.length() == 6) {
                return new int[]{
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16)
                };
            }
        }
        
        // Default to white
        return new int[]{255, 255, 255};
    }
    
    /**
     * Calculate relative luminance
     */
    private double calculateLuminance(int r, int g, int b) {
        double rsRGB = r / 255.0;
        double gsRGB = g / 255.0;
        double bsRGB = b / 255.0;
        
        double rLinear = rsRGB <= 0.03928 ? rsRGB / 12.92 : Math.pow((rsRGB + 0.055) / 1.055, 2.4);
        double gLinear = gsRGB <= 0.03928 ? gsRGB / 12.92 : Math.pow((gsRGB + 0.055) / 1.055, 2.4);
        double bLinear = bsRGB <= 0.03928 ? bsRGB / 12.92 : Math.pow((bsRGB + 0.055) / 1.055, 2.4);
        
        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear;
    }
}

