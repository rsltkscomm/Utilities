package accessibility;

import com.microsoft.playwright.Page;
import reporting.TestLogManager;

import java.util.*;

/**
 * Main accessibility checker with WCAG compliance validation
 */
public class AccessibilityChecker {
    
    private final Page page;
    private final ARIAValidator ariaValidator;
    private final KeyboardNavigationTester keyboardTester;
    private final ColorContrastChecker contrastChecker;
    
    public AccessibilityChecker(Page page) {
        this.page = page;
        this.ariaValidator = new ARIAValidator(page);
        this.keyboardTester = new KeyboardNavigationTester(page);
        this.contrastChecker = new ColorContrastChecker(page);
    }
    
    /**
     * Run comprehensive accessibility check
     */
    public AccessibilityReport runFullCheck() {
        TestLogManager.info("Running comprehensive accessibility check");
        
        AccessibilityReport report = new AccessibilityReport();
        
        // WCAG compliance checks
        report.addViolations(checkWCAGCompliance());
        
        // ARIA validation
        report.addViolations(ariaValidator.validateAll());
        
        // Keyboard navigation
        report.addViolations(keyboardTester.testNavigation());
        
        // Color contrast
        report.addViolations(contrastChecker.checkAllElements());
        
        // Calculate score
        report.calculateScore();
        
        return report;
    }
    
    /**
     * Check WCAG compliance
     */
    public List<AccessibilityViolation> checkWCAGCompliance() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        // Check for missing alt text on images
        violations.addAll(checkImageAltText());
        
        // Check for missing form labels
        violations.addAll(checkFormLabels());
        
        // Check for heading hierarchy
        violations.addAll(checkHeadingHierarchy());
        
        // Check for missing language attribute
        violations.addAll(checkLanguageAttribute());
        
        // Check for focus indicators
        violations.addAll(checkFocusIndicators());
        
        // Check for skip links
        violations.addAll(checkSkipLinks());
        
