package core.interfaces;

public interface AlertInterface {

    boolean acceptAlert();

    boolean dismissAlert();

    String getAlertText();

    boolean sendKeysToAlert(String keys);
}
