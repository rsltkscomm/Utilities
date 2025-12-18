package core.interfaces;

import java.util.List;

public interface DropdownInterface {

    boolean selectListElements(String elementsPath, String input);

    boolean selectExactListElements(String elementsPath, String input);

    boolean selectListElementByIndex(String elementsPath, int index);

    List<String> getDropdownValuesasList(String dropdownLocator, String dropdownListLocator);

    boolean selectListElementByAttribute(Object pr, String attribute, String value);
}
