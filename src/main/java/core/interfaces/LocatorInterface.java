package core.interfaces;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.codeborne.selenide.SelenideTargetLocator;
import com.microsoft.playwright.Locator;

public interface LocatorInterface {
    /**
     * Convert your "ElementName,locatorType,value" style string into a Selenium By.
     * Keeping this so main project that calls autolocator(...) continues to work.
     */
    By autolocator(String key);

    // placeholder replacements (kept same signatures)
    String replacePlaceHolder(String locator, String placeHolder);
    String replacePlaceHolder(String locator, int placeHolder);
    String replacePlaceHolder(String locator, String placeHolder, String placeHolder1);

    /**
     * Accepts either a locator string (same format as before) or a WebElement.
     * Returns a WebElement (Selenium WebElement interface). For Playwright implementation
     * we return a wrapper that implements WebElement.
     */
    WebElement getElement(Object pr);

    /**
     * Accepts either a locator string or a WebElement and returns list of WebElements.
     */
    List<WebElement> getElements(Object pr);
    
}
