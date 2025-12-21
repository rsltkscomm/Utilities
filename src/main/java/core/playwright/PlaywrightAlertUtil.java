package core.playwright;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Page;

import base.DriverContext;
import core.interfaces.AlertInterface;
import reporting.ExtentManager;

public class PlaywrightAlertUtil extends PlaywrightDragAndDropUtil
        implements AlertInterface {

    private final DriverContext driverContext;
    private Dialog activeDialog;

    public PlaywrightAlertUtil(DriverContext driverContext) {
        super(driverContext);
        this.driverContext = driverContext;
        bindDialogListener();
    }

    /**
     * Bind dialog listener to the CURRENT page
     */
    private void bindDialogListener() {
        Page page = driverContext.getPage();

        page.onDialog(dialog -> {
            this.activeDialog = dialog;
            ExtentManager.infoTest(
                "Alert appeared -> Type: " + dialog.type() +
                ", Message: " + dialog.message()
            );
        });
    }

    /**
     * Ensure listener follows tab switching
     */
    private void refreshBinding() {
        bindDialogListener();
    }

    /* -------------------- ACCEPT ALERT -------------------- */

    @Override
    public boolean acceptAlert() {
        try {
            refreshBinding();

            if (activeDialog == null) {
                throw new IllegalStateException("No active alert present");
            }

            String text = activeDialog.message();
            activeDialog.accept();

            ExtentManager.infoTest("Accepted alert with text: " + text);
            activeDialog = null;
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Accept alert failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- DISMISS ALERT -------------------- */

    @Override
    public boolean dismissAlert() {
        try {
            refreshBinding();

            if (activeDialog == null) {
                throw new IllegalStateException("No active alert present");
            }

            String text = activeDialog.message();
            activeDialog.dismiss();

            ExtentManager.infoTest("Dismissed alert with text: " + text);
            activeDialog = null;
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Dismiss alert failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- GET ALERT TEXT -------------------- */

    @Override
    public String getAlertText() {
        try {
            refreshBinding();

            if (activeDialog == null) {
                throw new IllegalStateException("No active alert present");
            }

            String text = activeDialog.message();
            ExtentManager.infoTest("Alert text: " + text);
            return text;

        } catch (Exception e) {
            ExtentManager.failTest("Get alert text failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return null;
        }
    }

    /* -------------------- SEND KEYS TO ALERT -------------------- */

    @Override
    public boolean sendKeysToAlert(String keys) {
        try {
            refreshBinding();

            if (activeDialog == null) {
                throw new IllegalStateException("No active alert present");
            }

            activeDialog.accept(keys);
            ExtentManager.infoTest("Sent keys to alert: " + keys);
            activeDialog = null;
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Send keys to alert failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }
}