        return violations;
    }
    
    /**
     * Check image alt text
     */
    private List<AccessibilityViolation> checkImageAltText() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const images = Array.from(document.querySelectorAll('img')); " +
                "return images.map(img => ({ " +
                "  src: img.src, " +
                "  alt: img.alt, " +
                "  hasAlt: img.hasAttribute('alt') " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> images = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> img : images) {
                boolean hasAlt = (Boolean) img.get("hasAlt");
                String alt = (String) img.get("alt");
                String src = (String) img.get("src");
                
                if (!hasAlt || alt == null || alt.trim().isEmpty()) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "1.1.1",
                        "Non-text Content",
                        "Image missing alt text",
                        "Image at: " + src,
                        "Add alt attribute to img element",
                        AccessibilityViolation.ViolationType.MISSING_ALT_TEXT
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error checking image alt text: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Check form labels
     */
    private List<AccessibilityViolation> checkFormLabels() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const inputs = Array.from(document.querySelectorAll('input, select, textarea')); " +
                "return inputs.map(input => ({ " +
                "  id: input.id, " +
                "  type: input.type, " +
                "  name: input.name, " +
                "  hasLabel: !!document.querySelector('label[for=\"' + input.id + '\"]'), " +
                "  hasAriaLabel: input.hasAttribute('aria-label'), " +
                "  hasAriaLabelledBy: input.hasAttribute('aria-labelledby') " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> inputs = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> input : inputs) {
                String type = (String) input.get("type");
                boolean hasLabel = (Boolean) input.get("hasLabel");
                boolean hasAriaLabel = (Boolean) input.get("hasAriaLabel");
                boolean hasAriaLabelledBy = (Boolean) input.get("hasAriaLabelledBy");
                
                // Skip hidden inputs
                if ("hidden".equals(type)) {
                    continue;
                }
                
                if (!hasLabel && !hasAriaLabel && !hasAriaLabelledBy) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "1.3.1",
                        "Info and Relationships",
                        "Form input missing label",
                        "Input type: " + type + ", name: " + input.get("name"),
                        "Add label element or aria-label/aria-labelledby attribute",
                        AccessibilityViolation.ViolationType.MISSING_FORM_LABEL
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error checking form labels: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Check heading hierarchy
     */
    private List<AccessibilityViolation> checkHeadingHierarchy() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const headings = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6')); " +
                "return headings.map(h => ({ " +
                "  level: parseInt(h.tagName.substring(1)), " +
                "  text: h.textContent.trim().substring(0, 50) " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> headings = (List<Map<String, Object>>) result;
            
            int previousLevel = 0;
            for (Map<String, Object> heading : headings) {
                int level = ((Number) heading.get("level")).intValue();
                
                if (previousLevel > 0 && level > previousLevel + 1) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "1.3.1",
                        "Info and Relationships",
                        "Heading hierarchy skipped",
                        "Heading level " + level + " follows level " + previousLevel,
                        "Use sequential heading levels (h1, h2, h3...)",
                        AccessibilityViolation.ViolationType.HEADING_HIERARCHY
                    ));
                }
                
                previousLevel = level;
            }
        } catch (Exception e) {
            TestLogManager.warning("Error checking heading hierarchy: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Check language attribute
     */
    private List<AccessibilityViolation> checkLanguageAttribute() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => document.documentElement.lang || ''");
            String lang = (String) result;
            
            if (lang == null || lang.trim().isEmpty()) {
                violations.add(new AccessibilityViolation(
                    AccessibilityViolation.WCAGLevel.A,
                    "3.1.1",
                    "Language of Page",
                    "Missing language attribute",
                    "html element missing lang attribute",
                    "Add lang attribute to html element (e.g., lang='en')",
                    AccessibilityViolation.ViolationType.MISSING_LANGUAGE
                ));
            }
        } catch (Exception e) {
            TestLogManager.warning("Error checking language attribute: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Check focus indicators
     */
    private List<AccessibilityViolation> checkFocusIndicators() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const style = window.getComputedStyle(document.documentElement); " +
                "const testElement = document.createElement('a'); " +
                "testElement.href = '#'; " +
                "testElement.textContent = 'Test'; " +
                "document.body.appendChild(testElement); " +
                "testElement.focus(); " +
                "const focusedStyle = window.getComputedStyle(testElement); " +
                "const outline = focusedStyle.outline; " +
                "const outlineWidth = focusedStyle.outlineWidth; " +
                "document.body.removeChild(testElement); " +
                "return { " +
                "  hasOutline: outline !== 'none' && outlineWidth !== '0px', " +
                "  outline: outline " +
                "}; " +
                "}");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> focusInfo = (Map<String, Object>) result;
            boolean hasOutline = (Boolean) focusInfo.get("hasOutline");
            
            if (!hasOutline) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.AA,
                    "2.4.7",
                    "Focus Visible",
                    "Missing focus indicator",
                    "Interactive elements lack visible focus indicator",
                    "Add CSS outline or box-shadow for focus states",
                    AccessibilityViolation.ViolationType.MISSING_FOCUS_INDICATOR
                ));
            }
        } catch (Exception e) {
            TestLogManager.warning("Error checking focus indicators: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Check skip links
     */
    private List<AccessibilityViolation> checkSkipLinks() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const skipLinks = Array.from(document.querySelectorAll('a[href^=\"#\"]')); " +
                "return skipLinks.filter(link => { " +
                "  const text = link.textContent.trim().toLowerCase(); " +
                "  return text.includes('skip') || text.includes('main') || text.includes('content'); " +
                "}).length; " +
                "}");
            
            int skipLinkCount = ((Number) result).intValue();
            
            if (skipLinkCount == 0) {
                violations.add(new AccessibilityViolation(
                    AccessibilityViolation.WCAGLevel.A,
                    "2.4.1",
                    "Bypass Blocks",
                    "Missing skip link",
                    "No skip to main content link found",
                    "Add skip link to allow users to bypass repetitive content",
                    AccessibilityViolation.ViolationType.MISSING_SKIP_LINK
                ));
            }
        } catch (Exception e) {
            TestLogManager.warning("Error checking skip links: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Get ARIA validator
     */
    public ARIAValidator getAriaValidator() {
        return ariaValidator;
    }
    
    /**
     * Get keyboard navigation tester
     */
    public KeyboardNavigationTester getKeyboardTester() {
        return keyboardTester;
    }
    
    /**
     * Get color contrast checker
     */
    public ColorContrastChecker getContrastChecker() {
        return contrastChecker;
    }
}

