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
import reporting.TestLogManager;

/**
 * Playwright Element Utility (Framework-safe, no TODOs)
 */
public class PlaywrightElementUtil extends PlaywrightClickUtil implements ElementInterface {

    protected final DriverContext driverContext;
    private final PlaywrightLocatorUtil locatorUtil;

    public PlaywrightElementUtil(DriverContext driverContext) {
        super(driverContext);
        this.driverContext = driverContext;
        this.locatorUtil = new PlaywrightLocatorUtil(driverContext);
    }

    protected Page page() {
        return driverContext.getPage();
    }

    // ====================== RESOLVERS ======================

    protected Locator resolveLocator(Object obj) {

        if (obj == null)
            return null;

        if (obj instanceof String)
            return locatorUtil.getLocator((String) obj);

        if (obj instanceof PlaywrightWebElement)
            return ((PlaywrightWebElement) obj).locator;

        return null;
    }

    private PlaywrightWebElement resolveElement(Object obj) {
        Locator loc = resolveLocator(obj);
        return loc == null ? null : new PlaywrightWebElement(loc);
    }

    // ====================== TEXT ======================

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
            String text = loc.innerText();
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

    // ====================== SEND KEYS ======================

    @Override
    public boolean sendValue(Object locator, String value) {
        try {
            resolveLocator(locator).fill(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean javaScriptEnterValue(Object locator, String text) {
        try {
            resolveLocator(locator).evaluate("(e,v)=>e.value=v", text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ====================== STATES ======================

    @Override
    public boolean isDisplayed(String locator) {
        try {
        	Locator locator2 = getRawLocator(locator);
            return locator2.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEnabled(String locator) {
        try {
            return !resolveLocator(locator).isDisabled();
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

    // ====================== ENTER VALUE ======================

    @Override
    public boolean enterValue(Object locator, String value) {
        try {
            Locator loc = resolveLocator(locator);
            loc.fill("");
            loc.fill(value);
            return value.equals(loc.inputValue());
        } catch (Exception e) {
            return false;
        }
    }

    // ====================== FIND ELEMENT(S) ======================

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

    // ====================== DROPDOWN ======================

    @Override
    public String getAllDropdownValues(String locator) {
        return resolveLocator(locator)
                .allInnerTexts()
                .stream()
                .collect(Collectors.joining(","));
    }

    // ====================== UTIL ======================

    @Override
    public void tabAction() {
        page().keyboard().press("Tab");
    }

    @Override
    public void clickEnter() {
        page().keyboard().press("Enter");
    }

    @Override
    public void clearField(Object locator) {
        resolveLocator(locator).fill("");
    }

    @Override
    public int findGCV(int a, int b) {
        return (b == 0) ? a : findGCV(b, a % b);
    }

    @Override
    public String normalizeText(String input) {
        if (input == null)
            return "";
        return input.replace("\u00A0", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Override
    public boolean isElementPresent(String locator) {
        try {
            return getRawLocator(locator).count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ====================== TABLE ======================

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

    // ====================== FILE UPLOAD ======================

    @Override
    public boolean uploadFile(String locator, String filePath) {
        try {
            page().setInputFiles(locator, Paths.get(filePath));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ====================== LINK VALIDATION ======================

    @Override
    public boolean validateInvalidLinks(String pageName) {

        try {
            List<ElementHandle> anchors = page().querySelectorAll("a");

            for (ElementHandle anchor : anchors) {
                String url = anchor.getAttribute("href");
                if (url != null && !url.contains("javascript")) {
                    if (!verifyURLStatus(url))
                        return false;
                }
            }
            return true;
        } catch (Exception e) {
            TestLogManager.error("validateInvalidLinks error", e);
            return false;
        }
    }

    private boolean verifyURLStatus(String url) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet request = new HttpGet(url);
            CloseableHttpResponse response = client.execute(request);
            return response.getStatusLine().getStatusCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getCssValue(String locator, String property) {
        try {
            return resolveLocator(locator)
                    .evaluate(
                        "(el, prop) => window.getComputedStyle(el).getPropertyValue(prop)",
                        property
                    )
                    .toString()
                    .trim();
        } catch (Exception e) {
            return null;
        }
    }

}
