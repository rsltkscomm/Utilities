package patterns.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.remote.DesiredCapabilities;

import base.DriverContext;
import reporting.TestLogManager;

/**
 * Central factory for creating DriverContext instances
 * using Strategy pattern.
 */
public final class DriverFactory {

    private static final Map<String, DriverStrategy> strategies = new HashMap<>();

    static {
        registerStrategies();
    }

    private DriverFactory() {}

    /* ==========================================================
       STRATEGY REGISTRATION
       ========================================================== */

    private static void registerStrategies() {

        boolean headless =
                Boolean.parseBoolean(System.getProperty("headless", "false"));

        boolean remote =
                Boolean.parseBoolean(System.getProperty("remote", "false"));

        String remoteUrl =
                System.getProperty("remoteUrl");

        // ---------- Local / Remote Browsers ----------
        register(new ChromeDriverStrategy(headless, remote, remoteUrl));
        register(new ChromeHeadlessDriverStrategy(remote, remoteUrl));
        register(new FirefoxDriverStrategy(headless, remote, remoteUrl));
        register(new EdgeDriverStrategy(headless, remote, remoteUrl));

        // ---------- Cross-platform alias ----------
        register(new CrossPlatformDriverStrategy("chrome"));
        register(new CrossPlatformDriverStrategy("chromeheadless"));
        register(new CrossPlatformDriverStrategy("firefox"));
        register(new CrossPlatformDriverStrategy("edge"));

        // ---------- Cloud ----------
//        register(new CloudDriverStrategy("browserstack"));
        register(new CloudDriverStrategy("lambdatest"));
//        register(new CloudDriverStrategy("saucelabs"));
    }

    private static void register(DriverStrategy strategy) {
        strategies.put(strategy.getBrowserName().toLowerCase(), strategy);
    }

    /* ==========================================================
       DRIVER CREATION
       ========================================================== */

    public static DriverContext createDriver(String browserType) {
        return createDriver(browserType, null);
    }

    public static DriverContext createDriver(
            String browserType,
            DesiredCapabilities capabilities) {

        if (browserType == null || browserType.isBlank()) {
            throw new IllegalArgumentException(
                    "Browser type cannot be null or empty");
        }

        String normalized = browserType.toLowerCase().trim();
        DriverStrategy strategy = findStrategy(normalized);

        if (strategy == null) {
            throw new UnsupportedOperationException(
                    "Unsupported browser type: " + browserType);
        }

        TestLogManager.info("Creating driver for browser: " + browserType);
        return strategy.createDriver(capabilities);
    }

    /* ==========================================================
       LOOKUP
       ========================================================== */

    private static DriverStrategy findStrategy(String browserType) {

        DriverStrategy direct = strategies.get(browserType);
        if (direct != null) {
            return direct;
        }

        for (DriverStrategy strategy : strategies.values()) {
            if (strategy.supports(browserType)) {
                return strategy;
            }
        }
        return null;
    }

    /* ==========================================================
       UTIL
       ========================================================== */

    public static Set<String> getSupportedBrowsers() {
        return strategies.keySet();
    }

    public static boolean isBrowserSupported(String browserType) {
        return findStrategy(browserType.toLowerCase()) != null;
    }
}
