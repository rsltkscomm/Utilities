package runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.SkipException;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import constants.FrameworkConstants;
import data.XLSReader;
import reporting.TestLogManager;

public class DynamicSuiteGenerator {

    public static ThreadLocal<XLSReader> credentialSheet = new ThreadLocal<>();
    public static ThreadLocal<XLSReader> suiteFileName = new ThreadLocal<>();

    public void createXml() {
        checkCredential();

        if ("yes".equalsIgnoreCase(System.getProperty("Customizedxml"))) {
            XmlSuite suite = new XmlSuite();
            suite.setName(System.getProperty("SuiteName"));
            suite.setParallel(XmlSuite.ParallelMode.METHODS);
            suite.setThreadCount(2);

            List<String> listeners = new ArrayList<>();
            if ("yes".equalsIgnoreCase(System.getProperty("isRetry"))) {
                listeners.add("listeners.RetryAnalyzer");
            }
            listeners.add("listeners.TestListener");
            listeners.add("reporting.NewCutsomHTMLReport");
            listeners.add("listeners.NoProdMethodSkipper");
            suite.setListeners(listeners);

            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("runner", "SELENIUM WEBDRIVER");
            suite.setParameters(parameters);

            List<XmlTest> xmlTests;
            if (System.getProperty("SuiteName").toLowerCase().contains("all")) {
                xmlTests = readRunnerExcelAllSheet(suite);
            } else {
                xmlTests = readRunnerExcel(suite);
            }

            TestNG testng = new TestNG();
            testng.setXmlSuites(List.of(suite));
            testng.setVerbose(10);
            testng.run();

            try {
                String filePath = "testng-dynamic.xml";
                Files.write(Paths.get(filePath), suite.toXml().getBytes());
                TestLogManager.info("TestNG XML saved to: " + filePath);
            } catch (IOException e) {
                TestLogManager.error("Failed to save TestNG XML", e);
            }
        } else {
            TestNG testng = new TestNG();
            String xmlPath = getXml();
            if (!xmlPath.isEmpty()) {
                testng.setTestSuites(List.of(xmlPath));
            } else {
                throw new SkipException("Default suite file does not exist.");
            }
            testng.run();
        }

        // Cleanup ThreadLocals
        suiteFileName.remove();
        credentialSheet.remove();
    }

    public static List<XmlTest> readRunnerExcel(XmlSuite suite) {
        return processExcel(suite, false);
    }

    public static List<XmlTest> readRunnerExcelAllSheet(XmlSuite suite) {
        return processExcel(suite, true);
    }

