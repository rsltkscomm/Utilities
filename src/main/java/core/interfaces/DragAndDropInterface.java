package core.interfaces;

public interface DragAndDropInterface {

    boolean dragAndDrop(Object source, Object target);

    boolean dragAndDropByOffset(Object source, int xOffset, int yOffset);

    boolean clickHoldMoveRelease(Object source, Object target);

    boolean jsDragAndDrop(Object source, Object target);

    boolean jsDragByOffset(Object source, int xOffset, int yOffset);
}
