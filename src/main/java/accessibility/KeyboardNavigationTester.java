package accessibility;

import com.microsoft.playwright.Page;
import reporting.TestLogManager;

import java.util.*;

/**
 * Tests keyboard navigation accessibility
 */
public class KeyboardNavigationTester {
    
    private final Page page;
    
    public KeyboardNavigationTester(Page page) {
        this.page = page;
    }
    
    /**
     * Test keyboard navigation
     */
    public List<AccessibilityViolation> testNavigation() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        violations.addAll(testTabOrder());
        violations.addAll(testKeyboardAccessibility());
        violations.addAll(testFocusTrapping());
        
        return violations;
    }
    
    /**
     * Test tab order
     */
    public List<AccessibilityViolation> testTabOrder() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const focusable = Array.from(document.querySelectorAll(" +
                "  'a[href], button, input, select, textarea, [tabindex]:not([tabindex=\"-1\"])' " +
                ")); " +
                "return focusable.map((el, index) => ({ " +
                "  tag: el.tagName.toLowerCase(), " +
                "  tabIndex: el.tabIndex, " +
                "  index: index " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> focusable = (List<Map<String, Object>>) result;
            
            // Check for non-sequential tabindex
            for (Map<String, Object> el : focusable) {
                int tabIndex = ((Number) el.get("tabIndex")).intValue();
                if (tabIndex > 0) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "2.4.3",
                        "Focus Order",
                        "Non-sequential tabindex",
                        "Element has tabindex=" + tabIndex,
                        "Avoid positive tabindex values, use natural tab order",
                        AccessibilityViolation.ViolationType.INVALID_TAB_ORDER
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error testing tab order: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Test keyboard accessibility
     */
    public List<AccessibilityViolation> testKeyboardAccessibility() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const interactive = Array.from(document.querySelectorAll(" +
                "  'button, a[href], input, select, textarea, [role=\"button\"], [role=\"link\"]' " +
                ")); " +
                "return interactive.map(el => ({ " +
                "  tag: el.tagName.toLowerCase(), " +
                "  role: el.getAttribute('role'), " +
                "  hasOnClick: el.onclick !== null, " +
                "  hasKeyboardHandler: el.onkeydown !== null || el.onkeypress !== null, " +
                "  isDisabled: el.disabled || el.getAttribute('aria-disabled') === 'true' " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> interactive = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> el : interactive) {
                boolean hasOnClick = (Boolean) el.get("hasOnClick");
                boolean hasKeyboardHandler = (Boolean) el.get("hasKeyboardHandler");
                boolean isDisabled = (Boolean) el.get("isDisabled");
                
                // Check if click-only elements have keyboard handlers
                if (hasOnClick && !hasKeyboardHandler && !isDisabled) {
                    String tag = (String) el.get("tag");
                    if (!"button".equals(tag) && !"a".equals(tag)) {
                        violations.add(new AccessibilityViolation(
                            AccessibilityViolation.WCAGLevel.A,
                            "2.1.1",
                            "Keyboard",
                            "Interactive element not keyboard accessible",
                            "Element: " + tag + " has onclick but no keyboard handler",
                            "Add keyboard event handler (onkeydown/onkeypress) or use semantic HTML",
                            AccessibilityViolation.ViolationType.KEYBOARD_INACCESSIBLE
                        ));
                    }
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error testing keyboard accessibility: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Test focus trapping in modals
     */
    public List<AccessibilityViolation> testFocusTrapping() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const modals = Array.from(document.querySelectorAll('[role=\"dialog\"], [role=\"alertdialog\"]')); " +
                "return modals.map(modal => ({ " +
                "  isVisible: modal.offsetParent !== null, " +
                "  hasFocusable: Array.from(modal.querySelectorAll(" +
                "    'a[href], button, input, select, textarea, [tabindex]:not([tabindex=\"-1\"])' " +
                "  )).length > 0 " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> modals = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> modal : modals) {
                boolean isVisible = (Boolean) modal.get("isVisible");
                boolean hasFocusable = (Boolean) modal.get("hasFocusable");
                
                if (isVisible && !hasFocusable) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.AA,
                        "2.1.2",
                        "No Keyboard Trap",
                        "Modal dialog without focusable elements",
                        "Visible modal dialog has no focusable elements",
                        "Ensure modal has at least one focusable element and implement focus trapping",
                        AccessibilityViolation.ViolationType.FOCUS_TRAP_ISSUE
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error testing focus trapping: " + e.getMessage());
        }
        
        return violations;
    }
}

