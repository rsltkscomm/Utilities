package ai;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks self-healing behavior across test runs.
 *
 * PURPOSE:
 * - Measure locator stability
 * - Identify flaky selectors
 * - Recommend permanent locator fixes
 *
 * NOTE:
 * - Does NOT affect test execution
 * - Purely analytics & reporting
 */
public class HealingTracker {

    private static volatile HealingTracker instance;

    /** elementName -> healing attempts */
    private final Map<String, List<HealingAttempt>> healingAttempts = new ConcurrentHashMap<>();

    /** elementName -> locator usage */
    private final Map<String, LocatorUsage> locatorUsage = new ConcurrentHashMap<>();

    private HealingTracker() {}

    public static HealingTracker getInstance() {
        if (instance == null) {
            synchronized (HealingTracker.class) {
                if (instance == null) {
                    instance = new HealingTracker();
                }
            }
        }
        return instance;
    }

    // ===================== RECORDING =====================

    /** Original locator worked */
    public void recordSuccess(String elementName, String locator) {
        locatorUsage
            .computeIfAbsent(elementName, k -> new LocatorUsage())
            .recordSuccess(locator);
    }

    /** Healing succeeded */
    public void recordHealing(String elementName, String originalLocator, String healedLocator) {
        healingAttempts
            .computeIfAbsent(elementName, k -> new ArrayList<>())
            .add(new HealingAttempt(originalLocator, healedLocator, true));

        locatorUsage
            .computeIfAbsent(elementName, k -> new LocatorUsage())
            .recordHealing(originalLocator, healedLocator);
    }

    /** Healing failed */
    public void recordFailure(String elementName, String originalLocator) {
        healingAttempts
            .computeIfAbsent(elementName, k -> new ArrayList<>())
            .add(new HealingAttempt(originalLocator, null, false));

        locatorUsage
            .computeIfAbsent(elementName, k -> new LocatorUsage())
            .recordFailure(originalLocator);
    }

    // ===================== QUERY =====================

    public HealingStatistics getStatistics(String elementName) {
        List<HealingAttempt> attempts =
                healingAttempts.getOrDefault(elementName, Collections.emptyList());
        LocatorUsage usage = locatorUsage.get(elementName);

        if (attempts.isEmpty() && usage == null) {
            return null;
        }
        return new HealingStatistics(elementName, attempts, usage);
    }

    /** Elements that SHOULD have selectors updated */
    public List<String> getElementsNeedingUpdate() {
        List<String> result = new ArrayList<>();

        for (String element : healingAttempts.keySet()) {
            HealingStatistics stats = getStatistics(element);
            if (stats != null && stats.shouldUpdateSelector()) {
                result.add(element);
            }
        }
        return result;
    }

    // ===================== RESET =====================

    public void clear(String elementName) {
        healingAttempts.remove(elementName);
        locatorUsage.remove(elementName);
    }

    public void clearAll() {
        healingAttempts.clear();
        locatorUsage.clear();
    }

    // ===================== DATA MODELS =====================

    public static class HealingAttempt {
        private final String originalLocator;
        private final String healedLocator;
        private final boolean success;

        public HealingAttempt(String originalLocator, String healedLocator, boolean success) {
            this.originalLocator = originalLocator;
            this.healedLocator = healedLocator;
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getHealedLocator() {
            return healedLocator;
        }
    }

    /**
     * Tracks how locators behave over time.
     * Provides aggregate metrics for HealingStatistics.
     */
    public static class LocatorUsage {

        private final Map<String, Integer> success = new ConcurrentHashMap<>();
        private final Map<String, Integer> failure = new ConcurrentHashMap<>();
        private final Map<String, String> healed = new ConcurrentHashMap<>();

        // ---------- Recording ----------

        void recordSuccess(String locator) {
            success.merge(locator, 1, Integer::sum);
        }

        void recordFailure(String locator) {
            failure.merge(locator, 1, Integer::sum);
        }

        void recordHealing(String original, String healedLocator) {
            healed.put(original, healedLocator);
        }

        // ---------- Aggregates (REQUIRED BY HealingStatistics) ----------

        /** Total successful usages of original locator */
        public int getTotalSuccesses() {
            return success.values().stream().mapToInt(Integer::intValue).sum();
        }

        /** Total failures of original locator */
        public int getTotalFailures() {
            return failure.values().stream().mapToInt(Integer::intValue).sum();
        }

        /** Recommended healed locator (best candidate) */
        public String getRecommendedLocator() {
            return healed.values()
                    .stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
    }
}
