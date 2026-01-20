package listeners;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import reporting.DetailedTestReporter.ExecutionStatus;
import reporting.DetailedTestReporter.TestExecution;

public class TestExecutionExcelReport {

    public static void writeResultsToExcel(String fileName, List<String[]> results, String baseSheetName) {
        Workbook workbook;
        Sheet sheet;
        File file = new File(fileName);

        try {
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis);
                fis.close();

                // Get or create the sheet
                sheet = workbook.getSheet(baseSheetName);
                if (sheet == null) {
                    sheet = workbook.createSheet(baseSheetName);
                    createHeaderRow(sheet, workbook);
                }
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet(baseSheetName);
                createHeaderRow(sheet, workbook);
            }

            // Define styles
            CellStyle passStyle = createStatusCellStyle(workbook, IndexedColors.GREEN);
            CellStyle failStyle = createStatusCellStyle(workbook, IndexedColors.RED);
            CellStyle skipStyle = createStatusCellStyle(workbook, IndexedColors.YELLOW);
            CellStyle defaultStyle = createBorderStyle(workbook);

            // Find the last row to append data
            int rowNum = sheet.getLastRowNum();
            if (rowNum == 0 && sheet.getRow(0) == null) {
                // If header wasn't written somehow
                createHeaderRow(sheet, workbook);
                rowNum = 0;
            }

            for (int i = 0; i < results.size(); i++) {
                Row row = sheet.createRow(++rowNum);

                // S.No
                Cell sno = row.createCell(0);
                sno.setCellValue(rowNum); // serial number
                sno.setCellStyle(defaultStyle);

                for (int j = 0; j < results.get(i).length; j++) {
                    Cell cell = row.createCell(j + 1);
                    cell.setCellValue(results.get(i)[j]);

                    if (j == 2) { // Status column
                        String status = results.get(i)[j].toUpperCase();
                        switch (status) {
                            case "PASS" -> cell.setCellStyle(passStyle);
                            case "FAIL" -> cell.setCellStyle(failStyle);
                            case "SKIP" -> cell.setCellStyle(skipStyle);
                            default -> cell.setCellStyle(defaultStyle);
                        }
                    } else {
                        cell.setCellStyle(defaultStyle);
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            // Save workbook
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();

            System.out.println("✅ Excel updated: " + baseSheetName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createHeaderRow(Sheet sheet, Workbook workbook) {
        String[] headers = { "S.No", "Test Script Name", "Test Description", "Status" };
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setAllBorders(style);
        return style;
    }

    private static CellStyle createStatusCellStyle(Workbook workbook, IndexedColors bgColor) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setAllBorders(style);
        return style;
    }

    private static CellStyle createBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setAllBorders(style);
        return style;
    }

    private static void setAllBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
    
    public static void updateResultsForDailycheckList(
            String excelPath,
            List<TestExecution> testExecutions,
            boolean flag
    ) {
 
        if (!flag || testExecutions == null || testExecutions.isEmpty()) {
            return;
        }
 
        // 🗓 Date formats
        String monthName =
                new SimpleDateFormat("MMMM", Locale.ENGLISH).format(new Date());
 
        String runDate =
                new SimpleDateFormat("dd.MM.yyyy").format(new Date());
 
        String sheetName = "Daily CheckList " + monthName;
        String dateColumnHeader = "Testing Status RUN " + runDate;
 
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
 
            // 📄 Get or create sheet
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
            }
 
            // 🧾 Header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                headerRow = sheet.createRow(0);
            }
 
            // 🔧 Styles
            CellStyle passStyle = createStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle failStyle = createStyle(workbook, IndexedColors.RED);
            CellStyle skipStyle = createStyle(workbook, IndexedColors.YELLOW);
 
            int methodDescCol = getOrCreateColumn(headerRow, "MethodDescription");
            int todayCol = getOrCreateColumn(headerRow, dateColumnHeader);
 
            // 🔁 Process all test executions
            for (TestExecution execution : testExecutions) {
 
                String description = execution.getShortDescription();
 
                String status;
                CellStyle statusStyle;
 
                if (execution.getStatus() == ExecutionStatus.PASS) {
                    status = "Pass";
                    statusStyle = passStyle;
                } else if (execution.getStatus() == ExecutionStatus.FAIL) {
                    status = "Fail";
                    statusStyle = failStyle;
                } else {
                    status = "Skip";
                    statusStyle = skipStyle;
                }
 
                boolean rowFound = false;
 
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
 
                    Cell descCell = row.getCell(methodDescCol);
                    if (descCell == null) continue;
 
                    String excelDesc =
                            descCell.getStringCellValue();
 
                    String runtimeDesc =
                            description == null ? "" :
                                    description;
 
                    if (excelDesc.equalsIgnoreCase(runtimeDesc)) {
 
                        Cell resultCell = row.createCell(todayCol);
                        resultCell.setCellValue(status);
                        resultCell.setCellStyle(statusStyle);
 
                        rowFound = true;
                        break;
                    }
                }
 
                // ➕ Create new row if description not present
                if (!rowFound) {
                    int newRowNum = sheet.getLastRowNum() + 1;
                    Row newRow = sheet.createRow(newRowNum);
 
                    newRow.createCell(methodDescCol)
                          .setCellValue(description);
 
                    Cell resultCell = newRow.createCell(todayCol);
                    resultCell.setCellValue(status);
                    resultCell.setCellStyle(statusStyle);
                }
            }
 
            // 💾 Save once
            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
                workbook.write(fos);
            }
 
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to update Daily CheckList Excel", e);
        }
 
        System.out.println("Daily CheckList Excel updated successfully.");
    }
    
    private static int getOrCreateColumn(Row headerRow, String headerName) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null &&
                headerName.equalsIgnoreCase(cell.getStringCellValue().trim())) {
                return i;
            }
        }
 
        int newCol = headerRow.getLastCellNum() == -1 ? 0 : headerRow.getLastCellNum();
        headerRow.createCell(newCol).setCellValue(headerName);
        return newCol;
    }
 
    private static CellStyle createStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
 
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
 
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
 
        return style;
    }
}
