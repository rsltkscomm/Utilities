package ai.openai;

import ai.SelfHealingLocator;
import config.ConfigurationManager;
import reporting.TestLogManager;

/**
 * OpenAI-powered locator healing.
 *
 * PURPOSE:
 * - Acts as a LAST-RESORT healing mechanism
 * - Invoked only when all deterministic healing strategies fail
 *
 * IMPORTANT:
 * - Completely optional
 * - Disabled by default
 * - Never blocks test execution
 */
public class OpenAILocatorHealer {

    private final OpenAIClient openAIClient;
    private final OpenAIIntegration integration;
    private final ConfigurationManager config;

    public OpenAILocatorHealer() {
        this.openAIClient = OpenAIClient.getInstance();
        this.integration = new OpenAIIntegration();
        this.config = ConfigurationManager.getInstance();
    }

    /**
     * Attempt to find an alternative locator using OpenAI.
     *
     * @param originalLocator Original parsed locator
     * @param description     Human-readable element description (optional)
     * @return Alternative locator string OR null if not found / disabled
     */
    public String findAlternativeLocator(
            SelfHealingLocator.ParsedLocator originalLocator,
            String description
    ) {

        // ===== Guard rails (AI must be opt-in) =====
        if (originalLocator == null) {
            return null;
        }

        if (!integration.isEnabled()) {
            return null;
        }

        if (!config.getBoolean("self.healing.enabled", false)) {
            return null;
        }

        if (!config.getBoolean("self.healing.openai.enabled", false)) {
            return null;
        }

        try {
            String promptDescription = description != null ? description : "";

            String alternative = integration.findAlternativeLocatorWithOpenAI(
                    originalLocator.getElementName(),
                    promptDescription,
                    originalLocator.toString()
            );

            // ===== Validate AI output =====
            if (isValidAlternative(alternative)) {
                TestLogManager.info(
                        "OpenAI locator healing succeeded for element '"
                                + originalLocator.getElementName()
                                + "' → " + alternative
                );
                return alternative;
            }

        } catch (Exception e) {
            TestLogManager.warning(
                    "OpenAI locator healing failed for element '"
                            + originalLocator.getElementName()
                            + "' : " + e.getMessage()
            );
        }

        return null;
    }

    // ====================================================================================
    // VALIDATION
    // ====================================================================================

    /**
     * Minimal sanity checks to prevent bad AI output from breaking tests.
     */
    private boolean isValidAlternative(String locator) {

        if (locator == null || locator.isBlank()) {
            return false;
        }

        // Reject obvious hallucinations or explanations
        if (locator.length() > 500) {
            return false;
        }

        // Must look like a selector (very lightweight check)
        return locator.startsWith("//")
                || locator.startsWith("xpath=")
                || locator.startsWith("#")
                || locator.startsWith(".")
                || locator.contains("[")
                || locator.startsWith("text=");
    }
}
