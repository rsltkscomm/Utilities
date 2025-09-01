package data;

import java.util.HashMap;
import java.util.Map;

import base.BaseTest;
import constants.FrameworkConstants;
import reporting.TestLogManager;

public class TestDataUtil
{
	private static Map<Long, ExcelUtil> exceldata = new HashMap<>();
	/**
     * Validates if the current TestMethodName exists in Excel sheet.
     * Sets currentRow in BaseTest if found.
     */
    public synchronized boolean isTCIDFound(BaseTest test) {
        XLSReader table = test.datatable.get();
        String sheetname = test.sheet_name.get();
        String methodName = test.method_name.get();

        if (table == null) {
            TestLogManager.error("Datatable is not initialized!");
            return false;
        }

        for (int i = 2; i <= table.getRowCount(sheetname); i++) {
            String cellValue = table.getCellData(sheetname, "TestMethodName", i);
            if (methodName.equals(cellValue)) {
                TestLogManager.info("TCID match found for: ---> " + methodName);
                test.currentRow.set(i);
                return true;
            }
        }

        TestLogManager.error("TCID not found in sheet: " + sheetname + " for method: " + methodName);
        return false;
    }
    
    public static synchronized ExcelUtil createDataRef()
	{
		ExcelUtil obj = new ExcelUtil();
		exceldata.put(Thread.currentThread().getId(), obj);
		return obj;
	}
    
    public static synchronized ExcelUtil getData()
	{
		return exceldata.get(Thread.currentThread().getId());
	}
    
    public static String getUploadFilesPath(String fileName)
	{
		return FrameworkConstants.UPLOADFILES + fileName;
	}

	public static String getDataFilesPath(String fileName)
	{
		return FrameworkConstants.DATAFILE + fileName;
	}
}
