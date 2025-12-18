package core.interfaces;

import java.util.List;

public interface SelectInterface {

    boolean selectByVisibleText(String locator, String text);

    boolean selectByValue(String locator, String value);

    boolean selectByIndex(String locator, int index);

    boolean deselectByVisibleText(String locator, String text);

    boolean deselectByValue(String locator, String value);

    boolean deselectByIndex(String locator, int index);

    boolean deselectAll(String locator);

    String getSelectedOption(String locator);

    List<String> getAllSelectedOptions(String locator);

    List<String> getAllOptions(String locator);

    boolean isMultiple(String locator);

    boolean jsSelectAllText(String locator);
}
