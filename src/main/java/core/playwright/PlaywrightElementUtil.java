package core.playwright;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import base.DriverContext;
import core.interfaces.ElementInterface;
import reporting.ExtentManager;
import reporting.TestLogManager;

public class PlaywrightElementUtil extends PlaywrightClickUtil
        implements ElementInterface {

    protected final DriverContext driverContext;

    public PlaywrightElementUtil(DriverContext driverContext) {
        super(driverContext); // ✅ parent uses DriverContext
        this.driverContext = driverContext;
    }

    /**
     * Always resolve the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    /**
     * Always resolve locator util with CURRENT page
     */
    protected PlaywrightLocatorUtil locatorUtil() {
        return new PlaywrightLocatorUtil(driverContext);
    }

    // ---------------- RESOLVERS ----------------

    public Locator resolveLocator(Object obj) {
        if (obj == null) return null;

        if (obj instanceof String)
            return locatorUtil().getLocator((String) obj);

        if (obj instanceof PlaywrightWebElement)
            return ((PlaywrightWebElement) obj).locator;

        return null;
    }

    private PlaywrightWebElement resolveElement(Object obj) {
        if (obj instanceof PlaywrightWebElement)
            return (PlaywrightWebElement) obj;

        Locator loc = resolveLocator(obj);
        if (loc == null) return null;

        return new PlaywrightWebElement(loc);
    }

    // ---------------- TEXT METHODS ----------------

    @Override
    public String getTextBoxValue(Object locator, String attribute) {
        try {
            return resolveLocator(locator).getAttribute(attribute);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getText(Object locator) {
        try {
            Locator loc = resolveLocator(locator);
            if (loc == null) {
                return null;
            }
            String text;
            try {
                text = loc.innerText();
            } catch (Exception ignored) {
                text = loc.textContent();
            }
            return text != null ? text.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }


    @Override
    public String getAttribute(Object locator, String attribute) {
        try {
            return resolveLocator(locator).getAttribute(attribute);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- SEND KEYS ----------------

    @Override
    public boolean sendValue(Object locator, String value) {
        try {
            resolveLocator(locator).fill(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- CSS ----------------

    @Override
    public String getCssValue(String locator, String property) {
        try {
            return resolveLocator(locator)
                .evaluate("(e, p) => getComputedStyle(e).getPropertyValue(p)", property)
                .toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- STATES ----------------

    @Override
    public boolean isDisplayed(String locator) {
        try {
            return resolveLocator(locator).isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEnabled(String locator) {
        try {
            return !Boolean.TRUE.equals(resolveLocator(locator).isDisabled());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isSelected(String locator) {
        try {
            return resolveLocator(locator).isChecked();
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- ENTER VALUE ----------------

    @Override
    public boolean enterValue(Object locator, String value) {
        try {
            Locator loc = resolveLocator(locator);
            loc.fill("");
            loc.fill(value);
            return loc.inputValue().equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- JS ENTER VALUE ----------------

    @Override
    public boolean javaScriptEnterValue(Object locator, String text) {
        try {
            resolveLocator(locator).evaluate("(e, v) => e.value = v", text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- FIND ELEMENT(S) ----------------

    @Override
    public Object findElement(Object locator) {
        return resolveElement(locator);
    }

    @Override
    public List<?> findElements(Object locator) {
        Locator loc = resolveLocator(locator);
        int count = loc.count();

        List<PlaywrightWebElement> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new PlaywrightWebElement(loc.nth(i)));
        }
        return list;
    }

    // ---------------- DROPDOWN ----------------

    @Override
    public String getAllDropdownValues(String locator) {
        return resolveLocator(locator)
                .allInnerTexts()
                .stream()
                .collect(Collectors.joining(","));
    }

    // ---------------- KEYS ----------------

    @Override
    public void tabAction() {
        page().keyboard().press("Tab");
    }

    @Override
    public void clickEnter() {
        page().keyboard().press("Enter");
    }

    // ---------------- CLEAR FIELD ----------------

    @Override
    public void clearField(Object locator) {
        resolveLocator(locator).fill("");
    }

    // ---------------- TABLE HANDLER ----------------

    @Override
    public List<Map<String, String>> getWebTable(String tableLocator) {
        Locator table = resolveLocator(tableLocator);

        List<String> headers = table.locator("th").allInnerTexts();
        int rows = table.locator("tbody tr").count();

        List<Map<String, String>> list = new LinkedList<>();

        for (int i = 0; i < rows; i++) {
            Locator row = table.locator("tbody tr").nth(i);
            List<String> cols = row.locator("td").allInnerTexts();

            Map<String, String> map = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                map.put(headers.get(j), cols.get(j));
            }
            list.add(map);
        }
        return list;
    }

    // ---------------- UTIL ----------------

    @Override
    public int findGCV(int a, int b) {
        return (b == 0) ? a : findGCV(b, a % b);
    }

    @Override
    public String normalizeText(String input) {
        if (input == null) return "";
        return input.replace("\u00A0", " ")
                .replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Override
    public boolean isElementPresent(String locator) {
        try {
            return resolveLocator(locator).count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- LINK VALIDATION ----------------

    @Override
    public boolean validateInvalidLinks(String pageName) {

        int invalidLinksCount = 0;
        int validLinksCount = 0;
        int linksWithJavascriptOrNull = 0;

        try {
            List<ElementHandle> anchors = page().querySelectorAll("a");

            ExtentManager.infoTest(
                "Total <a> tags found on <b>" + pageName + "</b> page: " + anchors.size());

            for (ElementHandle anchor : anchors) {

                String url = anchor.getAttribute("href");

                if (url != null &&
                        !url.trim().isEmpty() &&
                        !url.contains("javascript") &&
                        !url.equals("\"\"")) {

                    if (verifyURLStatus(url)) {
                        validLinksCount++;
                    } else {
                        invalidLinksCount++;
                    }

                } else {
                    linksWithJavascriptOrNull++;
                }
            }

            ExtentManager.infoTest("Valid links: " + validLinksCount);
            ExtentManager.infoTest("Invalid links: " + invalidLinksCount);
            ExtentManager.infoTest("Links with null/javascript: " + linksWithJavascriptOrNull);

            return invalidLinksCount == 0;

        } catch (Exception e) {
            ExtentManager.failTest(
                "Exception in validateInvalidLinks for page <b>" +
                pageName + "</b>: " + e.getMessage());
            TestLogManager.error("validateInvalidLinks error", e);
            return false;
        }
    }

    private boolean verifyURLStatus(String url) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Mozilla/5.0");

            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                ExtentManager.warningTest("Invalid link: " + url);
                return false;
            }

            ExtentManager.infoTest("Valid link: " + url);
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Exception verifying URL: " + url);
            TestLogManager.error("verifyURLStatus error", e);
            return false;
        }
    }

    // ---------------- FILE UPLOAD ----------------

    @Override
    public boolean uploadFile(String locator, String filePath) {
        try {
            page().setInputFiles(locator, Paths.get(filePath));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
