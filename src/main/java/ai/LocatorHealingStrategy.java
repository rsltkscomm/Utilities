package ai;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements various strategies for healing broken locators
 */
class LocatorHealingStrategy {
    
    private final Page page;
    
    public LocatorHealingStrategy(Page page) {
        this.page = page;
    }
    
    /**
     * Strategy 1: Try alternative locator types
     */
    public Locator tryAlternativeLocatorTypes(SelfHealingLocator.ParsedLocator original, String description) {
        String elementName = original.getElementName().toLowerCase();
        String originalType = original.getLocatorType().toLowerCase();
        String value = original.getLocatorValue();
        
        // Try different locator types based on element name patterns
        List<String> alternativeTypes = getAlternativeTypes(elementName, originalType);
        
        for (String altType : alternativeTypes) {
            try {
                SelfHealingLocator.ParsedLocator altLocator = new SelfHealingLocator.ParsedLocator(
                    original.getElementName(), altType, value);
                Locator locator = createLocator(altLocator);
                
                if (isValidLocator(locator)) {
                    return locator;
                }
            } catch (Exception e) {
                // Try next alternative
            }
        }
        
        return null;
    }
    
    /**
     * Strategy 2: Semantic search using element name and description
     */
    public Locator trySemanticSearch(String elementName, String description) {
        if (description == null || description.isEmpty()) {
            description = elementName;
        }
        
        // Extract keywords from element name and description
        String[] keywords = extractKeywords(elementName + " " + description);
        
        // Try finding by text content
        for (String keyword : keywords) {
            if (keyword.length() < 3) continue; // Skip very short keywords
            
            try {
                // Try exact text match
                Locator locator = page.getByText(keyword, new Page.GetByTextOptions().setExact(true));
                if (isValidLocator(locator)) {
                    return locator;
                }
                
                // Try partial text match
                locator = page.getByText(keyword, new Page.GetByTextOptions().setExact(false));
                if (isValidLocator(locator)) {
                    return locator;
                }
            } catch (Exception e) {
                // Continue to next keyword
            }
        }
        
        return null;
    }
    
