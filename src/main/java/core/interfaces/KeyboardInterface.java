package core.interfaces;

public interface KeyboardInterface {

    boolean sendKeys(Object pr, CharSequence keys);

    boolean sendEnter(Object pr);

    boolean sendTab(Object pr);

    boolean sendEscape(Object pr);

    boolean sendArrowUp(Object pr);

    boolean sendArrowDown(Object pr);

    boolean sendArrowLeft(Object pr);

    boolean sendArrowRight(Object pr);
}
