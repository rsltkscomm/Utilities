package constants;

import java.nio.file.Paths;

public class FrameworkConstants
{
	public static final String PROJECT_NAME = System.getProperty("Project");
	public static final String USER_NAME = System.getProperty("UserName");
	public static final String ENVIRONMENT = System.getProperty("Environment");
	public static final String BASE_PATH = Paths.get("C:", "AQAcred").toString();
	public static final String DEFAULT_FILE_PATH = Paths.get(BASE_PATH, PROJECT_NAME).toString();
	public static final String CREDENTIAL_FILE_PATH = Paths.get(BASE_PATH, "Credential.xlsx").toString();
	public static final String PROPERTIES_PATH = Paths.get(DEFAULT_FILE_PATH, "Properties").toString();
	public static final String KLOV_PROPERTIES_PATH = Paths.get(PROPERTIES_PATH, "klov.properties").toString();
	public static final String SCRIPT_DETAILS_FILE = Paths.get(DEFAULT_FILE_PATH, "ScriptDetails.xlsx").toString();
	public static final String SUITE_NAME_FILE = Paths.get(DEFAULT_FILE_PATH, "SuiteNameFile.xlsx").toString();
	public static final String ONEDRIVE_BASE_PATH = Paths.get(System.getProperty("user.home"), "OneDrive - RESULTICKS DIGITALS INDIA PRIVATE LIMITED", "Automation", PROJECT_NAME).toString();
	public static final String TEST_DATA_PATH = Paths.get(ONEDRIVE_BASE_PATH, "AQAcred", PROJECT_NAME).toString();
	public static final String UPLOAD_FILES = Paths.get(TEST_DATA_PATH, "TestData", USER_NAME + "_" + ENVIRONMENT, "uploadfiles").toString();
	public static final String TEAM_DATA_FILE = Paths.get(TEST_DATA_PATH, "TestData", USER_NAME + "_" + ENVIRONMENT, "Team").toString();
	public static final String DYNAMIC_PATH = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "dynamicFile").toString();
	public static final String DYNAMIC_CSV_PREFIX = Paths.get(DYNAMIC_PATH, "Automation_dynamicdata").toString();
	private FrameworkConstants() {
	}

}
