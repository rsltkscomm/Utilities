package core.interfaces;

import java.util.List;
import java.util.Map;

import com.microsoft.playwright.Locator;

public interface ElementInterface {

    String getTextBoxValue(Object locator, String attribute);

    String getText(Object locator);

    String getAttribute(Object locator, String attribute);

    boolean sendValue(Object locator, String value);

    String getCssValue(String locator, String property);

    boolean isDisplayed(String locator);

    boolean isEnabled(String locator);

    boolean isSelected(String locator);

    boolean enterValue(Object locator, String value);

    boolean javaScriptEnterValue(Object locator, String text);

    boolean isElementPresent(String locator);

    Object findElement(Object locator);

    List<?> findElements(Object locator);

    String getAllDropdownValues(String locator);

    void tabAction();

    void clickEnter();

    void clearField(Object locator);

    int findGCV(int a, int b);

    String normalizeText(String input);

    List<Map<String, String>> getWebTable(String tableLocator);
    
    boolean validateInvalidLinks(String page);
    
    boolean uploadFile(String locator,String filePath);
    
}
