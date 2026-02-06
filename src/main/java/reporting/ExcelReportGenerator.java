package reporting;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import reporting.DetailedTestReporter.ExecutionStatus;
import reporting.DetailedTestReporter.TestExecution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExcelReportGenerator
{
	public static void writeTestExecutionsToExcel(
            String defaultPath,
            String sheetNames,
            String flags,
            String releases,
            String accountHeader,
            String moduleName,
            List<TestExecution> testExecutions) {

        List<String> tcIds = new ArrayList<>();
        List<String> results = new ArrayList<>();

        for (TestExecution execution : testExecutions) {
            tcIds.add(execution.getShortDescription());
            results.add(execution.getStatus() == ExecutionStatus.PASS ? "PASS" : "FAIL");
        }

        writeToExcel(
                defaultPath,
                sheetNames,
                flags,
                String.join(",", tcIds),
                releases,
                String.join(",", results),
                accountHeader,
                moduleName
        );
    }

    /* =========================
       CORE EXCEL WRITE METHOD
       ========================= */

    public static void writeToExcel(
            String defaultPath,
            String sheetNames,
            String flags,
            String testCaseIds,
            String releases,
            String results,
            String accountHeader,
            String moduleName) {

        String[] sheetArr = sheetNames.split(",");
        String[] flagArr = flags.split(",");
        String[] tcIdArr = testCaseIds.split(",");
        String[] releaseArr = releases.split(",");
        String[] resultArr = results.split(",");

        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // ✅ Ensure directory exists
        File dir = new File(defaultPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (int s = 0; s < sheetArr.length; s++) {

            if (!"yes".equalsIgnoreCase(flagArr[s])) {
                continue;
            }

            String sheetName = sheetArr[s];
            String filePath = defaultPath + sheetName + ".xlsx";
            File file = new File(filePath);

            Workbook workbook = null;

            try {
                // ✅ SAFE workbook creation
                if (file.exists() && file.length() > 0) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        workbook = new XSSFWorkbook(fis);
                    }
                } else {
                    workbook = new XSSFWorkbook();
                }

                Sheet sheet = getOrCreateSheet(workbook, sheetName);
                Row headerRow = getOrCreateHeaderRow(sheet);

                CellStyle headerStyle = createHeaderCellStyle(workbook);
                createHeaderIfAbsent(headerRow, 0, "Module Name", headerStyle);
                createHeaderIfAbsent(headerRow, 1, "Test Case ID", headerStyle);

                int resultColIndex = getResultColumnIndex(
                        headerRow,
                        sheetName,
                        today,
                        releaseArr[s],
                        accountHeader,
                        headerStyle
                );

                CellStyle passStyle = createCellStyle(
                        workbook,
                        new XSSFColor(new Color(0, 97, 0), null),
                        IndexedColors.WHITE.getIndex()
                );

                CellStyle failStyle = createCellStyle(
                        workbook,
                        new XSSFColor(new Color(156, 0, 6), null),
                        IndexedColors.WHITE.getIndex()
                );

                for (int i = 0; i < tcIdArr.length; i++) {

                    String tcId = tcIdArr[i];
                    String res = resultArr[i];
                    boolean updated = false;

                    for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row != null &&
                                cellEquals(row.getCell(0), moduleName) &&
                                cellEquals(row.getCell(1), tcId)) {

                            updateResultCell(row, resultColIndex, res, passStyle, failStyle);
                            updated = true;
                            break;
                        }
                    }

                    if (!updated) {
                        Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
                        newRow.createCell(0).setCellValue(moduleName);
                        newRow.createCell(1).setCellValue(tcId);
                        updateResultCell(newRow, resultColIndex, res, passStyle, failStyle);
                    }
                }

                // ✅ WRITE ONLY ONCE, AFTER ALL UPDATES
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    workbook.write(fos);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (workbook != null) {
                        workbook.close();
                    }
                } catch (IOException ignored) {}
            }
        }
    }

    /* =========================
       SUPPORT METHODS
       ========================= */

    private static Sheet getOrCreateSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        return sheet != null ? sheet : workbook.createSheet(name);
    }

    private static Row getOrCreateHeaderRow(Sheet sheet) {
        Row row = sheet.getRow(0);
        return row != null ? row : sheet.createRow(0);
    }

    private static void createHeaderIfAbsent(Row row, int index, String value, CellStyle style) {
        if (row.getCell(index) == null) {
            Cell cell = row.createCell(index);
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }

    private static int getResultColumnIndex(
            Row headerRow,
            String sheetName,
            String today,
            String release,
            String accountHeader,
            CellStyle style) {

        String dynamicHeader =
                sheetName.equalsIgnoreCase("daily") ? today :
                sheetName.equalsIgnoreCase("release") ? "Release - " + release :
                accountHeader;

        for (int i = 2; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && dynamicHeader.equals(cell.getStringCellValue())) {
                return i;
            }
        }

        int newIndex = headerRow.getLastCellNum() == -1 ? 2 : headerRow.getLastCellNum();
        Cell cell = headerRow.createCell(newIndex);
        cell.setCellValue(dynamicHeader);
        cell.setCellStyle(style);
        return newIndex;
    }

    private static void updateResultCell(
            Row row,
            int colIndex,
            String result,
            CellStyle passStyle,
            CellStyle failStyle) {

        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(result);
        cell.setCellStyle(result.contains("PASS") ? passStyle : failStyle);
    }

    private static boolean cellEquals(Cell cell, String expected) {
        return cell != null &&
               cell.getCellType() == CellType.STRING &&
               expected.equalsIgnoreCase(cell.getStringCellValue());
    }

    private static CellStyle createCellStyle(
            Workbook workbook,
            XSSFColor bgColor,
            short fontColor) {

        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(fontColor);
        style.setFont(font);
        style.setFillForegroundColor(bgColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle createHeaderCellStyle(Workbook workbook) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
