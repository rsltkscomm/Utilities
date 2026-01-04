package ai;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import reporting.TestLogManager;

/**
 * Central Self-Healing Locator Engine
 *
 * Responsibilities:
 * - Try original locator
 * - Heal when locator fails
 * - Delegate strategies to LocatorHealingStrategy
 *
 * Does NOT:
 * - Perform element actions
 * - Log test steps
 */
public class SelfHealingLocator {

    private final Page page;
    private final LocatorHealingStrategy healingStrategy;

    public SelfHealingLocator(Page page) {
        this.page = page;
        this.healingStrategy = new LocatorHealingStrategy(page);
    }

    /**
     * Public API used by Element / Locator utils
     */
    public Locator findElement(String locatorString) {
        return findElement(locatorString, null);
    }

    public Locator findElement(String locatorString, String description) {

        ParsedLocator parsed = parse(locatorString);

        // 1️⃣ Try original locator
        Locator original = createLocator(parsed);
        if (isValid(original)) {
            return original;
        }

        TestLogManager.warning(
                "Locator failed, attempting self-healing for element: " + parsed.elementName
        );

        // 2️⃣ Strategy: Alternative locator types
        Locator healed = healingStrategy.tryAlternativeLocatorTypes(parsed, description);
        if (isValid(healed)) return healed;

        // 3️⃣ Strategy: Semantic search
        healed = healingStrategy.trySemanticSearch(parsed.elementName, description);
        if (isValid(healed)) return healed;

        // 4️⃣ Strategy: Partial matching
        healed = healingStrategy.tryPartialMatching(parsed, description);
        if (isValid(healed)) return healed;

        // 5️⃣ Strategy: Text-based search
        healed = healingStrategy.tryTextBasedSearch(parsed.elementName, description);
        if (isValid(healed)) return healed;

        // 6️⃣ Strategy: Attribute-based search
        healed = healingStrategy.tryAttributeBasedSearch(parsed, description);
        if (isValid(healed)) return healed;

        throw new RuntimeException(
                "Element not found even after self-healing: " + parsed.elementName
        );
    }

    // ====================================================================================
    // INTERNAL HELPERS
    // ====================================================================================

    private ParsedLocator parse(String locatorString) {

        if (locatorString == null || locatorString.isEmpty()) {
            throw new IllegalArgumentException("Locator string cannot be null or empty");
        }

        String[] parts = locatorString.split(",", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Invalid locator format: " + locatorString +
                    " | Expected: ElementName,locatorType,value"
            );
        }

        return new ParsedLocator(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim()
        );
    }

    private Locator createLocator(ParsedLocator parsed) {

        String type = parsed.locatorType.toLowerCase();
        String value = parsed.locatorValue;

        return switch (type) {
            case "id" -> page.locator("#" + value);
            case "name" -> page.locator("[name='" + value + "']");
            case "xpath" -> page.locator("xpath=" + value);
            case "css", "cssselector" -> page.locator(value);
            case "link", "linktext" -> page.getByText(value);
            case "parlink", "partiallinktext" ->
                    page.getByText(value, new Page.GetByTextOptions().setExact(false));
            case "class", "classname" -> page.locator("." + value.replace(" ", "."));
            case "tag", "tagname" -> page.locator(value);
            default -> page.locator(value);
        };
    }

    private boolean isValid(Locator locator) {
        try {
            return locator != null
                    && locator.count() > 0
                    && locator.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    // ====================================================================================
    // DATA HOLDER
    // ====================================================================================

    public static class ParsedLocator {

        private final String elementName;
        private final String locatorType;
        private final String locatorValue;

        public ParsedLocator(String elementName, String locatorType, String locatorValue) {
            this.elementName = elementName;
            this.locatorType = locatorType;
            this.locatorValue = locatorValue;
        }

        public String getElementName() {
            return elementName;
        }

        public String getLocatorType() {
            return locatorType;
        }

        public String getLocatorValue() {
            return locatorValue;
        }
    }
}
