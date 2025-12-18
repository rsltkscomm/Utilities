package pages;

import base.AutomationContext;
import base.DriverContext;

public class PageFactory
{
	private final AutomationContext context;
    private final DriverContext driverContext;

    public PageFactory(DriverContext driverContext) {
        this.context = driverContext.getAutomationContext();
        this.driverContext = driverContext;
    }
}
