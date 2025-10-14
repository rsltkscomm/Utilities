package constants;

import config.ConfigurationFactory;
import java.nio.file.Paths;

/**
 * Framework Constants with Configuration Management Integration
 * Maintains backward compatibility while using new configuration system
 */
public class FrameworkConstants
{
	// Legacy static fields - now use ConfigurationFactory for dynamic values
	public static final String PROJECT_NAME = getProjectName();
	public static final String USER_NAME = getUserName();
	public static final String ENVIRONMENT = getEnvironment();
	
	// Path configurations - now use ConfigurationFactory
	public static final String BASE_PATH = getBasePath();
	public static final String DEFAULT_FILE_PATH = getDefaultFilePath();
	public static final String CREDENTIAL_FILE_PATH = getCredentialFilePath();
	public static final String PROPERTIES_PATH = getPropertiesPath();
	public static final String KLOV_PROPERTIES_PATH = getKlovPropertiesPath();
	public static final String SCRIPT_DETAILS_FILE = getScriptDetailsFile();
	public static final String SUITE_NAME_FILE = getSuiteNameFile();
	public static final String ONEDRIVE_BASE_PATH = getOneDriveBasePath();
	public static final String TEST_DATA_PATH = getTestDataPath();
	public static final String UPLOAD_FILES = getUploadFiles();
	public static final String TEAM_DATA_FILE = getTeamDataFile();
	public static final String DYNAMIC_PATH = getDynamicPath();
	public static final String DYNAMIC_CSV_PREFIX = getDynamicCsvPrefix();
	
	private FrameworkConstants() {
		// Private constructor to prevent instantiation
	}
	
	// ===========================================
	// CONFIGURATION GETTERS WITH FALLBACK
	// ===========================================
	
	private static String getProjectName() {
		try {
			return ConfigurationFactory.getProjectName();
		} catch (Exception e) {
			return System.getProperty("Project", "DefaultProject");
		}
	}
	
	private static String getUserName() {
		try {
			return ConfigurationFactory.getUserName();
		} catch (Exception e) {
			return System.getProperty("UserName", System.getProperty("user.name", "default"));
		}
	}
	
	private static String getEnvironment() {
		try {
			return ConfigurationFactory.getEnvironment();
		} catch (Exception e) {
			return System.getProperty("Environment", "test");
		}
	}
	
	private static String getBasePath() {
		try {
			String basePath = ConfigurationFactory.getConfigValue("paths.base", "C:\\AQAcred");
			return Paths.get(basePath).toString();
		} catch (Exception e) {
			return Paths.get("C:", "AQAcred").toString();
		}
	}
	
	private static String getDefaultFilePath() {
		try {
			return Paths.get(BASE_PATH, PROJECT_NAME).toString();
		} catch (Exception e) {
			return Paths.get(BASE_PATH, "DefaultProject").toString();
		}
	}
	
	private static String getCredentialFilePath() {
		try {
			return Paths.get(BASE_PATH, "Credential.xlsx").toString();
		} catch (Exception e) {
			return Paths.get(BASE_PATH, "Credential.xlsx").toString();
		}
	}
	
	private static String getPropertiesPath() {
		try {
			return Paths.get(DEFAULT_FILE_PATH, "Properties").toString();
		} catch (Exception e) {
			return Paths.get(DEFAULT_FILE_PATH, "Properties").toString();
		}
	}
	
	private static String getKlovPropertiesPath() {
		try {
			String klovPath = ConfigurationFactory.getKlovPropertyFile();
			if (klovPath != null && !klovPath.isEmpty()) {
				return klovPath;
			}
			return Paths.get(PROPERTIES_PATH, "klov.properties").toString();
		} catch (Exception e) {
			return Paths.get(PROPERTIES_PATH, "klov.properties").toString();
		}
	}
	
	private static String getScriptDetailsFile() {
		try {
			return Paths.get(DEFAULT_FILE_PATH, "ScriptDetails.xlsx").toString();
		} catch (Exception e) {
			return Paths.get(DEFAULT_FILE_PATH, "ScriptDetails.xlsx").toString();
		}
	}
	
	private static String getSuiteNameFile() {
		try {
			return Paths.get(DEFAULT_FILE_PATH, "SuiteNameFile.xlsx").toString();
		} catch (Exception e) {
			return Paths.get(DEFAULT_FILE_PATH, "SuiteNameFile.xlsx").toString();
		}
	}
	
	private static String getOneDriveBasePath() {
		try {
			String oneDrivePath = ConfigurationFactory.getConfigValue("paths.oneDrive", 
				Paths.get(System.getProperty("user.home"), "OneDrive - RESULTICKS DIGITALS INDIA PRIVATE LIMITED", "Automation", PROJECT_NAME).toString());
			return oneDrivePath;
		} catch (Exception e) {
			return Paths.get(System.getProperty("user.home"), "OneDrive - RESULTICKS DIGITALS INDIA PRIVATE LIMITED", "Automation", "DefaultProject").toString();
		}
	}
	
	private static String getTestDataPath() {
		try {
			return Paths.get(ONEDRIVE_BASE_PATH, "AQAcred", PROJECT_NAME).toString();
		} catch (Exception e) {
			return Paths.get(ONEDRIVE_BASE_PATH, "AQAcred", "DefaultProject").toString();
		}
	}
	
	private static String getUploadFiles() {
		try {
			return Paths.get(TEST_DATA_PATH, "TestData", USER_NAME + "_" + ENVIRONMENT, "uploadfiles").toString();
		} catch (Exception e) {
			return Paths.get(TEST_DATA_PATH, "TestData", "default_test", "uploadfiles").toString();
		}
	}
	
	private static String getTeamDataFile() {
		try {
			return Paths.get(TEST_DATA_PATH, "TestData", USER_NAME + "_" + ENVIRONMENT, "Team").toString();
		} catch (Exception e) {
			return Paths.get(TEST_DATA_PATH, "TestData", "default_test", "Team").toString();
		}
	}
	
	private static String getDynamicPath() {
		try {
			return Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "dynamicFile").toString();
		} catch (Exception e) {
			return Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "dynamicFile").toString();
		}
	}
	
	private static String getDynamicCsvPrefix() {
		try {
			return Paths.get(DYNAMIC_PATH, "Automation_dynamicdata").toString();
		} catch (Exception e) {
			return Paths.get(DYNAMIC_PATH, "Automation_dynamicdata").toString();
		}
	}
	
	// ===========================================
	// UTILITY METHODS
	// ===========================================
	
	/**
	 * Reload configuration and update constants
	 */
	public static void reloadConfiguration() {
		try {
			ConfigurationFactory.reload();
			// Note: Static final fields cannot be updated, but new calls will use updated configuration
		} catch (Exception e) {
			// Log error but don't fail
			System.err.println("Failed to reload configuration: " + e.getMessage());
		}
	}
	
	/**
	 * Get current configuration status
	 */
	public static void printConfigurationStatus() {
		try {
			ConfigurationFactory.printAllConfiguration();
		} catch (Exception e) {
			System.err.println("Failed to print configuration status: " + e.getMessage());
		}
	}
}
