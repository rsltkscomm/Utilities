package core.interfaces;

public interface ClickInterface {

    boolean clickElement(Object pr);

    boolean safeClick(Object pr);

    boolean jsClick(Object pr);

    boolean click(Object element, String elementName);

    boolean doubleClick(Object pr);

    boolean actionsClickElement(String locator);

    boolean rightClick(Object pr);

    boolean hoverAndClick(Object pr);
}
