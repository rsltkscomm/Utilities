package core.playwright;

import java.util.Base64;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import base.DriverContext;
import core.interfaces.AssertInterface;
import reporting.ExtentManager;
import reporting.TestLogManager;

public class PlaywrightAssertUtil extends PlaywrightElementUtil
        implements AssertInterface {

    protected final DriverContext driverContext;

    public PlaywrightAssertUtil(DriverContext driverContext) {
        super(driverContext); // ✅ parent now also uses DriverContext
        this.driverContext = driverContext;
    }

    /**
     * Always resolve the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    // ------------------------------------------------------------------
    // LOGGING WITH SCREENSHOT
    // ------------------------------------------------------------------

    @Override
    public boolean writeLog(boolean expression, String passLog, String failLog) {
        if (expression) {
            ExtentManager.infoTest(passLog);
        } else {
            ExtentManager.warningTest(failLog);
        }
        takeScreenshot();
        return expression;
    }

    @Override
    public boolean checkIsElementNull(Object element) {
        return element != null;
    }

    @Override
    public boolean writeLogger(boolean expression, String passLog, String failLog) {

        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String location = caller.getClassName() + "." + caller.getMethodName() +
                "():" + caller.getLineNumber();

        String message = (expression ? passLog : failLog) + " [" + location + "]";

        if (expression) {
            TestLogManager.info(message);
            ExtentManager.infoTest(message);
        } else {
            TestLogManager.error(message);
            ExtentManager.warningTest(message);
        }

        takeScreenshot();
        return expression;
    }

    // ------------------------------------------------------------------
    // PLACEHOLDER CHECK
    // ------------------------------------------------------------------

    @Override
    public boolean placeholderValueCheck(String locatorKey, String placeHolderText) {
        Locator locator = resolveLocator(locatorKey);
        String actual = locator.innerText();

        boolean status = placeHolderText.equals(actual);

        if (status) {
            ExtentManager.infoTest(
                "UI Placeholder Text <b>" + actual +
                "</b> is displayed as expected <b>" + placeHolderText + "</b>"
            );
        } else {
            ExtentManager.failTest(
                "UI Placeholder Text <b>" + actual +
                "</b> does NOT match expected <b>" + placeHolderText + "</b>"
            );
        }
        return status;
    }

    // ------------------------------------------------------------------
    // BACKGROUND COLOR VALIDATION
    // ------------------------------------------------------------------

    @Override
    public boolean validateUiBackgroundColour(
            String cssProperty, String pr, String expectedHex) {
        try {
            Locator locator = resolveLocator(pr);

            String cssValue = locator.evaluate(
                "(el, prop) => getComputedStyle(el)[prop]",
                cssProperty
            ).toString();

            String rgb = cssValue.substring(
                cssValue.indexOf("(") + 1, cssValue.indexOf(")")
            );
            String[] colors = rgb.replace(" ", "").split(",");

            String actualHex = String.format(
                "#%02x%02x%02x",
                Integer.parseInt(colors[0]),
                Integer.parseInt(colors[1]),
                Integer.parseInt(colors[2])
            );

            boolean isMatch = actualHex.equalsIgnoreCase(expectedHex);

            writeLog(
                isMatch,
                "Background colour matches expected: " + actualHex,
                "Background colour mismatch. Expected: " + expectedHex +
                ", Found: " + actualHex
            );

            return isMatch;

        } catch (Exception e) {
            ExtentManager.warningTest(
                "Error validating background colour for locator [" +
                pr + "]: " + e.getMessage()
            );
            return false;
        }
    }

    // ------------------------------------------------------------------
    // MULTI VALUE UI ASSERT
    // ------------------------------------------------------------------

    @Override
    public boolean uiPageEqualsWithMultipleInputValue(
            String locatorKey, String testDatas) {

        List<String> uiTexts = resolveLocator(locatorKey)
                .allTextContents()
                .stream()
                .map(String::trim)
                .toList();

        String[] expectedValues = testDatas.split(",");

        if (uiTexts.isEmpty()) {
            ExtentManager.getTest().fail(
                "No elements found for locator: " + locatorKey
            );
            takeScreenshot();
            return false;
        }

        if (uiTexts.size() != expectedValues.length) {
            ExtentManager.getTest().fail(
                "UI values count (" + uiTexts.size() +
                ") does not match expected count (" +
                expectedValues.length + ")"
            );
            return false;
        }

        boolean allMatch = true;

        for (int i = 0; i < uiTexts.size(); i++) {
            String actual = uiTexts.get(i);
            String expected = expectedValues[i].trim();

            if (actual.equals(expected)) {
                ExtentManager.getTest().info(
                    "UI text <b>'" + actual +
                    "'</b> matches expected."
                );
            } else {
                ExtentManager.getTest().fail(
                    "UI text <b>'" + actual +
                    "'</b> does NOT match expected <b>'" +
                    expected + "'</b>"
                );
                allMatch = false;
            }
        }
        return allMatch;
    }

    // ------------------------------------------------------------------
    // SIMPLE TEXT MATCH
    // ------------------------------------------------------------------

    @Override
    public boolean uiPageEqualswithInputValue(
            String txt, String actualText) {

        boolean flag = txt.trim().equals(actualText.trim());

        if (flag) {
            ExtentManager.infoTest(
                "UI text <b>'" + txt +
                "'</b> is displayed as expected."
            );
        } else {
            ExtentManager.warningTest(
                "UI text <b>'" + txt +
                "'</b> is NOT displayed as expected <b>" +
                actualText + "</b>"
            );
        }
        return flag;
    }

    // ------------------------------------------------------------------
    // BASE64 DECODE
    // ------------------------------------------------------------------

    @Override
    public String decodeBase64ToText(String base64Text) {
        return new String(Base64.getDecoder().decode(base64Text));
    }
}
