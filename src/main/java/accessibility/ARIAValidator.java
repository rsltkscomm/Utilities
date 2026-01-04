package accessibility;

import com.microsoft.playwright.Page;
import reporting.TestLogManager;

import java.util.*;

/**
 * Validates ARIA attributes for accessibility
 */
public class ARIAValidator {
    
    private final Page page;
    
    public ARIAValidator(Page page) {
        this.page = page;
    }
    
    /**
     * Validate all ARIA attributes
     */
    public List<AccessibilityViolation> validateAll() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        violations.addAll(validateAriaLabels());
        violations.addAll(validateAriaRoles());
        violations.addAll(validateAriaStates());
        violations.addAll(validateAriaRelationships());
        
        return violations;
    }
    
    /**
     * Validate aria-label attributes
     */
    public List<AccessibilityViolation> validateAriaLabels() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const elements = Array.from(document.querySelectorAll('[aria-label]')); " +
                "return elements.map(el => ({ " +
                "  tag: el.tagName.toLowerCase(), " +
                "  ariaLabel: el.getAttribute('aria-label'), " +
                "  hasText: el.textContent.trim().length > 0 " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> elements = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> el : elements) {
                String ariaLabel = (String) el.get("ariaLabel");
                
                if (ariaLabel == null || ariaLabel.trim().isEmpty()) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "4.1.2",
                        "Name, Role, Value",
                        "Empty aria-label",
                        "Element: " + el.get("tag"),
                        "Provide meaningful aria-label text",
                        AccessibilityViolation.ViolationType.INVALID_ARIA_LABEL
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error validating aria-labels: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Validate ARIA roles
     */
    public List<AccessibilityViolation> validateAriaRoles() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const elements = Array.from(document.querySelectorAll('[role]')); " +
                "const validRoles = ['button', 'link', 'heading', 'img', 'list', 'listitem', " +
                "  'navigation', 'main', 'complementary', 'contentinfo', 'search', 'form', " +
                "  'textbox', 'checkbox', 'radio', 'combobox', 'slider', 'tab', 'tabpanel', " +
                "  'dialog', 'alert', 'alertdialog', 'status', 'timer', 'progressbar', 'menu', " +
                "  'menubar', 'menuitem', 'toolbar', 'tooltip', 'tree', 'treeitem', 'grid', " +
                "  'gridcell', 'row', 'rowgroup', 'columnheader', 'rowheader', 'table', " +
                "  'presentation', 'none', 'banner', 'article', 'region', 'application']; " +
                "return elements.map(el => ({ " +
                "  tag: el.tagName.toLowerCase(), " +
                "  role: el.getAttribute('role') " +
                "})).filter(el => !validRoles.includes(el.role)); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> invalidRoles = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> el : invalidRoles) {
                violations.add(new AccessibilityViolation(
                    AccessibilityViolation.WCAGLevel.A,
                    "4.1.2",
                    "Name, Role, Value",
                    "Invalid ARIA role",
                    "Element: " + el.get("tag") + ", role: " + el.get("role"),
                    "Use valid ARIA role from WAI-ARIA specification",
                    AccessibilityViolation.ViolationType.INVALID_ARIA_ROLE
                ));
            }
        } catch (Exception e) {
            TestLogManager.warning("Error validating ARIA roles: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Validate ARIA states
     */
    public List<AccessibilityViolation> validateAriaStates() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const elements = Array.from(document.querySelectorAll('[aria-expanded], [aria-selected], [aria-checked], [aria-disabled], [aria-hidden]')); " +
                "return elements.map(el => ({ " +
                "  tag: el.tagName.toLowerCase(), " +
                "  ariaExpanded: el.getAttribute('aria-expanded'), " +
                "  ariaSelected: el.getAttribute('aria-selected'), " +
                "  ariaChecked: el.getAttribute('aria-checked'), " +
                "  ariaDisabled: el.getAttribute('aria-disabled'), " +
                "  ariaHidden: el.getAttribute('aria-hidden') " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> elements = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> el : elements) {
                // Check for invalid boolean values
                String ariaExpanded = (String) el.get("ariaExpanded");
                String ariaSelected = (String) el.get("ariaSelected");
                
                if (ariaExpanded != null && !isValidBoolean(ariaExpanded)) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "4.1.2",
                        "Name, Role, Value",
                        "Invalid aria-expanded value",
                        "Element: " + el.get("tag") + ", value: " + ariaExpanded,
                        "Use 'true' or 'false' for aria-expanded",
                        AccessibilityViolation.ViolationType.INVALID_ARIA_STATE
                    ));
                }
                
                if (ariaSelected != null && !isValidBoolean(ariaSelected)) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "4.1.2",
                        "Name, Role, Value",
                        "Invalid aria-selected value",
                        "Element: " + el.get("tag") + ", value: " + ariaSelected,
                        "Use 'true' or 'false' for aria-selected",
                        AccessibilityViolation.ViolationType.INVALID_ARIA_STATE
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error validating ARIA states: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Validate ARIA relationships
     */
    public List<AccessibilityViolation> validateAriaRelationships() {
        List<AccessibilityViolation> violations = new ArrayList<>();
        
        try {
            Object result = page.evaluate("() => { " +
                "const elements = Array.from(document.querySelectorAll('[aria-labelledby], [aria-describedby], [aria-controls]')); " +
                "return elements.map(el => ({ " +
                "  tag: el.tagName.toLowerCase(), " +
                "  ariaLabelledBy: el.getAttribute('aria-labelledby'), " +
                "  ariaDescribedBy: el.getAttribute('aria-describedby'), " +
                "  ariaControls: el.getAttribute('aria-controls') " +
                "})); " +
                "}");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> elements = (List<Map<String, Object>>) result;
            
            for (Map<String, Object> el : elements) {
                String ariaLabelledBy = (String) el.get("ariaLabelledBy");
                String ariaDescribedBy = (String) el.get("ariaDescribedBy");
                
                if (ariaLabelledBy != null && !elementExists(ariaLabelledBy)) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "4.1.2",
                        "Name, Role, Value",
                        "aria-labelledby references non-existent element",
                        "Element: " + el.get("tag") + ", id: " + ariaLabelledBy,
                        "Ensure referenced element exists with matching id",
                        AccessibilityViolation.ViolationType.INVALID_ARIA_RELATIONSHIP
                    ));
                }
                
                if (ariaDescribedBy != null && !elementExists(ariaDescribedBy)) {
                    violations.add(new AccessibilityViolation(
                        AccessibilityViolation.WCAGLevel.A,
                        "4.1.2",
                        "Name, Role, Value",
                        "aria-describedby references non-existent element",
                        "Element: " + el.get("tag") + ", id: " + ariaDescribedBy,
                        "Ensure referenced element exists with matching id",
                        AccessibilityViolation.ViolationType.INVALID_ARIA_RELATIONSHIP
                    ));
                }
            }
        } catch (Exception e) {
            TestLogManager.warning("Error validating ARIA relationships: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Check if element exists by ID
     */
    private boolean elementExists(String id) {
        try {
            Object result = page.evaluate("(id) => document.getElementById(id) !== null", id);
            return (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if value is valid boolean
     */
    private boolean isValidBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }
}

