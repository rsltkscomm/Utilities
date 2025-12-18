package core.interfaces;

public interface AssertInterface {

    boolean writeLog(boolean expression, String passLog, String failLog);

    boolean checkIsElementNull(Object element);

    boolean writeLogger(boolean expression, String passLog, String failLog);

    boolean placeholderValueCheck(String locator, String placeHolderText);

    boolean validateUiBackgroundColour(String cssProperty, String locator, String expectedHex);

    boolean uiPageEqualsWithMultipleInputValue(String locator, String testDatas);

    boolean uiPageEqualswithInputValue(String txt, String actualText);

    String decodeBase64ToText(String base64Text);
}