    /**
     * Strategy 3: Partial matching
     */
    public Locator tryPartialMatching(SelfHealingLocator.ParsedLocator original, String description) {
        String value = original.getLocatorValue();
        
        // Try partial ID matching
        if (original.getLocatorType().equalsIgnoreCase("id")) {
            String[] parts = value.split("[_-]");
            for (String part : parts) {
                if (part.length() > 3) {
                    try {
                        Locator locator = page.locator("#" + part);
                        if (isValidLocator(locator)) {
                            return locator;
                        }
                    } catch (Exception e) {
                        // Continue
                    }
                }
            }
        }
        
        // Try partial class matching
        if (original.getLocatorType().equalsIgnoreCase("class") || 
            original.getLocatorType().equalsIgnoreCase("classname")) {
            String[] parts = value.split(" ");
            for (String part : parts) {
                if (part.length() > 3) {
                    try {
                        Locator locator = page.locator("." + part);
                        if (isValidLocator(locator)) {
                            return locator;
                        }
                    } catch (Exception e) {
                        // Continue
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Strategy 4: Text-based search
     */
    public Locator tryTextBasedSearch(String elementName, String description) {
        String searchText = description != null && !description.isEmpty() ? description : elementName;
        
        // Clean up search text
        searchText = searchText.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        
        if (searchText.length() < 3) return null;
        
        try {
            // Try button with text
            Locator button = page.locator("button:has-text('" + searchText + "')");
            if (isValidLocator(button)) {
                return button;
            }
            
            // Try link with text
            Locator link = page.locator("a:has-text('" + searchText + "')");
            if (isValidLocator(link)) {
                return link;
            }
            
            // Try input with placeholder
            Locator input = page.locator("input[placeholder*='" + searchText + "']");
            if (isValidLocator(input)) {
                return input;
            }
            
            // Try label with text
            Locator label = page.locator("label:has-text('" + searchText + "')");
            if (isValidLocator(label)) {
                return label;
            }
            
        } catch (Exception e) {
            // Return null if all attempts fail
        }
        
        return null;
    }
    
    /**
     * Strategy 5: Attribute-based search
     */
    public Locator tryAttributeBasedSearch(SelfHealingLocator.ParsedLocator original, String description) {
        String elementName = original.getElementName().toLowerCase();
        String value = original.getLocatorValue();
        
        // Determine likely element type from name
        String tagName = inferTagName(elementName);
        
        // Try common attributes
        String[] attributes = {"id", "name", "class", "data-testid", "data-id", "aria-label", "title"};
        
        for (String attr : attributes) {
            try {
                // Try exact match
                Locator locator = page.locator(tagName + "[" + attr + "='" + value + "']");
                if (isValidLocator(locator)) {
                    return locator;
                }
                
                // Try contains match
                locator = page.locator(tagName + "[" + attr + "*='" + value + "']");
                if (isValidLocator(locator)) {
                    return locator;
                }
            } catch (Exception e) {
                // Continue to next attribute
            }
        }
        
        // Try with description as aria-label or title
        if (description != null && !description.isEmpty()) {
            try {
                Locator locator = page.locator(tagName + "[aria-label*='" + description + "']");
                if (isValidLocator(locator)) {
                    return locator;
                }
                
                locator = page.locator(tagName + "[title*='" + description + "']");
                if (isValidLocator(locator)) {
                    return locator;
                }
            } catch (Exception e) {
                // Continue
            }
        }
        
        return null;
    }
    
    /**
     * Get alternative locator types based on element name
     */
    private List<String> getAlternativeTypes(String elementName, String originalType) {
        List<String> alternatives = new ArrayList<>();
        
        // If original is xpath, try other types
        if (originalType.equals("xpath")) {
            alternatives.add("id");
            alternatives.add("name");
            alternatives.add("css");
            alternatives.add("class");
        }
        
        // If original is id, try name and class
        if (originalType.equals("id")) {
            alternatives.add("name");
            alternatives.add("class");
            alternatives.add("css");
        }
        
        // For buttons, try text-based
        if (elementName.contains("button") || elementName.contains("btn")) {
            alternatives.add("linktext");
            alternatives.add("parlink");
        }
        
        // For links, try text-based
        if (elementName.contains("link") || elementName.contains("anchor")) {
            alternatives.add("linktext");
            alternatives.add("parlink");
        }
        
        return alternatives;
    }
    
    /**
     * Extract keywords from text
     */
    private String[] extractKeywords(String text) {
        // Remove common words and extract meaningful keywords
        String[] commonWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with"};
        String lowerText = text.toLowerCase();
        
        for (String word : commonWords) {
            lowerText = lowerText.replaceAll("\\b" + word + "\\b", "");
        }
        
        return lowerText.trim().split("\\s+");
    }
    
    /**
     * Infer HTML tag name from element name
     */
    private String inferTagName(String elementName) {
        String lower = elementName.toLowerCase();
        
        if (lower.contains("button") || lower.contains("btn")) return "button";
        if (lower.contains("link") || lower.contains("anchor")) return "a";
        if (lower.contains("input") || lower.contains("field") || lower.contains("textbox")) return "input";
        if (lower.contains("select") || lower.contains("dropdown")) return "select";
        if (lower.contains("checkbox")) return "input[type='checkbox']";
        if (lower.contains("radio")) return "input[type='radio']";
        if (lower.contains("image") || lower.contains("img")) return "img";
        if (lower.contains("div")) return "div";
        if (lower.contains("span")) return "span";
        
        return "*"; // Try all elements
    }
    
    /**
     * Create locator from ParsedLocator
     */
    private Locator createLocator(SelfHealingLocator.ParsedLocator parsed) {
        String type = parsed.getLocatorType().toLowerCase();
        String value = parsed.getLocatorValue();
        
        return switch (type) {
            case "id" -> page.locator("#" + value);
            case "name" -> page.locator("[name='" + value + "']");
            case "xpath" -> page.locator(value);
            case "css", "cssselector" -> page.locator(value);
            case "link", "linktext" -> page.getByText(value);
            case "parlink", "partiallinktext" -> page.getByText(value, new Page.GetByTextOptions().setExact(false));
            case "class", "classname" -> page.locator("." + value.replace(" ", "."));
            case "tag", "tagname" -> page.locator(value);
            default -> page.locator(value);
        };
    }
    
    /**
     * Check if locator is valid
     */
    private boolean isValidLocator(Locator locator) {
        try {
            // Quick check with short timeout
            int count = locator.count();
            if (count == 0) return false;
            
            // Check if first element is visible
            return locator.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }
}

