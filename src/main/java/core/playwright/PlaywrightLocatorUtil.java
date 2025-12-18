package core.playwright;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import core.interfaces.LocatorInterface;

/**
 * Playwright equivalent of Selenium LocatorUtil.
 * Fully compatible with LocatorInterface and main project expectations.
 */
public class PlaywrightLocatorUtil implements LocatorInterface {

    private final Page page;

    public PlaywrightLocatorUtil(Page page) {
        this.page = page;
    }

    public static ThreadLocal<String> logName = new ThreadLocal<>();

    // ============================================================================================
    // SELENIUM COMPATIBLE By autolocator()
    // ============================================================================================
    @Override
    public By autolocator(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Locator string cannot be null or empty.");
        }

        String[] parts = key.split(",", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                "Invalid locator format: " + key +
                " | Expected format: 'ElementName,locatorType,value'"
            );
        }

        logName.set(parts[0].trim());   // same as Selenium version
        String locatorType = parts[1].trim().toLowerCase(Locale.ROOT);
        String locatorValue = parts[2].trim();

        switch (locatorType) {
            case "id":
                return By.id(locatorValue);
            case "name":
                return By.name(locatorValue);
            case "xpath":
                return By.xpath(locatorValue);
            case "css":
            case "cssselector":
                return By.cssSelector(locatorValue);
            case "link":
            case "linktext":
                return By.linkText(locatorValue);
            case "parlink":
            case "partiallinktext":
                return By.partialLinkText(locatorValue);
            case "class":
            case "classname":
                return By.className(locatorValue);
            case "tag":
            case "tagname":
                return By.tagName(locatorValue);
            default:
                throw new IllegalArgumentException("Unsupported locator type: " + locatorType);
        }
    }

    // ============================================================================================
    // Convert Selenium locator string → Playwright selector
    // ============================================================================================
    private String toPlaywrightSelector(String key) {

        String[] parts = key.split(",", 3);
        if (parts.length < 3)
            throw new IllegalArgumentException("Invalid locator format: " + key);

        String locatorType = parts[1].trim().toLowerCase(Locale.ROOT);
        String locatorValue = parts[2].trim();

        logName.set(parts[0].trim());

        switch (locatorType) {

            case "id":
                return "#" + escapeCss(locatorValue);

            case "name":
                return "[name=\"" + escapeCss(locatorValue) + "\"]";

            case "xpath":
                return "xpath=" + locatorValue;

            case "css":
            case "cssselector":
                return locatorValue;

            case "link":
            case "linktext":
                return "text=\"" + locatorValue + "\"";

            case "parlink":
            case "partiallinktext":
                return "text=" + locatorValue;

            case "class":
            case "classname":
                return "." + locatorValue;

            case "tag":
            case "tagname":
                return locatorValue;

            default:
                return locatorValue;
        }
    }

    private String escapeCss(String s) {
        return s.replace("\"", "\\\"").replace(" ", "\\ ");
    }

    // ============================================================================================
    // NEW: Playwright locator getter (core method)
    // ============================================================================================
    public Locator getLocator(String key) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("Key cannot be null or empty");

        String selector = toPlaywrightSelector(key);
        return page.locator(selector);
    }

    // ============================================================================================
    // WebElement (wrapped) getters
    // ============================================================================================
    @Override
    public WebElement getElement(Object pr) {

        if (pr instanceof PlaywrightWebElement)
            return (PlaywrightWebElement) pr;

        if (pr instanceof String) {
            String selector = toPlaywrightSelector(pr.toString());
            Locator locator = page.locator(selector);
            return new PlaywrightWebElement(locator);
        }

        if (pr instanceof WebElement)
            return (WebElement) pr;

        throw new IllegalArgumentException("getElement expects String or WebElement");
    }

    @Override
    public List<WebElement> getElements(Object pr) {

        if (pr instanceof PlaywrightWebElement)
            return Arrays.asList((WebElement) pr);

        if (pr instanceof String) {
            String selector = toPlaywrightSelector(pr.toString());
            Locator locator = page.locator(selector);

            int count = locator.count();
            List<WebElement> list = new ArrayList<>();

            for (int i = 0; i < count; i++)
                list.add(new PlaywrightWebElement(locator.nth(i)));

            return list;
        }

        if (pr instanceof WebElement)
            return Arrays.asList((WebElement) pr);

        throw new IllegalArgumentException("getElements expects String or WebElement");
    }

    // ============================================================================================
    // Replace Placeholders — SAME AS SELENIUM
    // ============================================================================================
    @Override
    public String replacePlaceHolder(String locator, String placeHolder) {
        return locator.replace("PLACE_HOLDER", placeHolder);
    }

    @Override
    public String replacePlaceHolder(String locator, int placeHolder) {
        return locator.replace("PLACE_HOLDER", Integer.toString(placeHolder));
    }

    @Override
    public String replacePlaceHolder(String locator, String placeHolder, String placeHolder1) {
        return locator.replace("PLACE_HOLDER", placeHolder).replace("TEMP", placeHolder1);
    }

}