    private static List<XmlTest> processExcel(XmlSuite suite, boolean allSheets) {
        List<XmlTest> tests = new ArrayList<>();
        String filePath = FrameworkConstants.SCRIPTDETAILS_FILEPATH;
        File excelFile = new File(filePath);

        if (!excelFile.exists() || excelFile.length() == 0) {
            throw new RuntimeException("Excel file not found or empty: " + excelFile.getAbsolutePath());
        }

        try (FileInputStream file = new FileInputStream(excelFile);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                if (!allSheets && !sheetName.toLowerCase().contains(System.getProperty("SuiteName").toLowerCase())) {
                    continue;
                }

                XSSFRow headerRow = sheet.getRow(1);
                if (headerRow == null) continue;

                Map<String, Integer> colMap = mapHeaders(headerRow);

                if (!colMap.keySet().containsAll(Arrays.asList("package name", "class name", "method name", "run"))) {
                    TestLogManager.warning("Required columns not found in sheet: " + sheetName);
                    continue;
                }

                Set<String> packageAndClassName = new TreeSet<>();
                for (int j = 2; j <= sheet.getLastRowNum(); j++) {
                    XSSFRow row = sheet.getRow(j);
                    if (row == null) continue;

                    String pkg = getCellValue(row.getCell(colMap.get("package name")));
                    String cls = getCellValue(row.getCell(colMap.get("class name")));
                    if (!pkg.isEmpty() && !cls.isEmpty()) {
                        packageAndClassName.add(pkg + "." + cls);
                    }
                }

                for (String name : packageAndClassName) {
                    List<XmlInclude> methods = new ArrayList<>();

                    for (int k = 2; k <= sheet.getLastRowNum(); k++) {
                        XSSFRow row = sheet.getRow(k);
                        if (row == null) continue;

                        String isExecute = getCellValue(row.getCell(colMap.get("run")));
                        String type = getCellValue(row.getCell(colMap.getOrDefault("type", -1)));

                        String classFullName = getCellValue(row.getCell(colMap.get("package name"))) + "." +
                                getCellValue(row.getCell(colMap.get("class name")));

                        if ("yes".equalsIgnoreCase(isExecute) &&
                                classFullName.equals(name) &&
                                (type.isEmpty() || type.equalsIgnoreCase(System.getProperty("Type", "default").toLowerCase()))) {

                            String methodName = getCellValue(row.getCell(colMap.get("method name")));
                            if (!methodName.isEmpty()) methods.add(new XmlInclude(methodName));
                        }
                    }

                    if (!methods.isEmpty()) {
                        XmlTest test = new XmlTest(suite);
                        test.setName(name.split("\\.")[1]);
                        XmlClass cls = new XmlClass(name);
                        cls.setIncludedMethods(methods);
                        test.setXmlClasses(List.of(cls));

                        Map<String, String> params = new HashMap<>();
                        params.put("browser", System.getProperty("Browser", "chrome"));

                        for (int k = 2; k <= sheet.getLastRowNum(); k++) {
                            XSSFRow row = sheet.getRow(k);
                            if (row == null) continue;

                            String currentClassFullName = getCellValue(row.getCell(colMap.get("package name"))) + "." +
                                    getCellValue(row.getCell(colMap.get("class name")));

                            if (currentClassFullName.equals(name)) {
                                params.put("sheetname", getCellValue(row.getCell(colMap.getOrDefault("sheet name", -1))));
                                params.put("applicationName", getCellValue(row.getCell(colMap.getOrDefault("application name", -1))));
                                break;
                            }
                        }

                        test.setParameters(params);
                        tests.add(test);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process Excel file: " + filePath, e);
        }

        return tests;
    }

    private static Map<String, Integer> mapHeaders(XSSFRow headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            XSSFCell cell = headerRow.getCell(i);
            if (cell != null) map.put(cell.getStringCellValue().trim().toLowerCase(), i);
        }
        return map;
    }

    private static String getCellValue(XSSFCell cell) {
        return (cell == null) ? "" : cell.getStringCellValue().trim();
    }

    public static String getXml() {
        suiteFileName.set(new XLSReader(FrameworkConstants.SUITE_NAME_FILEPATH));
        String sheetName = System.getProperty("ProductName");
        String suiteName = System.getProperty("SuiteName");

        for (int i = 1; i <= suiteFileName.get().getRowCount(sheetName); i++) {
            if (suiteFileName.get().getCellData(sheetName, "CustomName", i).equalsIgnoreCase(suiteName.toLowerCase())) {
                String SuiteName = suiteFileName.get().getCellData(sheetName, "SuiteName", i);
                return Paths.get(System.getProperty("user.dir"), "runner", SuiteName + ".xml").toString();
            }
        }
        return "";
    }

    public boolean getCredentialAndValidate() {
        String sheetName = System.getProperty("ProductName");
        String userName = System.getProperty("UserName");
        String environment = System.getProperty("Environment");
        String accountType = System.getProperty("Account");
        String role = System.getProperty("Role");
        String version = System.getProperty("ReleaseVersion");

        credentialSheet.set(new XLSReader(FrameworkConstants.CREDENTIAL_FILEPATH));
        boolean matcher = false;

        for (int i = 1; i <= credentialSheet.get().getRowCount(sheetName); i++) {
            matcher = (userName == null || userName.isEmpty()) ?
                    "yes".equalsIgnoreCase(credentialSheet.get().getCellData(sheetName, "Default", i)) :
                    userName.equalsIgnoreCase(credentialSheet.get().getCellData(sheetName, "UserName", i));

            matcher = matcher &&
                    environment.equalsIgnoreCase(credentialSheet.get().getCellData(sheetName, "Environment", i)) &&
                    version.equalsIgnoreCase(credentialSheet.get().getCellData(sheetName, "Version", i)) &&
                    accountType.equalsIgnoreCase(credentialSheet.get().getCellData(sheetName, "Account", i)) &&
                    role.equalsIgnoreCase(credentialSheet.get().getCellData(sheetName, "Role", i));

            if (matcher) {
                String[] keys = { "Password", "KeyAccountWebmailUserName", "keyAccountWebmailPassword", "RFAOneApproverWebmailUserName",
                        "RFAOneApproverWebmailPassword", "RFAOneApproverAccountPassword", "RFATwoApproverWebmailUserName",
                        "RFATwoApprover",                        "RFATwoApproverWebmailPassword", "RFATwoApproverAccountPassword",
                        "RFAThreeApproverWebmailUserName", "RFAThreeApproverWebmailPassword",
                        "RFAThreeApproverAccountPassword", "CommunicationBlast", "CommunicationBlast1",
                        "CommunicationBlast2", "CommunicationTestpreview", "CommunicationEventtrigger",
                        "CommunicationBlastPassword", "CommunicationBlast1Password", "CommunicationBlast2Password",
                        "CommunicationTestpreviewPassword", "CommunicationEventtriggerPassword", "Communicationapprover",
                        "CommunicationapproverPassword", "CommunicationFormsPassword", "CommunicationForms",
                        "Communication_approver2_webmail_Uname", "Communication_approver2_webmail_pswrd",
                        "Target_Dynamic_approver1_Uname", "Target_Dynamic_approver1_pswrd",
                        "GateApprovalUsername", "GateApprovalPwd"
                };
                setSystemPropertiesFromExcel(keys, sheetName, i);
                System.setProperty("UserName", credentialSheet.get().getCellData(sheetName, "UserName", i));
                return true;
            }
        }
        return false;
    }

    public void setSystemPropertiesFromExcel(String[] columnNames, String sheetName, int row) {
        for (String key : columnNames) {
            String value = credentialSheet.get().getCellData(sheetName, key, row);
            System.setProperty(key, value);
        }
    }

    public void checkCredential() {
        if (getCredentialAndValidate()) {
            TestLogManager.success("Credential is matched successfully.");
        } else {
            throw new SkipException("Aborting suite: Credential values do not match.");
        }
    }
}

