package data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import constants.FrameworkConstants;
import reporting.TestLogManager;

/**
 * Utility class for Excel data input and test data management.
 */
public class DataInputProvider {

    private static final String COMMON_TESTDATA_FILE_PATH = FrameworkConstants.DRIVE_REPORT_FILEPATH
            + "TestData\\" + System.getProperty("UserName") + "_" + System.getProperty("Environment").toLowerCase()
            + "\\Team\\Common_testdata.xlsx";

    private static final int FIELD_NAME_COL = 1;
    private static final int DATA_VALUE_COL = 2;
    private static final int FLAG_COL = 3;

    private static final ReentrantLock lock = new ReentrantLock();
    private static final DataFormatter formatter = new DataFormatter();
    private static final String env = System.getProperty("Environment");

    // ------------------------- Sheet Reading -------------------------

    public static List<List<String>> getSheet(String dataExcelFileName, String sheetName) {
        List<List<String>> data = new ArrayList<>();

        String filePath = getDataFilePath(dataExcelFileName);
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                TestLogManager.warning("Sheet not found: " + sheetName);
                return data;
            }

            for (Row row : sheet) {
                List<String> record = new ArrayList<>();
                for (Cell cell : row) {
                    record.add(formatter.formatCellValue(cell));
                }
                data.add(record);
            }

        } catch (IOException e) {
            TestLogManager.error("Error reading sheet: " + sheetName, e);
        }
        return data;
    }

    // ------------------------- Create Sheet -------------------------

    public static void createSheet(String dataFileName, String sheetName, String clientId) {
        String filePath = getDataFilePath(dataFileName);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet(sheetName);
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(clientId);
            workbook.write(fos);

        } catch (IOException e) {
            TestLogManager.error("Error creating sheet: " + sheetName, e);
        }
    }

    // ------------------------- Read / Write Cell -------------------------

    public static void writeValue(String dataFileName, String sheetName, int rowNum, int colNum, String value) {
        String filePath = getDataFilePath(dataFileName);
        lock.lock();
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
            }

            Row row = sheet.getRow(rowNum);
            if (row == null) row = sheet.createRow(rowNum);

            Cell cell = row.getCell(colNum);
            if (cell == null) cell = row.createCell(colNum);

            cell.setCellValue(value);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

        } catch (IOException e) {
            TestLogManager.error("Error writing value in sheet: " + sheetName, e);
        } finally {
            lock.unlock();
        }
    }

    public static String readValue(String dataFileName, String sheetName, int rowNum, int colNum) {
        String value = "";
        String filePath = getDataFilePath(dataFileName);
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) return value;

            Row row = sheet.getRow(rowNum);
            if (row == null) return value;

            Cell cell = row.getCell(colNum);
            if (cell != null) value = formatter.formatCellValue(cell);

        } catch (IOException e) {
            TestLogManager.error("Error reading value from sheet: " + sheetName, e);
        }
        return value;
    }

    // ------------------------- Common Test Data -------------------------

    public static String getCommonData(String sheetName, String cellFieldName, String ignoreData) {
        lock.lock();
        try (FileInputStream fis = new FileInputStream(COMMON_TESTDATA_FILE_PATH);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                TestLogManager.warning("Sheet not found: " + sheetName);
                return "";
            }

            String value = fetchData(sheet, cellFieldName, ignoreData);
            if (value.isEmpty()) {
                resetAllFlags(sheet, cellFieldName);
                value = fetchData(sheet, cellFieldName, ignoreData);
            }

            saveWorkbook(workbook, COMMON_TESTDATA_FILE_PATH);
            return value;

        } catch (IOException e) {
            TestLogManager.error("Error reading common data from sheet: " + sheetName, e);
        } finally {
            lock.unlock();
        }
        return "";
    }

    private static String fetchData(Sheet sheet, String fieldName, String ignoreData) {
        for (Row row : sheet) {
            Cell fieldCell = row.getCell(FIELD_NAME_COL);
            if (fieldCell == null) continue;

            if (fieldName.equalsIgnoreCase(formatter.formatCellValue(fieldCell))) {
                Cell flagCell = row.getCell(FLAG_COL);
                boolean isActive = flagCell != null && Boolean.parseBoolean(formatter.formatCellValue(flagCell));

                if (isActive) {
                    Cell dataCell = row.getCell(DATA_VALUE_COL);
                    String value = dataCell != null ? formatter.formatCellValue(dataCell) : "";

                    if (!ignoreData.toLowerCase().contains(value.toLowerCase())) {
                        if (flagCell != null) flagCell.setCellValue(false);
                        return value;
                    }
                }
            }
        }
        return "";
    }

    private static void resetAllFlags(Sheet sheet, String fieldName) {
        for (Row row : sheet) {
            Cell fieldCell = row.getCell(FIELD_NAME_COL);
            if (fieldCell == null) continue;

            if (fieldName.equalsIgnoreCase(formatter.formatCellValue(fieldCell))) {
                Cell flagCell = row.getCell(FLAG_COL);
                if (flagCell == null) flagCell = row.createCell(FLAG_COL);
                flagCell.setCellValue(true);
            }
        }
    }

    // ------------------------- Helpers -------------------------

    private static String getDataFilePath(String dataFileName) {
        return TestDataUtil.getDataFilesPath(dataFileName + ".xlsx");
    }

    private static void saveWorkbook(Workbook workbook, String path) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            workbook.write(fos);
        } catch (IOException e) {
            TestLogManager.error("Error saving workbook: " + path, e);
        }
    }

}
