package ai;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Statistics about healing attempts for an element.
 *
 * PURPOSE:
 * - Analyze locator stability
 * - Decide whether a selector should be permanently updated
 * - Recommend the best healed selector
 *
 * NOTE:
 * - This class does NOT perform healing
 * - It is analytics + decision logic only
 */
public class HealingStatistics {

    private final String elementName;
    private final List<HealingTracker.HealingAttempt> attempts;
    private final HealingTracker.LocatorUsage usage;

    public HealingStatistics(
            String elementName,
            List<HealingTracker.HealingAttempt> attempts,
            HealingTracker.LocatorUsage usage
    ) {
        this.elementName = elementName;
        this.attempts = attempts;
        this.usage = usage;
    }

    // ====================== BASIC METRICS ======================

    /**
     * Total healing attempts
     */
    public int getTotalAttempts() {
        return attempts.size();
    }

    /**
     * Successful healing attempts
     */
    public int getSuccessfulHealings() {
        return (int) attempts.stream()
                .filter(HealingTracker.HealingAttempt::isSuccess)
                .count();
    }

    /**
     * Failed healing attempts
     */
    public int getFailedHealings() {
        return (int) attempts.stream()
                .filter(a -> !a.isSuccess())
                .count();
    }

    /**
     * Healing success rate (0.0 – 1.0)
     */
    public double getHealingSuccessRate() {
        if (attempts.isEmpty()) return 1.0;
        return (double) getSuccessfulHealings() / attempts.size();
    }

    /**
     * Failure rate based on original locator usage
     */
    public double getFailureRate() {
        if (usage == null) return 0.0;

        int failures = usage.getTotalFailures();
        int successes = usage.getTotalSuccesses();
        int total = failures + successes;

        if (total == 0) return 0.0;
        return (double) failures / total;
    }

    // ====================== DECISION LOGIC ======================

    /**
     * Decide if selector SHOULD be permanently updated.
     *
     * Central governance rule for locator health.
     */
    public boolean shouldUpdateSelector() {

        // Rule 1: Original locator fails frequently
        if (getFailureRate() > 0.5) {
            return true;
        }

        // Rule 2: Healing is unreliable
        if (getHealingSuccessRate() < 0.3) {
            return true;
        }

        // Rule 3: More failures than successes overall
        if (usage != null && usage.getTotalFailures() > usage.getTotalSuccesses()) {
            return true;
        }

        return false;
    }

    // ====================== RECOMMENDATION ======================

    /**
     * Best healed locator candidate to replace original selector
     */
    public String getRecommendedSelector() {
        if (usage == null) return null;
        return getMostSuccessfulHealedLocator();
    }

    /**
     * Most successful healed locator based on frequency
     */
    public String getMostSuccessfulHealedLocator() {

        Map<String, Long> frequency = attempts.stream()
                .filter(HealingTracker.HealingAttempt::isSuccess)
                .map(HealingTracker.HealingAttempt::getHealedLocator)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        locator -> locator,
                        Collectors.counting()
                ));

        return frequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // ====================== ACCESSORS ======================

    public String getElementName() {
        return elementName;
    }

    // ====================== DEBUG ======================

    @Override
    public String toString() {
        return String.format(
                "HealingStatistics{element='%s', attempts=%d, successRate=%.2f%%, failureRate=%.2f%%}",
                elementName,
                getTotalAttempts(),
                getHealingSuccessRate() * 100,
                getFailureRate() * 100
        );
    }
}
