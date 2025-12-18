package core.interfaces;

public interface MouseHoverInterface {

    boolean mouseHover(Object pr);

    boolean hoverAndClick(Object pr);

    boolean hoverAndDoubleClick(Object pr);

    boolean hoverAndRightClick(Object pr);

    boolean hoverAndSendKeys(Object pr, CharSequence keys);

    boolean hoverByOffset(Object pr, int xOffset, int yOffset);
    
    void clickAndHoldMoveByOffset(Object slider, int x, int y);

    boolean jsMouseHover(Object pr);

    boolean jsHoverAndClick(Object pr);

    boolean jsHoverByStyle(Object pr);
}
