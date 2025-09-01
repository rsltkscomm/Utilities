package constants;

public class FrameworkConstants
{
	public static final String PROJECT_NAME = System.getProperty("Project");
	public static final String DEFAULTFILEPATH = "C:\\AQAcred\\" + PROJECT_NAME + "\\";
	public static final String CREDENTIAL_FILEPATH= "C:\\AQAcred\\Credential.xlsx";
	public static final String PROPERTIES_FILEPATH= DEFAULTFILEPATH + "Properties";
	public static final String KLOV_PROPERTIES_FILEPATH= DEFAULTFILEPATH + "Properties\\klov.properties";
	public static final String SCRIPTDETAILS_FILEPATH= DEFAULTFILEPATH + "ScriptDetails.xlsx";
	public static final String SUITE_NAME_FILEPATH= DEFAULTFILEPATH + "SuiteNameFile.xlsx";
	public static final String DRIVE_REPORT_FILEPATH = System.getProperty("user.home") + "\\OneDrive - RESULTICKS DIGITALS INDIA PRIVATE LIMITED\\Automation\\"+PROJECT_NAME+"\\";
	public static final String DRIVE_TESTDATA_FILEPATH = DRIVE_REPORT_FILEPATH + "\\AQAcred\\"+PROJECT_NAME+"\\";
	public static final String UPLOADFILES = DRIVE_TESTDATA_FILEPATH + "TestData\\" + System.getProperty("UserName") + "_" + System.getProperty("Environment") + "\\uploadfiles\\";
	public static final String DATAFILE = DRIVE_TESTDATA_FILEPATH + "TestData\\" + System.getProperty("UserName") + "_" + System.getProperty("Environment").toLowerCase() + "\\Team\\";
	public static final String DYNAMICPATH = System.getProperty("user.dir") + "\\src\\main\\resources\\dynamicFile\\";
	public static final String DYNAMICCSV = System.getProperty("user.dir") + "\\src\\main\\resources\\dynamicFile\\Automation_dynamicdat";
}
