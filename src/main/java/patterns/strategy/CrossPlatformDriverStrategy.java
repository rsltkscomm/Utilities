package patterns.strategy;

import org.openqa.selenium.remote.DesiredCapabilities;

import base.DriverContext;
import reporting.TestLogManager;

/**
 * Cross-platform driver strategy that delegates to
 * browser-specific strategies.
 */
public class CrossPlatformDriverStrategy implements DriverStrategy {

    private final String browserType;
    private final DriverStrategy delegateStrategy;

    public CrossPlatformDriverStrategy(String browserType) {
        this.browserType = browserType.toLowerCase();
        this.delegateStrategy = createDelegateStrategy();
    }

    @Override
    public DriverContext createDriver() {
        return createDriver(null);
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {

        if (delegateStrategy == null) {
            throw new UnsupportedOperationException(
                    "Unsupported browser type: " + browserType);
        }

        TestLogManager.info(
                "Creating cross-platform driver for: " + browserType);

        return delegateStrategy.createDriver(capabilities);
    }

    @Override
    public String getBrowserName() {
        return browserType;
    }

    @Override
    public boolean supports(String browserType) {
        return this.browserType.equalsIgnoreCase(browserType);
    }

    /* ==========================================================
       DELEGATE CREATION
       ========================================================== */

    private DriverStrategy createDelegateStrategy() {

        boolean headless =
                Boolean.parseBoolean(
                        System.getProperty("headless", "false"));

        boolean remote =
                Boolean.parseBoolean(
                        System.getProperty("remote", "false"));

        String remoteUrl =
                System.getProperty("remoteUrl");

        switch (browserType) {

            case "chrome":
                return new ChromeDriverStrategy(
                        headless, remote, remoteUrl);

            case "chromeheadless":
            case "chrome-headless":
                return new ChromeHeadlessDriverStrategy(
                        remote, remoteUrl);

            case "firefox":
                return new FirefoxDriverStrategy(
                        headless, remote, remoteUrl);

            case "edge":
                return new EdgeDriverStrategy(
                        headless, remote, remoteUrl);

            case "browserstack":
            case "bs":
            case "cloud":
                return new CloudDriverStrategy(browserType);

            default:
                TestLogManager.warning(
                        "Unsupported browser type for cross-platform strategy: "
                                + browserType);
                return null;
        }
    }
}
