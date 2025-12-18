package core.interfaces;

public interface ScrollInterface {

    boolean scrollToElement(Object pr);

    void javaScriptScrollIntoView(Object pr);

    boolean scrollBy(int x, int y);

    boolean scrollToTop();

    boolean scrollToBottom();

    boolean scrollByElementOffset(Object pr, int xOffset, int yOffset);

    void waitForScroll();

    void scrollStep(int pixels);
}
